package com.ia.api.focuslock.api;

import java.util.List;

public record FlRuleResponse(
        String id,
        String name,
        String targetType,
        String targetIdentifier,
        String ruleType,
        Integer limitMinutes,
        String frictionType,
        boolean active,
        List<FlScheduleItem> schedules,
        List<FlWebDomainItem> domains,
        long overrideCount,
        String createdAt,
        String updatedAt
) {}
