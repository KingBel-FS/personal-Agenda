package com.ia.api.task.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TaskOccurrenceResponse(
        UUID id,
        UUID taskDefinitionId,
        UUID taskRuleId,
        String title,
        String icon,
        String description,
        String taskType,
        String timeMode,
        LocalTime fixedTime,
        Integer wakeUpOffsetMinutes,
        List<String> dayCategories,
        String recurrenceType,
        List<Integer> daysOfWeek,
        Integer dayOfMonth,
        LocalDate endDate,
        LocalTime endTime,
        LocalDate occurrenceDate,
        LocalTime occurrenceTime,
        String status,
        String dayCategory,
        boolean pastLocked,
        boolean recurring,
        boolean futureScopeAvailable,
        Integer slotOrder,
        int totalSlotsPerDay,
        List<TimeSlotSummary> timeSlots
) {
}
