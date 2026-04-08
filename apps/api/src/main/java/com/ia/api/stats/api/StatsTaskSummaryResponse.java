package com.ia.api.stats.api;

import java.util.UUID;

public record StatsTaskSummaryResponse(
        UUID taskDefinitionId,
        String title,
        String icon,
        long totalCount,
        long doneCount,
        long missedCount,
        long skippedCount,
        long plannedCount,
        int completionRate,
        long doneCountDelta,
        int completionRateDelta
) {}
