package com.ia.api.task.api;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskPreviewResponse(
        LocalDate occurrenceDate,
        LocalTime occurrenceTime,
        String occurrenceLabel
) {}
