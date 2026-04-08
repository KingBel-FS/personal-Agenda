package com.ia.api.task.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TaskPreviewRequest(
        @NotNull LocalDate startDate,
        @NotEmpty List<String> dayCategories,
        @NotBlank String timeMode,
        LocalTime fixedTime,
        Integer wakeUpOffsetMinutes,
        String recurrenceType,
        List<Integer> daysOfWeek,
        Integer dayOfMonth
) {}
