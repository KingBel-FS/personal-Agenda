package com.ia.api.focuslock.api;

import com.ia.api.common.api.ApiResponse;
import com.ia.api.focuslock.service.FocuslockDeviceService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/focuslock/devices")
public class FocuslockDeviceController {

    private final FocuslockDeviceService deviceService;

    public FocuslockDeviceController(FocuslockDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ApiResponse<List<FlDeviceResponse>> listDevices(Authentication authentication) {
        return ApiResponse.of(deviceService.listDevices(authentication.getName()));
    }

    @PostMapping("/pair-token")
    public ApiResponse<FlPairingTokenResponse> generatePairingToken(Authentication authentication) {
        return ApiResponse.of(deviceService.generatePairingToken(authentication.getName()));
    }

    @PostMapping("/confirm")
    public ApiResponse<FlDeviceResponse> confirmPairing(
            Authentication authentication,
            @Valid @RequestBody FlConfirmPairingRequest request
    ) {
        return ApiResponse.of(deviceService.confirmPairing(request.token(), request.deviceName()));
    }

    @PatchMapping("/{id}/permissions")
    public ApiResponse<FlDeviceResponse> updatePermissions(
            @PathVariable UUID id,
            @RequestBody FlDevicePermissionsRequest request
    ) {
        return ApiResponse.of(deviceService.updatePermissions(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> revokeDevice(Authentication authentication, @PathVariable UUID id) {
        deviceService.revokeDevice(authentication.getName(), id);
        return ApiResponse.of(null);
    }
}
