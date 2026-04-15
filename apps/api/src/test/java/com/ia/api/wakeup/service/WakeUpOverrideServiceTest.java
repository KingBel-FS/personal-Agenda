package com.ia.api.wakeup.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.notification.service.NotificationJobService;
import com.ia.api.sync.service.RealtimeSyncService;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskRuleRepository;
import com.ia.api.task.repository.TaskTimeSlotRepository;
import com.ia.api.user.domain.DayCategory;
import com.ia.api.user.domain.DayProfileEntity;
import com.ia.api.user.repository.DayProfileRepository;
import com.ia.api.wakeup.api.WakeUpOverrideResponse;
import com.ia.api.wakeup.domain.WakeUpOverrideEntity;
import com.ia.api.wakeup.repository.WakeUpOverrideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WakeUpOverrideServiceTest {

    @Mock private WakeUpOverrideRepository wakeUpOverrideRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskOccurrenceRepository taskOccurrenceRepository;
    @Mock private TaskRuleRepository taskRuleRepository;
    @Mock private TaskTimeSlotRepository taskTimeSlotRepository;
    @Mock private DayProfileRepository dayProfileRepository;
    @Mock private NotificationJobService notificationJobService;
    @Mock private RealtimeSyncService realtimeSyncService;

    @Captor private ArgumentCaptor<List<TaskOccurrenceEntity>> saveAllCaptor;

    private WakeUpOverrideService service;

    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";
    // Use a timezone where we can control "now"
    private final String timezone = "UTC";

    @BeforeEach
    void setUp() {
        service = new WakeUpOverrideService(
                wakeUpOverrideRepository,
                userRepository,
                taskOccurrenceRepository,
                taskRuleRepository,
                taskTimeSlotRepository,
                dayProfileRepository,
                notificationJobService,
                realtimeSyncService
        );

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);
        user.setTimezoneName(timezone);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
    }

    @Test
    void upsertOverride_createsNewEntityAndRecalculatesOccurrences() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime newWakeUp = LocalTime.of(6, 30);

        when(wakeUpOverrideRepository.findByUserIdAndOverrideDate(userId, tomorrow))
                .thenReturn(Optional.empty());

        TaskOccurrenceEntity occ = makeOccurrence("WORKDAY");
        TaskRuleEntity rule = makeRule(30); // wake_up + 30 min
        when(taskOccurrenceRepository.findAllByUserIdAndOccurrenceDateAndStatusNot(userId, tomorrow, "canceled"))
                .thenReturn(List.of(occ));
        when(taskRuleRepository.findById(occ.getTaskRuleId())).thenReturn(Optional.of(rule));

        WakeUpOverrideResponse response = service.upsertOverride(email, tomorrow, newWakeUp);

        assertThat(response.date()).isEqualTo(tomorrow.toString());
        assertThat(response.wakeUpTime()).isEqualTo(newWakeUp.toString());
        verify(wakeUpOverrideRepository).save(any(WakeUpOverrideEntity.class));
        verify(taskOccurrenceRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(1);
        assertThat(saveAllCaptor.getValue().get(0).getOccurrenceTime()).isEqualTo(LocalTime.of(7, 0)); // 6:30 + 30
        verify(notificationJobService).cancelPendingJobsForOccurrence(occ.getId());
        verify(realtimeSyncService).publish(email, "TODAY");
    }

    @Test
    void upsertOverride_updatesExistingEntity() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime newWakeUp = LocalTime.of(8, 0);

        WakeUpOverrideEntity existing = new WakeUpOverrideEntity();
        existing.setId(UUID.randomUUID());
        existing.setUserId(userId);
        existing.setOverrideDate(tomorrow);
        existing.setWakeUpTime(LocalTime.of(7, 0));
        when(wakeUpOverrideRepository.findByUserIdAndOverrideDate(userId, tomorrow))
                .thenReturn(Optional.of(existing));
        when(taskOccurrenceRepository.findAllByUserIdAndOccurrenceDateAndStatusNot(userId, tomorrow, "canceled"))
                .thenReturn(List.of());

        service.upsertOverride(email, tomorrow, newWakeUp);

        assertThat(existing.getWakeUpTime()).isEqualTo(newWakeUp);
        verify(wakeUpOverrideRepository).save(existing);
    }

    @Test
    void upsertOverride_rejectsDateInThePast() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertThatThrownBy(() -> service.upsertOverride(email, yesterday, LocalTime.of(7, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(wakeUpOverrideRepository, never()).save(any());
    }

    @Test
    void deleteOverride_recalculatesBackToProfileWakeUp() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        DayProfileEntity profile = new DayProfileEntity();
        profile.setDayCategory(DayCategory.WORKDAY);
        profile.setWakeUpTime(LocalTime.of(7, 30));
        when(dayProfileRepository.findAllByUserId(userId)).thenReturn(List.of(profile));

        TaskOccurrenceEntity occ = makeOccurrence("WORKDAY");
        TaskRuleEntity rule = makeRule(15);
        when(taskOccurrenceRepository.findAllByUserIdAndOccurrenceDateAndStatusNot(userId, tomorrow, "canceled"))
                .thenReturn(List.of(occ));
        when(taskRuleRepository.findById(occ.getTaskRuleId())).thenReturn(Optional.of(rule));

        service.deleteOverride(email, tomorrow);

        verify(wakeUpOverrideRepository).deleteByUserIdAndOverrideDate(userId, tomorrow);
        verify(taskOccurrenceRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue().get(0).getOccurrenceTime()).isEqualTo(LocalTime.of(7, 45)); // 7:30 + 15
        verify(notificationJobService).cancelPendingJobsForOccurrence(occ.getId());
        verify(realtimeSyncService).publish(email, "TODAY");
    }

    @Test
    void deleteOverride_skipsNonWakeUpOffsetOccurrences() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(dayProfileRepository.findAllByUserId(userId)).thenReturn(List.of());

        TaskOccurrenceEntity occ = makeOccurrence("WORKDAY");
        TaskRuleEntity rule = new TaskRuleEntity();
        rule.setId(occ.getTaskRuleId());
        rule.setTimeMode("FIXED"); // not WAKE_UP_OFFSET
        when(taskOccurrenceRepository.findAllByUserIdAndOccurrenceDateAndStatusNot(userId, tomorrow, "canceled"))
                .thenReturn(List.of(occ));
        when(taskRuleRepository.findById(occ.getTaskRuleId())).thenReturn(Optional.of(rule));

        service.deleteOverride(email, tomorrow);

        verify(taskOccurrenceRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).isEmpty();
    }

    // --- helpers ---

    private TaskOccurrenceEntity makeOccurrence(String dayCategory) {
        TaskOccurrenceEntity occ = new TaskOccurrenceEntity();
        occ.setId(UUID.randomUUID());
        occ.setUserId(userId);
        occ.setTaskRuleId(UUID.randomUUID());
        occ.setDayCategory(dayCategory);
        occ.setOccurrenceTime(LocalTime.of(8, 0));
        return occ;
    }

    private TaskRuleEntity makeRule(int offsetMinutes) {
        TaskRuleEntity rule = new TaskRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setTimeMode("WAKE_UP_OFFSET");
        rule.setWakeUpOffsetMinutes(offsetMinutes);
        return rule;
    }
}
