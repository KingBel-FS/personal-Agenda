package com.ia.api.stats.api;

public record StatsTaskRecentOccurrenceResponse(
        String occurrenceDate,
        String occurrenceTime,
        String status,
        String dayCategory
) {}
