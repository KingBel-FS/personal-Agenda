package com.ia.api.journal.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JournalEntryRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String date,
        @NotBlank String content
) {}
