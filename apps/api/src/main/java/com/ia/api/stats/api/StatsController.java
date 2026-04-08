package com.ia.api.stats.api;

import com.ia.api.common.api.ApiResponse;
import com.ia.api.stats.service.StatsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<StatsDashboardResponse> getDashboard(
            Authentication authentication,
            @RequestParam(required = false) LocalDate dailyAnchor,
            @RequestParam(required = false) LocalDate weeklyAnchor,
            @RequestParam(required = false) LocalDate monthlyAnchor,
            @RequestParam(required = false) LocalDate yearlyAnchor
    ) {
        return ApiResponse.of(statsService.getDashboard(authentication.getName(), dailyAnchor, weeklyAnchor, monthlyAnchor, yearlyAnchor));
    }

    @GetMapping("/tasks/{taskDefinitionId}")
    public ApiResponse<StatsTaskDetailResponse> getTaskDetail(
            Authentication authentication,
            @PathVariable UUID taskDefinitionId,
            @RequestParam(defaultValue = "MONTHLY") String periodType,
            @RequestParam(required = false) LocalDate anchorDate
    ) {
        return ApiResponse.of(statsService.getTaskDetail(authentication.getName(), taskDefinitionId, periodType, anchorDate));
    }
}
