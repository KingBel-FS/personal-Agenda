package com.ia.api.user.api;

import java.time.Instant;

public record HolidaySyncStatusResponse(
        String status,
        Integer lastSyncedYear,
        Instant lastSyncedAt,
        Instant nextRetryAt,
        String lastError,
        boolean alertVisible
) {
}
