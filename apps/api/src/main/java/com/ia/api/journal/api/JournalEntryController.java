package com.ia.api.journal.api;

import com.ia.api.common.api.ApiResponse;
import com.ia.api.journal.service.JournalEntryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/journal")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    public JournalEntryController(JournalEntryService journalEntryService) {
        this.journalEntryService = journalEntryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<JournalEntryResponse>> getEntry(
            Authentication authentication,
            @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        return journalEntryService.getEntry(authentication.getName(), localDate)
                .map(r -> ResponseEntity.ok(ApiResponse.of(r)))
                .orElse(ResponseEntity.ok(ApiResponse.of(null)));
    }

    @PutMapping
    public ApiResponse<JournalEntryResponse> upsertEntry(
            Authentication authentication,
            @Valid @RequestBody JournalEntryRequest request
    ) {
        LocalDate date = LocalDate.parse(request.date(), DateTimeFormatter.ISO_LOCAL_DATE);
        return ApiResponse.of(journalEntryService.upsertEntry(authentication.getName(), date, request.content()));
    }

    @DeleteMapping
    public ApiResponse<Void> deleteEntry(
            Authentication authentication,
            @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        journalEntryService.deleteEntry(authentication.getName(), localDate);
        return ApiResponse.of(null);
    }
}
