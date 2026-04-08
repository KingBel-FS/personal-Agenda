package com.ia.api.user.api;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank String confirmPassword
) {
}
