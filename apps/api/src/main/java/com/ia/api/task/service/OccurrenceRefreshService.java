package com.ia.api.task.service;

import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.domain.TaskOverrideEntity;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.domain.TaskTimeSlotEntity;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskOverrideRepository;
import com.ia.api.task.repository.TaskTimeSlotRepository;
import com.ia.api.user.domain.DayCategory;
import com.ia.api.user.domain.DayProfileEntity;
import com.ia.api.user.repository.DayProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OccurrenceRefreshService {

    private static final int MINUTES_IN_DAY = 1440;

    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final TaskOverrideRepository taskOverrideRepository;
    private final TaskTimeSlotRepository taskTimeSlotRepository;
    private final DayProfileRepository dayProfileRepository;
    private final DayClassificationReadService dayClassificationReadService;
    private final int horizonDays;

    public OccurrenceRefreshService(
            TaskOccurrenceRepository taskOccurrenceRepository,
            TaskOverrideRepository taskOverrideRepository,
            TaskTimeSlotRepository taskTimeSlotRepository,
            DayProfileRepository dayProfileRepository,
            DayClassificationReadService dayClassificationReadService,
            @Value("${occurrence.materialization.horizon-days:30}") int horizonDays
    ) {
        this.taskOccurrenceRepository = taskOccurrenceRepository;
        this.taskOverrideRepository = taskOverrideRepository;
        this.taskTimeSlotRepository = taskTimeSlotRepository;
        this.dayProfileRepository = dayProfileRepository;
        this.dayClassificationReadService = dayClassificationReadService;
        this.horizonDays = horizonDays;
    }

    public void refreshFutureOccurrences(TaskRuleEntity rule, UUID userId, String geographicZone, LocalDate fromDate) {
        LocalDate today = LocalDate.now(TaskService.PARIS);
        LocalDate effectiveFrom = fromDate.isBefore(today) ? today : fromDate;
        LocalDate horizon = today.plusDays(horizonDays);

        List<TaskOccurrenceEntity> stale = taskOccurrenceRepository
                .findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(rule.getId(), effectiveFrom);
        taskOccurrenceRepository.deleteAll(stale);

        Map<DayCategory, LocalTime> wakeUpTimes = dayProfileRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(DayProfileEntity::getDayCategory, DayProfileEntity::getWakeUpTime));

        List<LocalDate> dates = computeOccurrenceDates(rule, effectiveFrom, horizon);
        List<TaskTimeSlotEntity> slots = taskTimeSlotRepository.findAllByTaskRuleIdOrderBySlotOrderAsc(rule.getId());
        boolean multiSlot = !slots.isEmpty();

        // P-8 : batch insert au lieu de N saves individuels
        List<TaskOccurrenceEntity> toSave = new ArrayList<>();

        for (LocalDate date : dates) {
            String dayCategory = dayClassificationReadService.classifyDate(date, userId, geographicZone);
            String baseStatus = ruleApplies(rule, dayCategory) ? "planned" : "skipped";

            if (multiSlot) {
                // Base occurrence : always generated from rule's own timeMode/fixedTime/wakeUpOffset
                LocalTime baseTime = computeOccurrenceTime(rule, dayCategory, wakeUpTimes);
                TaskOverrideEntity baseOverride = taskOverrideRepository
                        .findByTaskRuleIdAndOccurrenceDate(rule.getId(), date)
                        .orElse(null);
                if (baseOverride == null || !"DELETE".equals(baseOverride.getOverrideAction())) {
                    String status = baseStatus;
                    LocalTime time = baseTime;
                    if (baseOverride != null && baseOverride.getFixedTime() != null && "FIXED".equals(baseOverride.getTimeMode())) {
                        time = baseOverride.getFixedTime();
                    }
                    if (baseOverride != null && baseOverride.getOverrideStatus() != null) {
                        status = baseOverride.getOverrideStatus();
                    }
                    toSave.add(buildOccurrence(userId, rule, date, time, status, dayCategory));
                }

                // Additional slot occurrences
                LocalTime previousTime = baseTime;
                for (TaskTimeSlotEntity slot : slots) {
                    if ("EVERY_N_MINUTES".equals(slot.getTimeMode())) {
                        int intervalMinutes = slot.getAfterPreviousMinutes() != null ? Math.max(1, slot.getAfterPreviousMinutes()) : 60;
                        int prevMinutes = toMinutesOfDay(previousTime);
                        int current = prevMinutes + intervalMinutes;
                        int ceiling = rule.getEndTime() != null ? toMinutesOfDay(rule.getEndTime()) : MINUTES_IN_DAY;
                        while (current < ceiling) {
                            LocalTime slotTime = LocalTime.of(current / 60, current % 60);
                            TaskOverrideEntity override = taskOverrideRepository
                                    .findByTaskRuleIdAndOccurrenceDateAndTaskTimeSlotId(rule.getId(), date, slot.getId())
                                    .orElse(null);
                            if (override == null || !"DELETE".equals(override.getOverrideAction())) {
                                String status = baseStatus;
                                LocalTime time = slotTime;
                                if (override != null && override.getFixedTime() != null && "FIXED".equals(override.getTimeMode())) {
                                    time = override.getFixedTime();
                                }
                                if (override != null && override.getOverrideStatus() != null) {
                                    status = override.getOverrideStatus();
                                }
                                TaskOccurrenceEntity occurrence = buildOccurrence(userId, rule, date, time, status, dayCategory);
                                occurrence.setTaskTimeSlotId(slot.getId());
                                toSave.add(occurrence);
                            }
                            previousTime = slotTime;
                            current += intervalMinutes;
                        }
                    } else if ("FIXED".equals(slot.getTimeMode())) {
                        LocalTime slotTime = slot.getFixedTime() != null ? slot.getFixedTime() : LocalTime.of(8, 0);
                        TaskOverrideEntity override = taskOverrideRepository
                                .findByTaskRuleIdAndOccurrenceDateAndTaskTimeSlotId(rule.getId(), date, slot.getId())
                                .orElse(null);
                        if (override != null && "DELETE".equals(override.getOverrideAction())) {
                            continue;
                        }
                        String status = baseStatus;
                        LocalTime time = slotTime;
                        if (override != null && override.getFixedTime() != null) {
                            time = override.getFixedTime();
                        }
                        if (override != null && override.getOverrideStatus() != null) {
                            status = override.getOverrideStatus();
                        }
                        previousTime = slotTime;
                        TaskOccurrenceEntity occurrence = buildOccurrence(userId, rule, date, time, status, dayCategory);
                        occurrence.setTaskTimeSlotId(slot.getId());
                        toSave.add(occurrence);
                    }
                    // Legacy modes (WAKE_UP_OFFSET, AFTER_PREVIOUS) kept for DB backward compat — skipped silently
                }
            } else {
                // Comportement legacy mono-slot (taskTimeSlotId = null)
                TaskOverrideEntity override = taskOverrideRepository
                        .findByTaskRuleIdAndOccurrenceDate(rule.getId(), date)
                        .orElse(null);
                if (override != null && "DELETE".equals(override.getOverrideAction())) {
                    continue;
                }

                String status = baseStatus;
                LocalTime time = computeOccurrenceTime(rule, dayCategory, wakeUpTimes);

                // P-2 : n'appliquer le fixedTime de l'override que si timeMode=FIXED
                if (override != null && override.getFixedTime() != null && "FIXED".equals(override.getTimeMode())) {
                    time = override.getFixedTime();
                }
                if (override != null && override.getOverrideStatus() != null) {
                    status = override.getOverrideStatus();
                }

                toSave.add(buildOccurrence(userId, rule, date, time, status, dayCategory));
            }
        }

        taskOccurrenceRepository.saveAll(toSave);
    }

    private int toMinutesOfDay(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private TaskOccurrenceEntity buildOccurrence(UUID userId, TaskRuleEntity rule, LocalDate date,
                                                  LocalTime time, String status, String dayCategory) {
        TaskOccurrenceEntity occurrence = new TaskOccurrenceEntity();
        occurrence.setId(UUID.randomUUID());
        occurrence.setUserId(userId);
        occurrence.setTaskDefinitionId(rule.getTaskDefinitionId());
        occurrence.setTaskRuleId(rule.getId());
        occurrence.setOccurrenceDate(date);
        occurrence.setOccurrenceTime(time);
        occurrence.setStatus(status);
        occurrence.setDayCategory(dayCategory);
        occurrence.setCreatedAt(Instant.now());
        occurrence.setUpdatedAt(Instant.now());
        return occurrence;
    }

    private boolean ruleApplies(TaskRuleEntity rule, String dayCategory) {
        // P-13 : trim pour éviter les espaces résiduels dans le CSV
        return List.of(rule.getDayCategories().split(","))
                .stream()
                .map(String::trim)
                .toList()
                .contains(dayCategory);
    }

    private LocalTime computeOccurrenceTime(
            TaskRuleEntity rule,
            String dayCategory,
            Map<DayCategory, LocalTime> wakeUpTimes
    ) {
        if ("FIXED".equals(rule.getTimeMode())) {
            return rule.getFixedTime() != null ? rule.getFixedTime() : LocalTime.of(8, 0);
        }
        DayCategory category = DayCategory.valueOf(dayCategory);
        LocalTime wakeUp = wakeUpTimes.getOrDefault(category, LocalTime.of(7, 0));
        int offset = rule.getWakeUpOffsetMinutes() != null ? rule.getWakeUpOffsetMinutes() : 0;
        return wakeUp.plusMinutes(offset);
    }

    List<LocalDate> computeOccurrenceDates(TaskRuleEntity rule, LocalDate today, LocalDate horizon) {
        LocalDate effectiveStart = rule.getStartDate().isBefore(today) ? today : rule.getStartDate();
        LocalDate effectiveEnd = (rule.getEndDate() != null && rule.getEndDate().isBefore(horizon))
                ? rule.getEndDate()
                : horizon;

        if ("WEEKLY".equals(rule.getRecurrenceType())) {
            List<Integer> daysOfWeek = rule.getDaysOfWeek() == null || rule.getDaysOfWeek().isBlank()
                    ? List.of()
                    : java.util.Arrays.stream(rule.getDaysOfWeek().split(","))
                            .map(s -> Integer.parseInt(s.trim()))
                            .toList();
            List<LocalDate> dates = new ArrayList<>();
            LocalDate current = effectiveStart;
            while (!current.isAfter(effectiveEnd)) {
                if (daysOfWeek.contains(current.getDayOfWeek().getValue())) {
                    dates.add(current);
                }
                current = current.plusDays(1);
            }
            return dates;
        }

        if ("MONTHLY".equals(rule.getRecurrenceType()) && rule.getDayOfMonth() != null) {
            List<LocalDate> dates = new ArrayList<>();
            YearMonth current = YearMonth.from(effectiveStart);
            YearMonth endMonth = YearMonth.from(effectiveEnd);
            while (!current.isAfter(endMonth)) {
                int day = Math.min(rule.getDayOfMonth(), current.lengthOfMonth());
                LocalDate candidate = current.atDay(day);
                if (!candidate.isBefore(effectiveStart) && !candidate.isAfter(effectiveEnd)) {
                    dates.add(candidate);
                }
                current = current.plusMonths(1);
            }
            return dates;
        }

        if (!rule.getStartDate().isBefore(today) && !rule.getStartDate().isAfter(horizon)) {
            return List.of(rule.getStartDate());
        }
        return List.of();
    }
}
