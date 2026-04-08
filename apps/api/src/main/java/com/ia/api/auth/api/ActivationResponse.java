package com.ia.api.auth.api;

import java.util.UUID;

public record ActivationResponse(UUID userId, String status) {
}
