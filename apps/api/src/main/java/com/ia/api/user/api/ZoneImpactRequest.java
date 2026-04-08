package com.ia.api.user.api;

import jakarta.validation.constraints.NotBlank;

public record ZoneImpactRequest(@NotBlank String geographicZone) {
}
