package com.ia.api.today;

import java.util.UUID;

public record TodayOccurrenceItem(
        UUID id,
        UUID taskDefinitionId,
        String title,
        String description,
        String icon,
        String occurrenceTime,
        String status,
        String dayCategory,
        boolean recurring,
        Integer slotOrder,
        int totalSlotsPerDay
) {}
