package com.ia.api.goal.api;

public record GoalProgressHistoryItem(
        String periodStart,
        String periodEnd,
        int completedCount,
        int targetCount,
        int progressPercent,
        boolean goalMet
) {}
