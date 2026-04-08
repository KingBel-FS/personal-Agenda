package com.ia.api.focuslock.api;

import java.util.List;

public record FlDashboardResponse(
        FlDeviceResponse activeDevice,
        int activeRuleCount,
        int totalRuleCount,
        int totalMinutesToday,
        List<FlUsageItem> todayUsage,
        List<FlRuleResponse> activeRules
) {}
