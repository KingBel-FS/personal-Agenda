package com.ia.api.stats.api;

import java.util.List;

public record StatsPeriodResponse(
        String periodType,
        String label,
        StatsSnapshotResponse current,
        StatsSnapshotResponse previous,
        StatsDeltaResponse delta,
        String comparisonLabel,
        List<StatsTaskSummaryResponse> taskBreakdown,
        List<StatsHistoryPointResponse> history
) {}
