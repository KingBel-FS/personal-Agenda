package com.ia.api.today;

import com.ia.api.common.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/today")
public class TodayController {

    private final TodayService todayService;
    private final OccurrenceStatusService statusService;

    public TodayController(TodayService todayService, OccurrenceStatusService statusService) {
        this.todayService = todayService;
        this.statusService = statusService;
    }

    @GetMapping
    public ApiResponse<TodayResponse> getToday(Authentication auth) {
        return ApiResponse.of(todayService.getToday(auth.getName()));
    }

    @GetMapping("/daily")
    public ApiResponse<TodayResponse> getDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication auth
    ) {
        return ApiResponse.of(todayService.getForDate(auth.getName(), date));
    }

    @PostMapping("/occurrences/{id}/complete")
    public ApiResponse<TodayResponse> complete(@PathVariable UUID id, Authentication auth) {
        return ApiResponse.of(statusService.changeStatus(auth.getName(), id, "done"));
    }

    @PostMapping("/occurrences/{id}/miss")
    public ApiResponse<TodayResponse> miss(@PathVariable UUID id, Authentication auth) {
        return ApiResponse.of(statusService.changeStatus(auth.getName(), id, "missed"));
    }

    @PostMapping("/occurrences/{id}/cancel")
    public ApiResponse<TodayResponse> cancel(@PathVariable UUID id, Authentication auth) {
        return ApiResponse.of(statusService.changeStatus(auth.getName(), id, "canceled"));
    }

    @PostMapping("/occurrences/{id}/edit-description")
    public ApiResponse<TodayResponse> editDescription(@PathVariable UUID id, @RequestBody Map<String, String> body, Authentication auth) {
        return ApiResponse.of(statusService.updateDescription(auth.getName(), id, body.get("description")));
    }

    @PostMapping("/occurrences/{id}/edit-time")
    public ApiResponse<TodayResponse> editTime(@PathVariable UUID id, @RequestBody Map<String, String> body, Authentication auth) {
        LocalTime newTime = LocalTime.parse(body.get("time"));
        return ApiResponse.of(statusService.updateTime(auth.getName(), id, newTime));
    }

    @PostMapping("/occurrences/{id}/revert")
    public ApiResponse<TodayResponse> revertToPlanned(@PathVariable UUID id, Authentication auth) {
        return ApiResponse.of(statusService.revertToPlanned(auth.getName(), id));
    }

    @PostMapping("/occurrences/{id}/reschedule")
    public ApiResponse<TodayResponse> reschedule(@PathVariable UUID id, @RequestBody Map<String, String> body, Authentication auth) {
        LocalDate newDate = LocalDate.parse(body.get("date"));
        LocalTime newTime = LocalTime.parse(body.get("time"));
        return ApiResponse.of(statusService.reschedule(auth.getName(), id, newDate, newTime));
    }
}
