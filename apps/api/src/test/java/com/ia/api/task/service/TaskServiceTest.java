package com.ia.api.task.service;

import com.ia.api.auth.domain.AccountStatus;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.task.api.CreateTaskRequest;
import com.ia.api.task.api.DeleteTaskOccurrenceRequest;
import com.ia.api.task.api.TaskMutationScope;
import com.ia.api.task.api.TaskOccurrenceListRequest;
import com.ia.api.task.api.TaskOccurrencePageResponse;
import com.ia.api.task.api.TaskOccurrenceResponse;
import com.ia.api.task.api.TaskPreviewRequest;
import com.ia.api.task.api.TaskPreviewResponse;
import com.ia.api.task.api.TaskResponse;
import com.ia.api.task.api.UpdateTaskOccurrenceRequest;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.domain.TaskOverrideEntity;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskOverrideRepository;
import com.ia.api.task.repository.TaskRuleRepository;
import com.ia.api.task.repository.TaskTimeSlotRepository;
import com.ia.api.sync.service.RealtimeSyncService;
import com.ia.api.user.domain.DayCategory;
import com.ia.api.user.domain.DayProfileEntity;
import com.ia.api.user.repository.AssetRepository;
import com.ia.api.user.repository.DayProfileRepository;
import com.ia.api.user.service.ProfilePhotoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock private TaskDefinitionRepository taskDefinitionRepository;
    @Mock private TaskRuleRepository taskRuleRepository;
    @Mock private TaskOccurrenceRepository taskOccurrenceRepository;
    @Mock private TaskOverrideRepository taskOverrideRepository;
    @Mock private TaskTimeSlotRepository taskTimeSlotRepository;
    @Mock private UserRepository userRepository;
    @Mock private DayProfileRepository dayProfileRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private ProfilePhotoStorageService photoStorageService;
    @Mock private OccurrenceRefreshService occurrenceRefreshService;
    @Mock private RealtimeSyncService realtimeSyncService;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskDefinitionRepository,
                taskRuleRepository,
                taskOccurrenceRepository,
                taskOverrideRepository,
                taskTimeSlotRepository,
                userRepository,
                dayProfileRepository,
                assetRepository,
                photoStorageService,
                occurrenceRefreshService,
                realtimeSyncService
        );
    }

    @Test
    void createTaskStoresDefinitionAndRule() throws IOException {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Meditation");
        request.setIcon("🧘");
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setDayCategories(List.of("WORKDAY"));
        request.setTimeMode("FIXED");
        request.setFixedTime(LocalTime.of(7, 0));

        TaskResponse response = taskService.createTask("alice@example.com", request);

        verify(taskDefinitionRepository).save(any(TaskDefinitionEntity.class));
        verify(taskRuleRepository).save(any(TaskRuleEntity.class));
        verify(occurrenceRefreshService).refreshFutureOccurrences(any(TaskRuleEntity.class), eq(user.getId()), eq("METROPOLE"), eq(request.getStartDate()));
        verify(realtimeSyncService).publish("alice@example.com", "TASKS");

        assertThat(response.title()).isEqualTo("Meditation");
        assertThat(response.taskType()).isEqualTo("ONE_TIME");
        assertThat(response.timeMode()).isEqualTo("FIXED");
        assertThat(response.fixedTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(response.dayCategories()).containsExactly("WORKDAY");
    }

    @Test
    void createTaskRejectsPastDate() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(activeUser()));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Meditation");
        request.setIcon("🧘");
        request.setStartDate(LocalDate.now().minusDays(1));
        request.setDayCategories(List.of("WORKDAY"));
        request.setTimeMode("FIXED");
        request.setFixedTime(LocalTime.of(7, 0));

        assertThatThrownBy(() -> taskService.createTask("alice@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("passe");
    }

    @Test
    void createTaskRejectsDateBeforeAccountCreation() {
        UserEntity user = activeUser();
        user.setCreatedAt(LocalDate.now().plusDays(2).atStartOfDay(TaskService.PARIS).toInstant());
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Meditation");
        request.setIcon("ðŸ§˜");
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setDayCategories(List.of("WORKDAY"));
        request.setTimeMode("FIXED");
        request.setFixedTime(LocalTime.of(7, 0));

        assertThatThrownBy(() -> taskService.createTask("alice@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creation du compte");
    }

    @Test
    void previewReturnsFixedTimeOccurrence() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(activeUser()));

        TaskPreviewRequest request = new TaskPreviewRequest(
                LocalDate.now().plusDays(1),
                List.of("WORKDAY"),
                "FIXED",
                LocalTime.of(8, 30),
                null,
                null,
                null,
                null
        );

        TaskPreviewResponse preview = taskService.previewNextOccurrence("alice@example.com", request);

        assertThat(preview.occurrenceDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(preview.occurrenceTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(preview.occurrenceLabel()).contains("08:30");
    }

    @Test
    void previewComputesWakeUpOffsetOccurrence() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        DayProfileEntity dayProfile = new DayProfileEntity();
        dayProfile.setId(UUID.randomUUID());
        dayProfile.setUserId(user.getId());
        dayProfile.setDayCategory(DayCategory.WORKDAY);
        dayProfile.setWakeUpTime(LocalTime.of(7, 0));
        when(dayProfileRepository.findAllByUserId(user.getId())).thenReturn(List.of(dayProfile));

        TaskPreviewRequest request = new TaskPreviewRequest(
                LocalDate.now().plusDays(1),
                List.of("WORKDAY"),
                "WAKE_UP_OFFSET",
                null,
                30,
                null,
                null,
                null
        );

        TaskPreviewResponse preview = taskService.previewNextOccurrence("alice@example.com", request);

        assertThat(preview.occurrenceTime()).isEqualTo(LocalTime.of(7, 30));
        assertThat(preview.occurrenceLabel()).contains("07:30");
    }

    @Test
    void previewRejectsPastStartDate() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(activeUser()));

        TaskPreviewRequest request = new TaskPreviewRequest(
                LocalDate.now().minusDays(1),
                List.of("WORKDAY"),
                "FIXED",
                LocalTime.of(8, 0),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> taskService.previewNextOccurrence("alice@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("passe");
    }

    @Test
    void previewRejectsStartDateBeforeAccountCreation() {
        UserEntity user = activeUser();
        user.setCreatedAt(LocalDate.now().plusDays(2).atStartOfDay(TaskService.PARIS).toInstant());
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        TaskPreviewRequest request = new TaskPreviewRequest(
                LocalDate.now().plusDays(1),
                List.of("WORKDAY"),
                "FIXED",
                LocalTime.of(8, 0),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> taskService.previewNextOccurrence("alice@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creation du compte");
    }

    @Test
    void previewComputesNextWeeklyOccurrence() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(activeUser()));

        // Start on a Monday (ISO 1). Ask for Wednesdays (3) and Fridays (5).
        // The next occurrence after Monday should be Wednesday.
        LocalDate monday = LocalDate.now().plusDays(1).with(java.time.DayOfWeek.MONDAY);
        if (monday.isBefore(LocalDate.now())) {
            monday = monday.plusWeeks(1);
        }
        LocalDate expectedWednesday = monday.plusDays(2); // Monday + 2 = Wednesday

        TaskPreviewRequest request = new TaskPreviewRequest(
                monday,
                List.of("WORKDAY"),
                "FIXED",
                LocalTime.of(9, 0),
                null,
                "WEEKLY",
                List.of(3, 5),
                null
        );

        TaskPreviewResponse preview = taskService.previewNextOccurrence("alice@example.com", request);

        assertThat(preview.occurrenceDate().getDayOfWeek().getValue()).isIn(3, 5);
        assertThat(preview.occurrenceTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void previewComputesNextMonthlyOccurrence() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(activeUser()));

        // Start on the 1st of next month, request day 15.
        LocalDate startOfNextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        TaskPreviewRequest request = new TaskPreviewRequest(
                startOfNextMonth,
                List.of("WORKDAY"),
                "FIXED",
                LocalTime.of(10, 0),
                null,
                "MONTHLY",
                null,
                15
        );

        TaskPreviewResponse preview = taskService.previewNextOccurrence("alice@example.com", request);

        assertThat(preview.occurrenceDate().getDayOfMonth()).isEqualTo(15);
        assertThat(preview.occurrenceDate().getMonth()).isEqualTo(startOfNextMonth.getMonth());
    }

    @Test
    void createRecurringTaskHasCorrectTaskType() throws IOException {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(activeUser()));
        when(taskDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Sport");
        request.setIcon("🏃");
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setDayCategories(List.of("WORKDAY"));
        request.setTimeMode("FIXED");
        request.setFixedTime(LocalTime.of(6, 30));
        request.setRecurrenceType("WEEKLY");
        request.setDaysOfWeek(List.of(1, 3, 5));

        TaskResponse response = taskService.createTask("alice@example.com", request);

        assertThat(response.taskType()).isEqualTo("RECURRING");
        assertThat(response.recurrenceType()).isEqualTo("WEEKLY");
    }

    @Test
    void listOccurrencesReturnsOnlyFutureByDefaultAndPaginates() {
        UserEntity user = activeUser();
        TaskDefinitionEntity definition = definition(user.getId(), "Meditation");
        TaskRuleEntity rule = recurringRule(definition.getId());
        TaskOccurrenceEntity past = occurrence(user.getId(), definition.getId(), rule.getId(), LocalDate.now().minusDays(1), LocalTime.of(7, 0));
        TaskOccurrenceEntity future = occurrence(user.getId(), definition.getId(), rule.getId(), LocalDate.now().plusDays(2), LocalTime.of(7, 0));
        TaskOverrideEntity override = new TaskOverrideEntity();
        override.setId(UUID.randomUUID());
        override.setTaskRuleId(rule.getId());
        override.setOccurrenceDate(future.getOccurrenceDate());
        override.setTitle("Meditation override");
        override.setIcon("🔥");
        override.setDescription("override");
        override.setTimeMode("WAKE_UP_OFFSET");
        override.setWakeUpOffsetMinutes(45);
        override.setFixedTime(LocalTime.of(9, 15));

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.findAllByUserIdAndStatusNotAndOccurrenceDateGreaterThanEqualOrderByOccurrenceDateAscOccurrenceTimeAsc(
                eq(user.getId()), eq("canceled"), any())).thenReturn(List.of(future));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(definition));
        when(taskRuleRepository.findAllById(any())).thenReturn(List.of(rule));
        when(taskOverrideRepository.findAllByTaskRuleIdIn(any())).thenReturn(List.of(override));

        TaskOccurrenceListRequest request = new TaskOccurrenceListRequest();
        request.setPage(0);
        request.setSize(10);

        TaskOccurrencePageResponse response = taskService.listOccurrences("alice@example.com", request);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().pastLocked()).isFalse();
        assertThat(response.items().getFirst().title()).isEqualTo("Meditation override");
        assertThat(response.items().getFirst().timeMode()).isEqualTo("WAKE_UP_OFFSET");
        assertThat(response.items().getFirst().wakeUpOffsetMinutes()).isEqualTo(45);
        assertThat(response.items().getFirst().occurrenceTime()).isEqualTo(LocalTime.of(9, 15));
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void listOccurrencesCanIncludePastWhenDateFilterRequestsIt() {
        UserEntity user = activeUser();
        TaskDefinitionEntity definition = definition(user.getId(), "Lecture");
        TaskRuleEntity rule = recurringRule(definition.getId());
        TaskOccurrenceEntity past = occurrence(user.getId(), definition.getId(), rule.getId(), LocalDate.now().minusDays(2), LocalTime.of(7, 0));
        TaskOccurrenceEntity future = occurrence(user.getId(), definition.getId(), rule.getId(), LocalDate.now().plusDays(2), LocalTime.of(8, 0));

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                eq(user.getId()), eq("canceled"), any(), any())).thenReturn(List.of(past, future));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(definition));
        when(taskRuleRepository.findAllById(any())).thenReturn(List.of(rule));
        when(taskOverrideRepository.findAllByTaskRuleIdIn(any())).thenReturn(List.of());

        TaskOccurrenceListRequest request = new TaskOccurrenceListRequest();
        request.setPage(0);
        request.setSize(10);
        request.setOccurrenceDateFrom(LocalDate.now().minusDays(7));
        request.setOccurrenceDateTo(LocalDate.now().plusDays(7));

        TaskOccurrencePageResponse response = taskService.listOccurrences("alice@example.com", request);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().pastLocked()).isTrue();
    }

    @Test
    void updateSingleOccurrenceCreatesOverrideAndUpdatesOccurrenceTime() {
        UserEntity user = activeUser();
        TaskDefinitionEntity definition = definition(user.getId(), "Lecture");
        TaskRuleEntity rule = recurringRule(definition.getId());
        TaskOccurrenceEntity future = occurrence(user.getId(), definition.getId(), rule.getId(), LocalDate.now().plusDays(1), LocalTime.of(7, 0));

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.findById(future.getId())).thenReturn(Optional.of(future));
        when(taskDefinitionRepository.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(taskRuleRepository.findById(rule.getId())).thenReturn(Optional.of(rule));
        when(taskOverrideRepository.findByTaskRuleIdAndOccurrenceDate(rule.getId(), future.getOccurrenceDate())).thenReturn(Optional.empty());
        when(taskOverrideRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskOccurrenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTaskOccurrenceRequest request = new UpdateTaskOccurrenceRequest(
                TaskMutationScope.THIS_OCCURRENCE,
                null,
                "Lecture du soir",
                "📚",
                "Description",
                "FIXED",
                LocalTime.of(21, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        TaskOccurrenceResponse response = taskService.updateOccurrence("alice@example.com", future.getId(), request);

        assertThat(response.title()).isEqualTo("Lecture du soir");
        assertThat(response.occurrenceTime()).isEqualTo(LocalTime.of(21, 0));
        verify(taskOverrideRepository).save(any(TaskOverrideEntity.class));
        verify(taskOccurrenceRepository).save(any(TaskOccurrenceEntity.class));
        verify(realtimeSyncService).publish("alice@example.com", "TASKS");
    }

    @Test
    void updateFutureOccurrencesSplitsRuleAndRefreshesDerivedOccurrences() {
        UserEntity user = activeUser();
        TaskDefinitionEntity definition = definition(user.getId(), "Sport");
        TaskRuleEntity rule = recurringRule(definition.getId());
        TaskOccurrenceEntity future = occurrence(user.getId(), definition.getId(), rule.getId(), LocalDate.now().plusDays(3), LocalTime.of(6, 30));

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.findById(future.getId())).thenReturn(Optional.of(future));
        when(taskDefinitionRepository.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(taskRuleRepository.findById(rule.getId())).thenReturn(Optional.of(rule));
        when(taskDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskOccurrenceRepository.findMaxOccurrenceDatePerRule(any()))
                .thenReturn(Collections.singletonList(new Object[]{rule.getId(), future.getOccurrenceDate().plusDays(1)}));
        when(taskOccurrenceRepository.findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(any(), eq(future.getOccurrenceDate())))
                .thenAnswer(inv -> {
                    UUID ruleId = inv.getArgument(0);
                    if (ruleId.equals(rule.getId())) {
                        return List.of(future);
                    }
                    TaskOccurrenceEntity regenerated = occurrence(user.getId(), UUID.randomUUID(), ruleId, future.getOccurrenceDate(), LocalTime.of(8, 45));
                    return List.of(regenerated);
                });

        UpdateTaskOccurrenceRequest request = new UpdateTaskOccurrenceRequest(
                TaskMutationScope.THIS_AND_FOLLOWING,
                null,
                "Sport matin",
                "🏃",
                "Nouveau bloc",
                "FIXED",
                LocalTime.of(8, 45),
                null,
                List.of("VACATION", "WEEKEND_HOLIDAY"),
                "WEEKLY",
                List.of(2, 4, 6),
                null,
                future.getOccurrenceDate().plusWeeks(10),
                null,
                null
        );

        TaskOccurrenceResponse response = taskService.updateOccurrence("alice@example.com", future.getId(), request);

        assertThat(response.title()).isEqualTo("Sport matin");
        verify(occurrenceRefreshService).refreshFutureOccurrences(any(TaskRuleEntity.class), eq(user.getId()), eq(user.getGeographicZone()), eq(future.getOccurrenceDate()));
        verify(taskDefinitionRepository).save(any(TaskDefinitionEntity.class));
        verify(taskRuleRepository, times(2)).save(any(TaskRuleEntity.class));
    }

    @Test
    void updateFutureOccurrencesAllowsExtendingSeriesWhenNoFutureOccurrenceRemains() {
        UserEntity user = activeUser();
        TaskDefinitionEntity definition = definition(user.getId(), "Sport");
        TaskRuleEntity rule = recurringRule(definition.getId());
        TaskOccurrenceEntity future = occurrence(user.getId(), definition.getId(), rule.getId(), LocalDate.now().plusDays(3), LocalTime.of(6, 30));

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.findById(future.getId())).thenReturn(Optional.of(future));
        when(taskDefinitionRepository.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(taskRuleRepository.findById(rule.getId())).thenReturn(Optional.of(rule));
        when(taskOccurrenceRepository.findMaxOccurrenceDatePerRule(any()))
                .thenReturn(List.of());
        when(taskDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskOccurrenceRepository.findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(any(), eq(future.getOccurrenceDate())))
                .thenAnswer(inv -> {
                    UUID ruleId = inv.getArgument(0);
                    if (ruleId.equals(rule.getId())) {
                        return List.of(future);
                    }
                    TaskOccurrenceEntity regenerated = occurrence(user.getId(), UUID.randomUUID(), ruleId, future.getOccurrenceDate(), LocalTime.of(8, 45));
                    return List.of(regenerated);
                });

        UpdateTaskOccurrenceRequest request = new UpdateTaskOccurrenceRequest(
                TaskMutationScope.THIS_AND_FOLLOWING,
                null,
                "Sport matin",
                "🏃",
                "Nouveau bloc",
                "FIXED",
                LocalTime.of(8, 45),
                null,
                List.of("WORKDAY"),
                "WEEKLY",
                List.of(1, 3, 5),
                null,
                future.getOccurrenceDate().plusWeeks(4),
                null,
                null
        );

        TaskOccurrenceResponse response = taskService.updateOccurrence("alice@example.com", future.getId(), request);

        assertThat(response.title()).isEqualTo("Sport matin");
        verify(taskDefinitionRepository).save(any(TaskDefinitionEntity.class));
        verify(taskRuleRepository, times(2)).save(any(TaskRuleEntity.class));
        verify(occurrenceRefreshService).refreshFutureOccurrences(any(TaskRuleEntity.class), eq(user.getId()), eq(user.getGeographicZone()), eq(future.getOccurrenceDate()));
    }

    @Test
    void deleteRejectsPastOccurrence() {
        UserEntity user = activeUser();
        TaskOccurrenceEntity past = occurrence(user.getId(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().minusDays(1), LocalTime.of(7, 0));

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.findById(past.getId())).thenReturn(Optional.of(past));

        assertThatThrownBy(() -> taskService.deleteOccurrence(
                "alice@example.com",
                past.getId(),
                new DeleteTaskOccurrenceRequest(TaskMutationScope.THIS_OCCURRENCE, null)
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("passees");
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setPseudo("alice");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setTimezoneName("Europe/Paris");
        user.setGeographicZone("METROPOLE");
        user.setCreatedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        return user;
    }

    private TaskDefinitionEntity definition(UUID userId, String title) {
        TaskDefinitionEntity definition = new TaskDefinitionEntity();
        definition.setId(UUID.randomUUID());
        definition.setUserId(userId);
        definition.setTitle(title);
        definition.setIcon("🧘");
        definition.setDescription("desc");
        definition.setTaskType("RECURRING");
        return definition;
    }

    private TaskRuleEntity recurringRule(UUID definitionId) {
        TaskRuleEntity rule = new TaskRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setTaskDefinitionId(definitionId);
        rule.setDayCategories("WORKDAY");
        rule.setStartDate(LocalDate.now().plusDays(1));
        rule.setTimeMode("FIXED");
        rule.setFixedTime(LocalTime.of(7, 0));
        rule.setRecurrenceType("WEEKLY");
        rule.setDaysOfWeek("1,3,5");
        rule.setCreatedAt(java.time.Instant.now());
        return rule;
    }

    private TaskOccurrenceEntity occurrence(UUID userId, UUID definitionId, UUID ruleId, LocalDate date, LocalTime time) {
        TaskOccurrenceEntity occurrence = new TaskOccurrenceEntity();
        occurrence.setId(UUID.randomUUID());
        occurrence.setUserId(userId);
        occurrence.setTaskDefinitionId(definitionId);
        occurrence.setTaskRuleId(ruleId);
        occurrence.setOccurrenceDate(date);
        occurrence.setOccurrenceTime(time);
        occurrence.setStatus("planned");
        occurrence.setDayCategory("WORKDAY");
        occurrence.setCreatedAt(java.time.Instant.now());
        occurrence.setUpdatedAt(java.time.Instant.now());
        return occurrence;
    }
}
