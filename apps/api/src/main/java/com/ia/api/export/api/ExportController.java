package com.ia.api.export.api;

import com.ia.api.common.api.ApiResponse;
import com.ia.api.export.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/history")
    public ApiResponse<ExportHistoryResponse> history(Authentication authentication) {
        return ApiResponse.of(exportService.listHistory(authentication.getName()));
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(
            Authentication authentication,
            @Valid @RequestBody ExportRequest request
    ) {
        ExportService.ExportPayload payload = exportService.generateExport(authentication.getName(), request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.fileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(org.springframework.http.MediaType.parseMediaType(payload.contentType()))
                .body(payload.content());
    }
}
