package com.ia.api.goal.api;

import java.util.UUID;

public record GoalEligibleTaskItem(
        UUID taskDefinitionId,
        String title,
        String icon,
        String recurrenceType
) {}
