package com.ia.api.auth.api;

public record RefreshResponse(
        String accessToken,
        long expiresInSeconds
) {
}
