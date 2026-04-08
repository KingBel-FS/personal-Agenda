package com.ia.api.task.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record UpdateTaskOccurrenceRequest(
        @NotNull TaskMutationScope scope,
        UUID taskTimeSlotId,
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 50) String icon,
        @Size(max = 5000) String description,
        // P-14 : timeMode validé en amont via validateTimeMode() dans le service.
        // On garde @NotBlank pour rejeter les valeurs vides au niveau HTTP boundary.
        @NotBlank String timeMode,
        LocalTime fixedTime,
        // offset réveil toujours positif (après le réveil) — max 12 h
        @Min(0) @Max(720) Integer wakeUpOffsetMinutes,
        // P-4 : nullable car non requis pour THIS_OCCURRENCE — guard explicite dans le service
        List<String> dayCategories,
        String recurrenceType,
        List<Integer> daysOfWeek,
        Integer dayOfMonth,
        LocalDate endDate,
        LocalTime endTime,
        // Nullable : provided only for THIS_AND_FOLLOWING scope to replace rule slots.
        // Null means "keep existing slots as-is"; empty list means "remove all slots".
        List<TimeSlotRequest> timeSlots
) {
}
