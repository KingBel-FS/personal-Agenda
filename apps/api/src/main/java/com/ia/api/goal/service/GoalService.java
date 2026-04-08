package com.ia.api.goal.service;

import com.ia.api.auth.api.MessageResponse;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.goal.api.CreateGoalRequest;
import com.ia.api.goal.api.GoalEligibleTaskItem;
import com.ia.api.goal.api.GoalListResponse;
import com.ia.api.goal.api.GoalProgressHistoryItem;
import com.ia.api.goal.api.GoalProgressSnapshot;
import com.ia.api.goal.api.GoalResponse;
import com.ia.api.goal.api.UpdateGoalRequest;
import com.ia.api.goal.domain.GoalEntity;
import com.ia.api.goal.repository.GoalRepository;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskRuleRepository;
import com.ia.api.task.service.TaskService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskRuleRepository taskRuleRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;

    public GoalService(
            GoalRepository goalRepository,
            UserRepository userRepository,
            TaskDefinitionRepository taskDefinitionRepository,
            TaskRuleRepository taskRuleRepository,
            TaskOccurrenceRepository taskOccurrenceRepository
    ) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskRuleRepository = taskRuleRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
    }

    public GoalListResponse listGoals(String email) {
        UserEntity user = getUser(email);
        LocalDate today = LocalDate.now(TaskService.PARIS);
        List<TaskDefinitionEntity> definitions = taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        Map<UUID, TaskDefinitionEntity> definitionMap = definitions.stream()
                .collect(Collectors.toMap(TaskDefinitionEntity::getId, Function.identity()));
        Map<UUID, TaskRuleEntity> ruleMap = taskRuleRepository.findAllByTaskDefinitionIdIn(definitionMap.keySet()).stream()
                .collect(Collectors.toMap(TaskRuleEntity::getTaskDefinitionId, Function.identity()));

        List<GoalEntity> allGoals = goalRepository.findAllByUserId(user.getId());

        List<GoalResponse> goals = allGoals.stream()
                .filter(GoalEntity::isActive)
                .sorted(Comparator.comparing(GoalEntity::getPeriodType).thenComparing(GoalEntity::getCreatedAt))
                .map(goal -> toResponse(goal, definitionMap, ruleMap, today))
                .toList();

        List<GoalResponse> inactiveGoals = allGoals.stream()
                .filter(goal -> !goal.isActive())
                .sorted(Comparator.comparing(GoalEntity::getPeriodType).thenComparing(GoalEntity::getCreatedAt))
                .map(goal -> toResponse(goal, definitionMap, ruleMap, today))
                .toList();

        List<GoalEligibleTaskItem> eligibleTasks = buildEligibleTaskItems(definitions, ruleMap);
        return new GoalListResponse(goals, inactiveGoals, eligibleTasks, user.getCreatedAt().toString());
    }

    @Transactional
    public GoalResponse createGoal(String email, CreateGoalRequest request) {
        UserEntity user = getUser(email);
        List<TaskDefinitionEntity> definitions = taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        Map<UUID, TaskDefinitionEntity> definitionMap = definitions.stream()
                .collect(Collectors.toMap(TaskDefinitionEntity::getId, Function.identity()));
        Map<UUID, TaskRuleEntity> ruleMap = taskRuleRepository.findAllByTaskDefinitionIdIn(definitionMap.keySet()).stream()
                .collect(Collectors.toMap(TaskRuleEntity::getTaskDefinitionId, Function.identity()));

        GoalDraft draft = validateDraft(user.getId(), request.goalScope(), request.periodType(), request.targetCount(), request.taskDefinitionId(), true, definitionMap, ruleMap);

        GoalEntity goal = new GoalEntity();
        goal.setId(UUID.randomUUID());
        goal.setUserId(user.getId());
        goal.setGoalScope(draft.goalScope());
        goal.setPeriodType(draft.periodType());
        goal.setTargetCount(request.targetCount());
        goal.setTaskDefinitionId(draft.taskDefinitionId());
        goal.setActive(true);
        goal.setCreatedAt(Instant.now());
        goal.setUpdatedAt(Instant.now());
        goalRepository.save(goal);

        return toResponse(goal, definitionMap, ruleMap, LocalDate.now(TaskService.PARIS));
    }

    @Transactional
    public GoalResponse updateGoal(String email, UUID goalId, UpdateGoalRequest request) {
        UserEntity user = getUser(email);
        GoalEntity goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Objectif introuvable."));

        List<TaskDefinitionEntity> definitions = taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        Map<UUID, TaskDefinitionEntity> definitionMap = definitions.stream()
                .collect(Collectors.toMap(TaskDefinitionEntity::getId, Function.identity()));
        Map<UUID, TaskRuleEntity> ruleMap = taskRuleRepository.findAllByTaskDefinitionIdIn(definitionMap.keySet()).stream()
                .collect(Collectors.toMap(TaskRuleEntity::getTaskDefinitionId, Function.identity()));

        GoalDraft draft = validateDraft(user.getId(), request.goalScope(), request.periodType(), request.targetCount(), request.taskDefinitionId(), false, definitionMap, ruleMap);
        ensureNoDuplicate(user.getId(), goalId, draft.goalScope(), draft.periodType(), draft.taskDefinitionId());

        goal.setGoalScope(draft.goalScope());
        goal.setPeriodType(draft.periodType());
        goal.setTargetCount(request.targetCount());
        goal.setTaskDefinitionId(draft.taskDefinitionId());
        goal.setActive(request.active());
        goal.setUpdatedAt(Instant.now());
        goalRepository.save(goal);

        return toResponse(goal, definitionMap, ruleMap, LocalDate.now(TaskService.PARIS));
    }

    @Transactional
    public MessageResponse deleteGoal(String email, UUID goalId) {
        UserEntity user = getUser(email);
        GoalEntity goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Objectif introuvable."));
        goalRepository.delete(goal);
        return new MessageResponse("Objectif supprimé.");
    }

    public List<UnmetGoalReminderProjection> findUnmetGoalReminders(UUID userId, String periodType, LocalDate anchorDate) {
        List<TaskDefinitionEntity> definitions = taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, TaskDefinitionEntity> definitionMap = definitions.stream()
                .collect(Collectors.toMap(TaskDefinitionEntity::getId, Function.identity()));
        Map<UUID, TaskRuleEntity> ruleMap = taskRuleRepository.findAllByTaskDefinitionIdIn(definitionMap.keySet()).stream()
                .collect(Collectors.toMap(TaskRuleEntity::getTaskDefinitionId, Function.identity()));

        return goalRepository.findAllByUserIdAndActiveTrueOrderByPeriodTypeAscCreatedAtAsc(userId).stream()
                .filter(goal -> goal.getPeriodType().equals(periodType))
                .map(goal -> {
                    ProgressComputation progress = computeProgress(goal, anchorDate);
                    if (progress.goalMet()) {
                        return null;
                    }
                    TaskDefinitionEntity definition = goal.getTaskDefinitionId() != null ? definitionMap.get(goal.getTaskDefinitionId()) : null;
                    TaskRuleEntity rule = goal.getTaskDefinitionId() != null ? ruleMap.get(goal.getTaskDefinitionId()) : null;
                    return new UnmetGoalReminderProjection(
                            goal.getId(),
                            goal.getGoalScope(),
                            goal.getPeriodType(),
                            goal.getTargetCount(),
                            progress.completedCount(),
                            progress.periodStart(),
                            progress.periodEnd(),
                            goal.getTaskDefinitionId(),
                            definition != null ? definition.getTitle() : null,
                            definition != null ? definition.getIcon() : null,
                            rule != null ? rule.getRecurrenceType() : null
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private GoalResponse toResponse(
            GoalEntity goal,
            Map<UUID, TaskDefinitionEntity> definitionMap,
            Map<UUID, TaskRuleEntity> ruleMap,
            LocalDate anchorDate
    ) {
        ProgressComputation currentProgress = computeProgress(goal, anchorDate);
        TaskDefinitionEntity definition = goal.getTaskDefinitionId() != null ? definitionMap.get(goal.getTaskDefinitionId()) : null;
        TaskRuleEntity rule = goal.getTaskDefinitionId() != null ? ruleMap.get(goal.getTaskDefinitionId()) : null;

        List<GoalProgressHistoryItem> history = new ArrayList<>();
        LocalDate historyAnchor = previousPeriodAnchor(goal.getPeriodType(), currentProgress.periodStart());
        for (int i = 0; i < 4; i++) {
            ProgressComputation item = computeProgress(goal, historyAnchor);
            history.add(new GoalProgressHistoryItem(
                    item.periodStart().toString(),
                    item.periodEnd().toString(),
                    item.completedCount(),
                    goal.getTargetCount(),
                    item.progressPercent(),
                    item.goalMet()
            ));
            historyAnchor = previousPeriodAnchor(goal.getPeriodType(), item.periodStart());
        }

        return new GoalResponse(
                goal.getId(),
                goal.getGoalScope(),
                goal.getPeriodType(),
                goal.getTargetCount(),
                goal.isActive(),
                goal.getTaskDefinitionId(),
                definition != null ? definition.getTitle() : null,
                definition != null ? definition.getIcon() : null,
                rule != null ? rule.getRecurrenceType() : null,
                new GoalProgressSnapshot(
                        currentProgress.periodStart().toString(),
                        currentProgress.periodEnd().toString(),
                        currentProgress.completedCount(),
                        goal.getTargetCount(),
                        Math.max(goal.getTargetCount() - currentProgress.completedCount(), 0),
                        currentProgress.progressPercent(),
                        currentProgress.goalMet(),
                        currentProgress.status()
                ),
                history,
                goal.getCreatedAt().toString(),
                goal.getUpdatedAt().toString()
        );
    }

    private List<GoalEligibleTaskItem> buildEligibleTaskItems(
            List<TaskDefinitionEntity> definitions,
            Map<UUID, TaskRuleEntity> ruleMap
    ) {
        return definitions.stream()
                .filter(definition -> "RECURRING".equals(definition.getTaskType()))
                .filter(definition -> {
                    TaskRuleEntity rule = ruleMap.get(definition.getId());
                    return rule != null && rule.getRecurrenceType() != null;
                })
                .map(definition -> {
                    TaskRuleEntity rule = ruleMap.get(definition.getId());
                    return new GoalEligibleTaskItem(
                            definition.getId(),
                            definition.getTitle(),
                            definition.getIcon(),
                            rule.getRecurrenceType()
                    );
                })
                .toList();
    }

    private GoalDraft validateDraft(
            UUID userId,
            String goalScopeRaw,
            String periodTypeRaw,
            int targetCount,
            UUID taskDefinitionId,
            boolean creating,
            Map<UUID, TaskDefinitionEntity> definitionMap,
            Map<UUID, TaskRuleEntity> ruleMap
    ) {
        String goalScope = normalize(goalScopeRaw);
        String periodType = normalize(periodTypeRaw);

        if (!List.of("GLOBAL", "TASK").contains(goalScope)) {
            throw new IllegalArgumentException("Le scope d'objectif est invalide.");
        }
        if (!List.of("WEEKLY", "MONTHLY").contains(periodType)) {
            throw new IllegalArgumentException("La période d'objectif est invalide.");
        }
        if (targetCount <= 0) {
            throw new IllegalArgumentException("La cible d'objectif doit être supérieure à zéro.");
        }
        if ("GLOBAL".equals(goalScope) && taskDefinitionId != null) {
            throw new IllegalArgumentException("Un objectif global ne doit pas cibler une tâche.");
        }
        if ("TASK".equals(goalScope) && taskDefinitionId == null) {
            throw new IllegalArgumentException("Un objectif par tâche doit cibler une tâche éligible.");
        }

        if ("TASK".equals(goalScope)) {
            TaskDefinitionEntity definition = definitionMap.get(taskDefinitionId);
            if (definition == null || !definition.getUserId().equals(userId)) {
                throw new IllegalArgumentException("La tâche ciblée est introuvable.");
            }
            TaskRuleEntity rule = ruleMap.get(taskDefinitionId);
            if (!"RECURRING".equals(definition.getTaskType()) || rule == null || rule.getRecurrenceType() == null) {
                throw new IllegalArgumentException("Seules les tâches récurrentes éligibles peuvent porter un objectif.");
            }
        }

        if (creating) {
            ensureNoDuplicate(userId, null, goalScope, periodType, taskDefinitionId);
        }

        return new GoalDraft(goalScope, periodType, taskDefinitionId);
    }

    private void ensureNoDuplicate(UUID userId, UUID currentGoalId, String goalScope, String periodType, UUID taskDefinitionId) {
        boolean duplicate = goalRepository.findAllByUserId(userId).stream()
                .filter(goal -> currentGoalId == null || !goal.getId().equals(currentGoalId))
                .filter(GoalEntity::isActive)
                .anyMatch(goal ->
                        goal.getGoalScope().equals(goalScope)
                                && goal.getPeriodType().equals(periodType)
                                && java.util.Objects.equals(goal.getTaskDefinitionId(), taskDefinitionId)
                );
        if (duplicate) {
            throw new IllegalArgumentException("Un objectif identique existe déjà pour cette période.");
        }
    }

    private ProgressComputation computeProgress(GoalEntity goal, LocalDate anchorDate) {
        LocalDate periodStart = periodStart(goal.getPeriodType(), anchorDate);
        LocalDate periodEnd = periodEnd(goal.getPeriodType(), anchorDate);
        long completedCount = goal.getTaskDefinitionId() == null
                ? taskOccurrenceRepository.countByUserIdAndOccurrenceDateBetweenAndStatus(goal.getUserId(), periodStart, periodEnd, "done")
                : taskOccurrenceRepository.countByUserIdAndTaskDefinitionIdAndOccurrenceDateBetweenAndStatus(goal.getUserId(), goal.getTaskDefinitionId(), periodStart, periodEnd, "done");
        int percent = Math.min((int) ((completedCount * 100) / goal.getTargetCount()), 100);
        boolean goalMet = completedCount >= goal.getTargetCount();
        String status = goalMet ? "ACHIEVED" : "IN_PROGRESS";
        return new ProgressComputation(periodStart, periodEnd, (int) completedCount, percent, goalMet, status);
    }

    private LocalDate periodStart(String periodType, LocalDate anchorDate) {
        return switch (periodType) {
            case "WEEKLY" -> anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "MONTHLY" -> anchorDate.withDayOfMonth(1);
            default -> throw new IllegalArgumentException("Période d'objectif inconnue.");
        };
    }

    private LocalDate periodEnd(String periodType, LocalDate anchorDate) {
        return switch (periodType) {
            case "WEEKLY" -> periodStart(periodType, anchorDate).plusDays(6);
            case "MONTHLY" -> anchorDate.with(TemporalAdjusters.lastDayOfMonth());
            default -> throw new IllegalArgumentException("Période d'objectif inconnue.");
        };
    }

    private LocalDate previousPeriodAnchor(String periodType, LocalDate currentPeriodStart) {
        return switch (periodType) {
            case "WEEKLY" -> currentPeriodStart.minusWeeks(1);
            case "MONTHLY" -> currentPeriodStart.minusMonths(1);
            default -> throw new IllegalArgumentException("Période d'objectif inconnue.");
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private UserEntity getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
    }

    private record GoalDraft(String goalScope, String periodType, UUID taskDefinitionId) {}

    private record ProgressComputation(
            LocalDate periodStart,
            LocalDate periodEnd,
            int completedCount,
            int progressPercent,
            boolean goalMet,
            String status
    ) {}

    public record UnmetGoalReminderProjection(
            UUID goalId,
            String goalScope,
            String periodType,
            int targetCount,
            int completedCount,
            LocalDate periodStart,
            LocalDate periodEnd,
            UUID taskDefinitionId,
            String taskTitle,
            String taskIcon,
            String recurrenceType
    ) {}
}
