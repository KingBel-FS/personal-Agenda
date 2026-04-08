package com.ia.api.auth.api;

import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String accessToken,
        long expiresInSeconds
) {
}
