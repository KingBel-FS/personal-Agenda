package com.ia.api.stats.api;

public record StatsDeltaResponse(
        long totalCountDelta,
        long doneCountDelta,
        int completionRateDelta
) {}
