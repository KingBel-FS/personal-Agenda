package com.ia.api.goal.api;

import java.util.List;

public record GoalListResponse(
        List<GoalResponse> goals,
        List<GoalResponse> inactiveGoals,
        List<GoalEligibleTaskItem> eligibleTasks,
        String accountCreatedAt
) {}
