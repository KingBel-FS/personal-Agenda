package com.ia.api.focuslock.api;

import com.ia.api.common.api.ApiResponse;
import com.ia.api.focuslock.service.FocuslockDashboardService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/focuslock")
public class FocuslockDashboardController {

    private final FocuslockDashboardService dashboardService;

    public FocuslockDashboardController(FocuslockDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<FlDashboardResponse> getDashboard(Authentication authentication) {
        return ApiResponse.of(dashboardService.getDashboard(authentication.getName()));
    }

    @GetMapping("/insights")
    public ApiResponse<FlInsightsResponse> getInsights(Authentication authentication) {
        return ApiResponse.of(dashboardService.getInsights(authentication.getName()));
    }

    @PostMapping("/usage")
    public ApiResponse<Void> recordUsage(
            Authentication authentication,
            @Valid @RequestBody FlUsageEventRequest request
    ) {
        dashboardService.recordUsage(authentication.getName(), request);
        return ApiResponse.of(null);
    }
}
