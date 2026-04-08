package com.ia.api.stats.api;

public record StatsHistoryPointResponse(
        String label,
        String periodStart,
        String periodEnd,
        long totalCount,
        long doneCount,
        long missedCount,
        long skippedCount,
        long plannedCount,
        int completionRate
) {}
