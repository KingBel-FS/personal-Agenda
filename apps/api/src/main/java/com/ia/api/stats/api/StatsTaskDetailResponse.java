package com.ia.api.stats.api;

import java.util.List;
import java.util.UUID;

public record StatsTaskDetailResponse(
        UUID taskDefinitionId,
        String title,
        String icon,
        String periodType,
        String label,
        StatsSnapshotResponse current,
        StatsSnapshotResponse previous,
        StatsDeltaResponse delta,
        List<StatsTaskRecentOccurrenceResponse> recentOccurrences
) {}
