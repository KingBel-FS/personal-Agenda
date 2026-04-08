package com.ia.api.focuslock.api;

import com.ia.api.common.api.ApiResponse;
import com.ia.api.focuslock.service.FocuslockRuleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/focuslock/rules")
public class FocuslockRuleController {

    private final FocuslockRuleService ruleService;

    public FocuslockRuleController(FocuslockRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public ApiResponse<List<FlRuleResponse>> listRules(Authentication authentication) {
        return ApiResponse.of(ruleService.listRules(authentication.getName()));
    }

    @PostMapping
    public ApiResponse<FlRuleResponse> createRule(
            Authentication authentication,
            @Valid @RequestBody FlRuleRequest request
    ) {
        return ApiResponse.of(ruleService.createRule(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<FlRuleResponse> updateRule(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody FlRuleRequest request
    ) {
        return ApiResponse.of(ruleService.updateRule(authentication.getName(), id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<FlRuleResponse> toggleRule(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return ApiResponse.of(ruleService.toggleRule(authentication.getName(), id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(Authentication authentication, @PathVariable UUID id) {
        ruleService.deleteRule(authentication.getName(), id);
        return ApiResponse.of(null);
    }
}
