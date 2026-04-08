package com.ia.api.goal.api;

public record GoalProgressSnapshot(
        String periodStart,
        String periodEnd,
        int completedCount,
        int targetCount,
        int remainingCount,
        int progressPercent,
        boolean goalMet,
        String status
) {}
