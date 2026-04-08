package com.ia.api.wakeup.api;

import com.ia.api.common.api.ApiResponse;
import com.ia.api.wakeup.service.WakeUpOverrideService;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/wake-up-override")
public class WakeUpOverrideController {

    private final WakeUpOverrideService wakeUpOverrideService;

    public WakeUpOverrideController(WakeUpOverrideService wakeUpOverrideService) {
        this.wakeUpOverrideService = wakeUpOverrideService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WakeUpOverrideResponse>> getOverride(
            Authentication authentication,
            @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        return wakeUpOverrideService.getOverride(authentication.getName(), localDate)
                .map(r -> ResponseEntity.ok(ApiResponse.of(r)))
                .orElse(ResponseEntity.ok(ApiResponse.of(null)));
    }

    @PutMapping
    public ApiResponse<WakeUpOverrideResponse> upsertOverride(
            Authentication authentication,
            @Valid @RequestBody WakeUpOverrideRequest request
    ) {
        LocalDate date = LocalDate.parse(request.date(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalTime wakeUpTime = LocalTime.parse(request.wakeUpTime(), DateTimeFormatter.ofPattern("HH:mm"));
        return ApiResponse.of(wakeUpOverrideService.upsertOverride(authentication.getName(), date, wakeUpTime));
    }

    @DeleteMapping
    public ApiResponse<Void> deleteOverride(
            Authentication authentication,
            @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        wakeUpOverrideService.deleteOverride(authentication.getName(), localDate);
        return ApiResponse.of(null);
    }
}
