package com.ia.api.export.api;

import com.ia.api.common.api.GlobalExceptionHandler;
import com.ia.api.export.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExportControllerTest {

    private ExportService exportService;
    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        exportService = Mockito.mock(ExportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ExportController(exportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authentication = new TestingAuthenticationToken("alice@example.com", null);
    }

    @Test
    void listsExportHistory() throws Exception {
        Mockito.when(exportService.listHistory("alice@example.com"))
                .thenReturn(new ExportHistoryResponse(List.of(
                        new ExportAuditItem("1", "EXCEL", "FULL",
                                "2026-03-20", "2026-03-27",
                                "SUCCESS", "export.xlsx",
                                12, 45L,
                                "2026-03-27T10:00:00Z", "2026-03-27T10:00:01Z")
                )));

        mockMvc.perform(get("/api/v1/exports/history").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].exportFormat").value("EXCEL"))
                .andExpect(jsonPath("$.data.items[0].status").value("SUCCESS"));
    }

    @Test
    void downloadsExcelExport() throws Exception {
        byte[] fakeXlsx = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK magic bytes
        Mockito.when(exportService.generateExport(
                eq("alice@example.com"),
                eq(new ExportRequest("EXCEL", "FULL", "2026-03-20", "2026-03-27"))))
                .thenReturn(new ExportService.ExportPayload(
                        "export-full-2026-03-20-2026-03-27.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        fakeXlsx
                ));

        mockMvc.perform(post("/api/v1/exports/download")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "format": "EXCEL",
                                  "scope": "FULL",
                                  "fromDate": "2026-03-20",
                                  "toDate": "2026-03-27"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"export-full-2026-03-20-2026-03-27.xlsx\""));
    }

    @Test
    void rejectsCsvFormatAsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/exports/download")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "format": "CSV",
                                  "scope": "FULL"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadsPdfExport() throws Exception {
        byte[] fakePdf = "%PDF-1.4".getBytes();
        Mockito.when(exportService.generateExport(
                eq("alice@example.com"),
                eq(new ExportRequest("PDF", "HISTORY", "2026-03-01", "2026-03-27"))))
                .thenReturn(new ExportService.ExportPayload(
                        "export-history-2026-03-01-2026-03-27.pdf",
                        "application/pdf",
                        fakePdf
                ));

        mockMvc.perform(post("/api/v1/exports/download")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "format": "PDF",
                                  "scope": "HISTORY",
                                  "fromDate": "2026-03-01",
                                  "toDate": "2026-03-27"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"export-history-2026-03-01-2026-03-27.pdf\""));
    }
}
