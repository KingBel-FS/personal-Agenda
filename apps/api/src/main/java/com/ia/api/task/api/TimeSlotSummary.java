package com.ia.api.task.api;

import java.time.LocalTime;

/**
 * Read-only DTO exposing a task time slot in occurrence responses.
 * timeMode : FIXED | EVERY_N_MINUTES (legacy modes silently omitted)
 */
public record TimeSlotSummary(
        String timeMode,
        LocalTime fixedTime,
        Integer afterPreviousMinutes
) {
}
