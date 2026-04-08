package com.ia.api.user.api;

import com.ia.api.auth.api.MessageResponse;
import com.ia.api.common.api.ApiResponse;
import com.ia.api.user.service.AccountDeletionService;
import com.ia.api.user.service.ProfileService;
import com.ia.api.vacation.service.VacationService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final VacationService vacationService;
    private final AccountDeletionService accountDeletionService;

    public ProfileController(ProfileService profileService, VacationService vacationService, AccountDeletionService accountDeletionService) {
        this.profileService = profileService;
        this.vacationService = vacationService;
        this.accountDeletionService = accountDeletionService;
    }

    @GetMapping
    public ApiResponse<ProfileResponse> getProfile(Authentication authentication) {
        return ApiResponse.of(profileService.getProfile(authentication.getName()));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @ModelAttribute UpdateProfileRequest request
    ) {
        return ApiResponse.of(profileService.updateProfile(authentication.getName(), request));
    }

    @PostMapping(path = "/zone-impact", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ZoneImpactResponse> previewZoneImpact(
            Authentication authentication,
            @Valid @RequestBody ZoneImpactRequest request
    ) {
        return ApiResponse.of(profileService.previewZoneImpact(authentication.getName(), request.geographicZone()));
    }

    @PostMapping(path = "/vacations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<VacationPeriodResponse> createVacation(
            Authentication authentication,
            @Valid @RequestBody VacationPeriodRequest request
    ) {
        return ApiResponse.of(vacationService.create(authentication.getName(), request));
    }

    @PutMapping(path = "/vacations/{vacationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<VacationPeriodResponse> updateVacation(
            Authentication authentication,
            @PathVariable UUID vacationId,
            @Valid @RequestBody VacationPeriodRequest request
    ) {
        return ApiResponse.of(vacationService.update(authentication.getName(), vacationId, request));
    }

    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<MessageResponse> deleteAccount(
            Authentication authentication,
            @Valid @RequestBody DeleteAccountRequest request
    ) {
        accountDeletionService.deleteAccount(authentication.getName(), request.confirmPassword());
        return ApiResponse.of(new MessageResponse("Account deleted."));
    }

    @DeleteMapping("/vacations/{vacationId}")
    public ApiResponse<MessageResponse> deleteVacation(Authentication authentication, @PathVariable UUID vacationId) {
        vacationService.delete(authentication.getName(), vacationId);
        return ApiResponse.of(new MessageResponse("Vacation period deleted."));
    }

    @GetMapping("/vacations")
    public ApiResponse<List<VacationPeriodResponse>> listVacations(Authentication authentication) {
        return ApiResponse.of(vacationService.list(authentication.getName()));
    }

    @GetMapping("/photo")
    public ResponseEntity<FileSystemResource> readSignedPhoto(
            @RequestParam String key,
            @RequestParam long expiresAt,
            @RequestParam String signature
    ) {
        ProfileService.ProfilePhotoPayload payload = profileService.readSignedPhoto(key, expiresAt, signature);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(new FileSystemResource(payload.path()));
    }
}
