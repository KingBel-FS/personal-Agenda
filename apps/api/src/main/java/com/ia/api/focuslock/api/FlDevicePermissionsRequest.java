package com.ia.api.focuslock.api;

public record FlDevicePermissionsRequest(
        boolean familyControlsGranted,
        boolean screenTimeGranted,
        boolean notificationsGranted
) {}
