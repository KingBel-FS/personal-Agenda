package com.ia.api.export.api;

import jakarta.validation.constraints.Pattern;

public record ExportRequest(
        @Pattern(regexp = "EXCEL|PDF", message = "Le format d'export doit être EXCEL ou PDF.")
        String format,
        @Pattern(regexp = "TASKS|HISTORY|FULL", message = "Le scope d'export est invalide.")
        String scope,
        String fromDate,
        String toDate
) {
}
