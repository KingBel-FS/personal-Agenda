package com.ia.api.journal.api;

public record JournalEntryResponse(
        String date,
        String content,
        String createdAt,
        String updatedAt
) {}
