package com.ia.api.focuslock.api;

import jakarta.validation.constraints.NotBlank;

public record FlConfirmPairingRequest(@NotBlank String token, String deviceName) {}
