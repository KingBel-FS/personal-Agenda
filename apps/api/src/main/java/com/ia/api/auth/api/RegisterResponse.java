package com.ia.api.auth.api;

import java.util.UUID;

public record RegisterResponse(UUID userId, String status, String email) {
}
