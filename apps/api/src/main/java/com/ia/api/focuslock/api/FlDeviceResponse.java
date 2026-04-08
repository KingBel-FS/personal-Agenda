package com.ia.api.focuslock.api;

public record FlDeviceResponse(
        String id,
        String deviceName,
        String status,
        boolean familyControlsGranted,
        boolean screenTimeGranted,
        boolean notificationsGranted,
        String pairedAt,
        String lastSeenAt,
        String createdAt
) {}
