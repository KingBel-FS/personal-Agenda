package com.ia.api.sync.api;

public record SyncEventResponse(
        String scope,
        String occurredAt
) {
}
