package com.ia.api.goal.api;

import com.ia.api.auth.api.MessageResponse;
import com.ia.api.common.api.ApiResponse;
import com.ia.api.goal.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public ApiResponse<GoalListResponse> listGoals(Authentication authentication) {
        return ApiResponse.of(goalService.listGoals(authentication.getName()));
    }

    @PostMapping
    public ApiResponse<GoalResponse> createGoal(
            Authentication authentication,
            @Valid @RequestBody CreateGoalRequest request
    ) {
        return ApiResponse.of(goalService.createGoal(authentication.getName(), request));
    }

    @PutMapping("/{goalId}")
    public ApiResponse<GoalResponse> updateGoal(
            Authentication authentication,
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateGoalRequest request
    ) {
        return ApiResponse.of(goalService.updateGoal(authentication.getName(), goalId, request));
    }

    @DeleteMapping("/{goalId}")
    public ApiResponse<MessageResponse> deleteGoal(
            Authentication authentication,
            @PathVariable UUID goalId
    ) {
        return ApiResponse.of(goalService.deleteGoal(authentication.getName(), goalId));
    }
}
