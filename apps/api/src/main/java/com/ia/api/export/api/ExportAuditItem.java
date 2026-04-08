package com.ia.api.export.api;

public record ExportAuditItem(
        String id,
        String exportFormat,
        String exportScope,
        String periodFrom,
        String periodTo,
        String status,
        String fileName,
        Integer rowCount,
        Long durationMs,
        String createdAt,
        String completedAt
) {
}
