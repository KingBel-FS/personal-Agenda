package com.ia.api.goal.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.goal.api.CreateGoalRequest;
import com.ia.api.goal.api.GoalListResponse;
import com.ia.api.goal.api.GoalResponse;
import com.ia.api.goal.api.UpdateGoalRequest;
import com.ia.api.goal.domain.GoalEntity;
import com.ia.api.goal.repository.GoalRepository;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private GoalRepository goalRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskDefinitionRepository taskDefinitionRepository;
    @Mock private TaskRuleRepository taskRuleRepository;
    @Mock private TaskOccurrenceRepository taskOccurrenceRepository;

    private GoalService goalService;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        goalService = new GoalService(
                goalRepository,
                userRepository,
                taskDefinitionRepository,
                taskRuleRepository,
                taskOccurrenceRepository
        );
        user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setCreatedAt(Instant.parse("2026-03-24T10:00:00Z"));
    }

    @Test
    void createGlobalWeeklyGoalStoresGoal() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());
        when(taskRuleRepository.findAllByTaskDefinitionIdIn(any())).thenReturn(List.of());
        when(goalRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(goalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskOccurrenceRepository.countByUserIdAndOccurrenceDateBetweenAndStatus(eq(user.getId()), any(), any(), eq("done")))
                .thenReturn(3L);

        GoalResponse response = goalService.createGoal("alice@example.com", new CreateGoalRequest(
                "GLOBAL", "WEEKLY", 5, null
        ));

        verify(goalRepository).save(any(GoalEntity.class));
        assertThat(response.goalScope()).isEqualTo("GLOBAL");
        assertThat(response.periodType()).isEqualTo("WEEKLY");
        assertThat(response.currentProgress().completedCount()).isEqualTo(3);
    }

    @Test
    void createTaskGoalRejectsOneTimeTask() {
        UUID taskId = UUID.randomUUID();
        TaskDefinitionEntity definition = taskDefinition(taskId, "ONE_TIME");

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(definition));
        when(taskRuleRepository.findAllByTaskDefinitionIdIn(any())).thenReturn(List.of());

        assertThatThrownBy(() -> goalService.createGoal("alice@example.com", new CreateGoalRequest(
                "TASK", "WEEKLY", 4, taskId
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("récurrentes éligibles");
    }

    @Test
    void listGoalsBuildsCurrentProgressAndHistory() {
        UUID taskId = UUID.randomUUID();
        TaskDefinitionEntity definition = taskDefinition(taskId, "RECURRING");
        TaskRuleEntity rule = taskRule(taskId, "WEEKLY");
        GoalEntity goal = goal(taskId, "TASK", "WEEKLY", 4);
        GoalEntity inactiveGoal = goal(null, "GLOBAL", "MONTHLY", 6);
        inactiveGoal.setActive(false);

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(definition));
        when(taskRuleRepository.findAllByTaskDefinitionIdIn(any())).thenReturn(List.of(rule));
        when(goalRepository.findAllByUserId(user.getId())).thenReturn(List.of(goal, inactiveGoal));
        when(taskOccurrenceRepository.countByUserIdAndTaskDefinitionIdAndOccurrenceDateBetweenAndStatus(
                eq(user.getId()), eq(taskId), any(), any(), eq("done")
        )).thenReturn(2L, 4L, 3L, 1L, 0L);
        when(taskOccurrenceRepository.countByUserIdAndOccurrenceDateBetweenAndStatus(
                eq(user.getId()), any(), any(), eq("done")
        )).thenReturn(1L, 0L, 2L, 4L, 3L);

        GoalListResponse response = goalService.listGoals("alice@example.com");

        assertThat(response.goals()).hasSize(1);
        assertThat(response.goals().getFirst().taskTitle()).isEqualTo("Lecture");
        assertThat(response.goals().getFirst().recentHistory()).hasSize(4);
        assertThat(response.inactiveGoals()).hasSize(1);
        assertThat(response.inactiveGoals().getFirst().active()).isFalse();
        assertThat(response.eligibleTasks()).hasSize(1);
        assertThat(response.accountCreatedAt()).isNotBlank();
    }

    @Test
    void updateGoalChangesTargetAndActiveFlag() {
        UUID taskId = UUID.randomUUID();
        GoalEntity goal = goal(taskId, "TASK", "MONTHLY", 6);
        goal.setId(UUID.randomUUID());
        TaskDefinitionEntity definition = taskDefinition(taskId, "RECURRING");
        TaskRuleEntity rule = taskRule(taskId, "MONTHLY");

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(goalRepository.findByIdAndUserId(goal.getId(), user.getId())).thenReturn(Optional.of(goal));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(definition));
        when(taskRuleRepository.findAllByTaskDefinitionIdIn(any())).thenReturn(List.of(rule));
        when(goalRepository.findAllByUserId(user.getId())).thenReturn(List.of(goal));
        when(goalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskOccurrenceRepository.countByUserIdAndTaskDefinitionIdAndOccurrenceDateBetweenAndStatus(
                eq(user.getId()), eq(taskId), any(), any(), eq("done")
        )).thenReturn(5L);

        GoalResponse response = goalService.updateGoal("alice@example.com", goal.getId(), new UpdateGoalRequest(
                "TASK", "MONTHLY", 8, taskId, false
        ));

        assertThat(response.targetCount()).isEqualTo(8);
        assertThat(response.active()).isFalse();
    }

    @Test
    void findUnmetGoalRemindersReturnsOnlyUnmetGoals() {
        UUID taskId = UUID.randomUUID();
        TaskDefinitionEntity definition = taskDefinition(taskId, "RECURRING");
        TaskRuleEntity rule = taskRule(taskId, "WEEKLY");
        GoalEntity unmet = goal(taskId, "TASK", "WEEKLY", 4);
        GoalEntity met = goal(null, "GLOBAL", "WEEKLY", 2);

        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(definition));
        when(taskRuleRepository.findAllByTaskDefinitionIdIn(any())).thenReturn(List.of(rule));
        when(goalRepository.findAllByUserIdAndActiveTrueOrderByPeriodTypeAscCreatedAtAsc(user.getId())).thenReturn(List.of(unmet, met));
        when(taskOccurrenceRepository.countByUserIdAndTaskDefinitionIdAndOccurrenceDateBetweenAndStatus(
                eq(user.getId()), eq(taskId), any(), any(), eq("done")
        )).thenReturn(1L);
        when(taskOccurrenceRepository.countByUserIdAndOccurrenceDateBetweenAndStatus(eq(user.getId()), any(), any(), eq("done")))
                .thenReturn(2L);

        var projections = goalService.findUnmetGoalReminders(user.getId(), "WEEKLY", LocalDate.of(2026, 3, 29));

        assertThat(projections).hasSize(1);
        assertThat(projections.getFirst().taskTitle()).isEqualTo("Lecture");
    }

    private TaskDefinitionEntity taskDefinition(UUID taskId, String taskType) {
        TaskDefinitionEntity definition = new TaskDefinitionEntity();
        definition.setId(taskId);
        definition.setUserId(user.getId());
        definition.setTitle("Lecture");
        definition.setIcon("📚");
        definition.setTaskType(taskType);
        definition.setCreatedAt(Instant.now());
        definition.setUpdatedAt(Instant.now());
        return definition;
    }

    private TaskRuleEntity taskRule(UUID taskId, String recurrenceType) {
        TaskRuleEntity rule = new TaskRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setTaskDefinitionId(taskId);
        rule.setRecurrenceType(recurrenceType);
        return rule;
    }

    private GoalEntity goal(UUID taskId, String scope, String periodType, int targetCount) {
        GoalEntity goal = new GoalEntity();
        goal.setId(UUID.randomUUID());
        goal.setUserId(user.getId());
        goal.setGoalScope(scope);
        goal.setPeriodType(periodType);
        goal.setTargetCount(targetCount);
        goal.setTaskDefinitionId(taskId);
        goal.setActive(true);
        goal.setCreatedAt(Instant.now());
        goal.setUpdatedAt(Instant.now());
        return goal;
    }
}
