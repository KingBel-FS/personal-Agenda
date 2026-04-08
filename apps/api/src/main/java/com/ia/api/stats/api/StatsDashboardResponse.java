package com.ia.api.stats.api;

public record StatsDashboardResponse(
        String generatedAt,
        String accountCreatedAt,
        StatsPeriodResponse daily,
        StatsPeriodResponse weekly,
        StatsPeriodResponse monthly,
        StatsPeriodResponse yearly
) {}
