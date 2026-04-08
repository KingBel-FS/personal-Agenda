package com.ia.api.agenda;

import com.ia.api.auth.domain.AccountStatus;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.service.DayClassificationReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TaskOccurrenceRepository taskOccurrenceRepository;
    @Mock private TaskDefinitionRepository taskDefinitionRepository;
    @Mock private DayClassificationReadService dayClassificationReadService;

    private AgendaService agendaService;

    @BeforeEach
    void setUp() {
        agendaService = new AgendaService(
                userRepository,
                taskOccurrenceRepository,
                taskDefinitionRepository,
                dayClassificationReadService
        );
    }

    @Test
    void weekViewStartsOnMonday() {
        UserEntity user = activeUser();
        TaskDefinitionEntity definition = definition(user.getId(), "Lecture", "📚");
        LocalDate anchor = LocalDate.of(2026, 4, 15);
        TaskOccurrenceEntity occurrence = occurrence(
                user.getId(),
                definition.getId(),
                LocalDate.of(2026, 4, 15),
                LocalTime.of(8, 0),
                "done",
                "WORKDAY"
        );

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                user.getId(), "canceled", LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 19)))
                .thenReturn(List.of(occurrence));
        when(taskDefinitionRepository.findAllById(any())).thenReturn(List.of(definition));
        when(dayClassificationReadService.classifyDate(any(), eq(user.getId()), eq("METROPOLE"), any()))
                .thenAnswer(invocation -> invocation.getArgument(0, LocalDate.class).getDayOfWeek().getValue() >= 6
                        ? "WEEKEND_HOLIDAY"
                        : "WORKDAY");

        AgendaRangeResponse response = agendaService.getWeek("alice@example.com", anchor);

        assertThat(response.rangeStart()).isEqualTo(LocalDate.of(2026, 4, 13));
        assertThat(response.rangeEnd()).isEqualTo(LocalDate.of(2026, 4, 19));
        assertThat(response.days()).hasSize(7);
        assertThat(response.days().get(2).date()).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(response.days().get(2).statusTone()).isEqualTo("done");
        assertThat(response.days().get(2).icons()).containsExactly("📚");
    }

    @Test
    void monthViewPadsCalendarAndMarksOutsideMonthDays() {
        UserEntity user = activeUser();
        LocalDate anchor = LocalDate.of(2026, 4, 15);

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                user.getId(), "canceled", LocalDate.of(2026, 3, 30), LocalDate.of(2026, 5, 3)))
                .thenReturn(List.of());
        when(dayClassificationReadService.classifyDate(any(), eq(user.getId()), eq("METROPOLE"), any()))
                .thenReturn("WORKDAY");

        AgendaRangeResponse response = agendaService.getMonth("alice@example.com", anchor);

        assertThat(response.rangeStart()).isEqualTo(LocalDate.of(2026, 3, 30));
        assertThat(response.rangeEnd()).isEqualTo(LocalDate.of(2026, 5, 3));
        assertThat(response.days()).hasSize(35);
        assertThat(response.days().getFirst().currentMonth()).isFalse();
        assertThat(response.days().stream().filter(AgendaDaySummary::currentMonth)).hasSize(30);
    }

    @Test
    void marksDaysBeforeAccountCreationAsLocked() {
        UserEntity user = activeUser();
        user.setCreatedAt(LocalDate.of(2026, 4, 10).atStartOfDay(ZoneId.of("Europe/Paris")).toInstant());
        LocalDate anchor = LocalDate.of(2026, 4, 15);

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                user.getId(), "canceled", LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 19)))
                .thenReturn(List.of());
        when(dayClassificationReadService.classifyDate(any(), eq(user.getId()), eq("METROPOLE"), any()))
                .thenReturn("WORKDAY");

        AgendaRangeResponse response = agendaService.getWeek("alice@example.com", anchor);

        assertThat(response.days().get(0).beforeAccountCreation()).isFalse();
        assertThat(response.days().stream().filter(day -> day.date().isBefore(LocalDate.of(2026, 4, 10))))
                .allMatch(AgendaDaySummary::beforeAccountCreation);
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setPseudo("alice");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setTimezoneName("Europe/Paris");
        user.setGeographicZone("METROPOLE");
        user.setCreatedAt(LocalDate.of(2026, 1, 1).atStartOfDay(ZoneId.of("Europe/Paris")).toInstant());
        return user;
    }

    private TaskDefinitionEntity definition(UUID userId, String title, String icon) {
        TaskDefinitionEntity definition = new TaskDefinitionEntity();
        definition.setId(UUID.randomUUID());
        definition.setUserId(userId);
        definition.setTitle(title);
        definition.setIcon(icon);
        definition.setTaskType("ONE_TIME");
        return definition;
    }

    private TaskOccurrenceEntity occurrence(
            UUID userId,
            UUID definitionId,
            LocalDate date,
            LocalTime time,
            String status,
            String dayCategory
    ) {
        TaskOccurrenceEntity occurrence = new TaskOccurrenceEntity();
        occurrence.setId(UUID.randomUUID());
        occurrence.setUserId(userId);
        occurrence.setTaskDefinitionId(definitionId);
        occurrence.setTaskRuleId(UUID.randomUUID());
        occurrence.setOccurrenceDate(date);
        occurrence.setOccurrenceTime(time);
        occurrence.setStatus(status);
        occurrence.setDayCategory(dayCategory);
        occurrence.setCreatedAt(java.time.Instant.now());
        occurrence.setUpdatedAt(java.time.Instant.now());
        return occurrence;
    }
}
