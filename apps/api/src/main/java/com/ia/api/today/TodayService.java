package com.ia.api.today;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.domain.TaskOverrideEntity;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskOverrideRepository;
import com.ia.api.task.repository.TaskRuleRepository;
import com.ia.api.task.service.DayClassificationReadService;
import com.ia.api.task.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TodayService {

    private static final String STATUS_CANCELED = "canceled";
    private static final String STATUS_PLANNED   = "planned";
    private static final String STATUS_SKIPPED   = "skipped";

    private final UserRepository userRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskOverrideRepository taskOverrideRepository;
    private final TaskRuleRepository taskRuleRepository;
    private final DayClassificationReadService dayClassificationReadService;
    private final StreakService streakService;

    public TodayService(
            UserRepository userRepository,
            TaskOccurrenceRepository taskOccurrenceRepository,
            TaskDefinitionRepository taskDefinitionRepository,
            TaskOverrideRepository taskOverrideRepository,
            TaskRuleRepository taskRuleRepository,
            DayClassificationReadService dayClassificationReadService,
            StreakService streakService
    ) {
        this.userRepository = userRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskOverrideRepository = taskOverrideRepository;
        this.taskRuleRepository = taskRuleRepository;
        this.dayClassificationReadService = dayClassificationReadService;
        this.streakService = streakService;
    }

    public TodayResponse getToday(String email) {
        return getForDate(email, LocalDate.now(TaskService.PARIS), true);
    }

    public TodayResponse getForDate(String email, LocalDate date) {
        return getForDate(email, date, false);
    }

    private TodayResponse getForDate(String email, LocalDate date, boolean recalculateStreak) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        LocalDate accountCreationDate = user.getCreatedAt() != null
                ? user.getCreatedAt().atZone(TaskService.PARIS).toLocalDate()
                : LocalDate.MIN;
        boolean beforeAccountCreation = date.isBefore(accountCreationDate);

        String dayCategory = dayClassificationReadService.classifyDate(date, user.getId(), user.getGeographicZone());

        List<TaskOccurrenceEntity> raw = taskOccurrenceRepository
                .findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                        user.getId(), STATUS_CANCELED, date, date);

        // Compute streak info
        StreakService.StreakResult streakResult = recalculateStreak
                ? streakService.recalculate(user.getId())
                : streakService.getLatest(user.getId());
        TodayResponse.StreakInfo streakInfo = new TodayResponse.StreakInfo(
                streakResult.currentStreak(),
                streakResult.longestStreak(),
                streakResult.streakActive(),
                streakResult.allBadges()
        );
        List<String> newBadges = streakResult.newBadges();

        if (raw.isEmpty()) {
            return new TodayResponse(date.toString(), beforeAccountCreation, dayCategory, 0, 0, 0, 0, 0, 0, List.of(),
                    streakInfo, newBadges);
        }

        // Batch-load definitions
        Set<UUID> defIds = raw.stream()
                .map(TaskOccurrenceEntity::getTaskDefinitionId)
                .collect(Collectors.toSet());
        Map<UUID, TaskDefinitionEntity> defMap = taskDefinitionRepository.findAllById(defIds)
                .stream()
                .collect(Collectors.toMap(TaskDefinitionEntity::getId, d -> d));

        // Batch-load rules (to determine recurring vs one-time)
        Set<UUID> ruleIds = raw.stream()
                .map(TaskOccurrenceEntity::getTaskRuleId)
                .collect(Collectors.toSet());
        Map<UUID, TaskRuleEntity> ruleMap = taskRuleRepository.findAllById(ruleIds)
                .stream()
                .collect(Collectors.toMap(TaskRuleEntity::getId, r -> r));

        // Batch-load overrides
        Map<String, TaskOverrideEntity> overrideMap = taskOverrideRepository
                .findAllByTaskRuleIdInAndOccurrenceDateBetween(ruleIds, date, date)
                .stream()
                .collect(Collectors.toMap(
                        o -> o.getTaskRuleId() + "::" + o.getOccurrenceDate() + "::" + o.getTaskTimeSlotId(),
                        o -> o,
                        (a, b) -> a
                ));

        // Group occurrences by (ruleId::date) for slot ordering
        Map<String, List<TaskOccurrenceEntity>> groups = raw.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getTaskRuleId() + "::" + o.getOccurrenceDate()
                ));

        Map<UUID, Integer> slotOrderMap = new HashMap<>();
        for (List<TaskOccurrenceEntity> group : groups.values()) {
            if (group.size() == 1) {
                slotOrderMap.put(group.get(0).getId(), null);
            } else {
                List<TaskOccurrenceEntity> sorted = group.stream()
                        .sorted(Comparator.comparing(TaskOccurrenceEntity::getOccurrenceTime))
                        .toList();
                for (int i = 0; i < sorted.size(); i++) {
                    slotOrderMap.put(sorted.get(i).getId(), i + 1);
                }
            }
        }

        List<TodayOccurrenceItem> occurrences = raw.stream()
                .sorted(Comparator.comparing(TaskOccurrenceEntity::getOccurrenceTime))
                .map(o -> {
                    String overrideKey = o.getTaskRuleId() + "::" + o.getOccurrenceDate() + "::" + o.getTaskTimeSlotId();
                    TaskOverrideEntity override = overrideMap.get(overrideKey);
                    TaskDefinitionEntity def = defMap.get(o.getTaskDefinitionId());

                    String title = (override != null && override.getTitle() != null)
                            ? override.getTitle()
                            : (def != null ? def.getTitle() : "");
                    String description = (override != null && override.getDescription() != null)
                            ? override.getDescription()
                            : (def != null ? def.getDescription() : "");
                    String icon = (override != null && override.getIcon() != null)
                            ? override.getIcon()
                            : (def != null ? def.getIcon() : "");

                    int total = groups.get(o.getTaskRuleId() + "::" + o.getOccurrenceDate()).size();
                    Integer slotOrder = slotOrderMap.get(o.getId());

                    TaskRuleEntity rule = ruleMap.get(o.getTaskRuleId());
                    boolean recurring = rule != null && rule.getRecurrenceType() != null;

                    return new TodayOccurrenceItem(
                            o.getId(),
                            o.getTaskDefinitionId(),
                            title != null ? title : "",
                            description != null ? description : "",
                            icon != null ? icon : "",
                            o.getOccurrenceTime().toString().substring(0, 5),
                            o.getStatus(),
                            o.getDayCategory(),
                            recurring,
                            slotOrder,
                            total
                    );
                })
                .toList();

        int active    = (int) occurrences.stream().filter(o -> STATUS_PLANNED.equals(o.status())).count();
        int skipped   = (int) occurrences.stream().filter(o -> STATUS_SKIPPED.equals(o.status())).count();
        int done      = (int) occurrences.stream().filter(o -> "done".equals(o.status())).count();
        int missed    = (int) occurrences.stream().filter(o -> "missed".equals(o.status())).count();
        int total     = occurrences.size();
        int progress  = total > 0 ? done * 100 / total : 0;

        return new TodayResponse(
                date.toString(),
                beforeAccountCreation,
                dayCategory,
                active,
                skipped,
                done,
                missed,
                total,
                progress,
                occurrences,
                streakInfo,
                newBadges
        );
    }
}
