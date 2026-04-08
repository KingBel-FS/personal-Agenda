package com.ia.api.goal.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UpdateGoalRequest(
        @NotBlank String goalScope,
        @NotBlank String periodType,
        @Min(1) @Max(1000) int targetCount,
        UUID taskDefinitionId,
        boolean active
) {}
