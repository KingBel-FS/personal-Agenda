package com.ia.api.user.api;

public record ZoneImpactResponse(
        String currentZone,
        String targetZone,
        boolean holidayRulesWillChange,
        String impactMessage
) {
}
