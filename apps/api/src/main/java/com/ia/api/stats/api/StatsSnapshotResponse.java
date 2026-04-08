package com.ia.api.stats.api;

public record StatsSnapshotResponse(
        String periodStart,
        String periodEnd,
        long totalCount,
        long doneCount,
        long missedCount,
        long skippedCount,
        long plannedCount,
        long taskCount,
        int completionRate
) {}
