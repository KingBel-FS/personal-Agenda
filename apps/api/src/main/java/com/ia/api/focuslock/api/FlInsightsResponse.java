package com.ia.api.focuslock.api;

import java.util.List;

public record FlInsightsResponse(
        List<FlUsageItem> topApps,
        List<FlWeeklyDayItem> weeklyBreakdown
) {}
