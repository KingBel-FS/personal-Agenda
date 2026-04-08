package com.ia.api.task.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String icon,
        String description,
        String taskType,
        LocalDate startDate,
        List<String> dayCategories,
        String timeMode,
        LocalTime fixedTime,
        Integer wakeUpOffsetMinutes,
        String recurrenceType,
        List<Integer> daysOfWeek,
        Integer dayOfMonth,
        LocalDate endDate,
        LocalTime endTime,
        String photoUrl
) {}
