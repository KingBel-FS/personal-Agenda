package com.ia.api.wakeup.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WakeUpOverrideRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String date,
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}") String wakeUpTime
) {}
