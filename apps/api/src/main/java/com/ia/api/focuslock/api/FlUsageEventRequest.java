package com.ia.api.focuslock.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record FlUsageEventRequest(
        @NotBlank String deviceId,
        @NotBlank String targetIdentifier,
        @Positive int consumedMinutes,
        @NotBlank String eventDate
) {}
