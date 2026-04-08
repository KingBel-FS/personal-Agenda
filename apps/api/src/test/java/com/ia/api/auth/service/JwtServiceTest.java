package com.ia.api.auth.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test
    void generatesAndValidatesAccessToken() {
        JwtService jwtService = new JwtService(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                15
        );

        String token = jwtService.generateAccessToken(UUID.randomUUID(), "alice@example.com");

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(jwtService.isTokenValid(token, "alice@example.com")).isTrue();
        assertThat(jwtService.getAccessTokenExpiresInSeconds()).isEqualTo(900);
    }
}
