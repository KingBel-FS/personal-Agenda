package com.ia.api.focuslock.api;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record FlRuleRequest(
        @NotBlank String name,
        @NotBlank String targetType,
        @NotBlank String targetIdentifier,
        @NotBlank String ruleType,
        Integer limitMinutes,
        String frictionType,
        List<FlScheduleItem> schedules,
        List<String> domains
) {}
