package com.ia.api.goal.api;

import java.util.List;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String goalScope,
        String periodType,
        int targetCount,
        boolean active,
        UUID taskDefinitionId,
        String taskTitle,
        String taskIcon,
        String recurrenceType,
        GoalProgressSnapshot currentProgress,
        List<GoalProgressHistoryItem> recentHistory,
        String createdAt,
        String updatedAt
) {}
