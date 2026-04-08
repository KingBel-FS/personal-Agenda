package com.ia.api.export.api;

import java.util.List;

public record ExportHistoryResponse(
        List<ExportAuditItem> items
) {
}
