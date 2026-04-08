package com.ia.api.user.api;

import com.ia.api.common.api.GlobalExceptionHandler;
import com.ia.api.user.service.AccountDeletionService;
import com.ia.api.user.service.ProfileService;
import com.ia.api.vacation.service.VacationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileControllerTest {
    private ProfileService profileService;
    private VacationService vacationService;
    private AccountDeletionService accountDeletionService;
    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        profileService = Mockito.mock(ProfileService.class);
        vacationService = Mockito.mock(VacationService.class);
        accountDeletionService = Mockito.mock(AccountDeletionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProfileController(profileService, vacationService, accountDeletionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authentication = new TestingAuthenticationToken("alice@example.com", null);
    }

    @Test
    void getProfileReturnsSchedulingProjection() throws Exception {
        Mockito.when(profileService.getProfile("alice@example.com")).thenReturn(profileResponse());

        mockMvc.perform(get("/api/v1/profile").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.schedulingProfile.dayProfiles[0].dayCategory").value("WORKDAY"));
    }

    @Test
    void updateProfileAcceptsMultipartPayload() throws Exception {
        Mockito.when(profileService.updateProfile(eq("alice@example.com"), any(UpdateProfileRequest.class)))
                .thenReturn(profileResponse());

        mockMvc.perform(multipart("/api/v1/profile")
                        .file(new MockMultipartFile("profilePhoto", "avatar.png", "image/png", "content".getBytes()))
                        .param("pseudo", "alice")
                        .param("geographicZone", "ALSACE_LORRAINE")
                        .param("timezoneName", "Europe/Paris")
                        .param("zoneChangeConfirmed", "true")
                        .param("dayProfiles[0].dayCategory", "WORKDAY")
                        .param("dayProfiles[0].wakeUpTime", "07:15")
                        .param("dayProfiles[1].dayCategory", "VACATION")
                        .param("dayProfiles[1].wakeUpTime", "09:10")
                        .param("dayProfiles[2].dayCategory", "WEEKEND_HOLIDAY")
                        .param("dayProfiles[2].wakeUpTime", "08:45")
                        .principal(authentication)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.geographicZone").value("METROPOLE"));
    }

    @Test
    void previewZoneImpactReturnsImpactMessage() throws Exception {
        Mockito.when(profileService.previewZoneImpact("alice@example.com", "ALSACE_LORRAINE"))
                .thenReturn(new ZoneImpactResponse(
                        "METROPOLE",
                        "ALSACE_LORRAINE",
                        true,
                        "Les jours feries synchronises basculeront vers la zone Alsace-Lorraine."
                ));

        mockMvc.perform(post("/api/v1/profile/zone-impact")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"geographicZone":"ALSACE_LORRAINE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.holidayRulesWillChange").value(true));
    }

    @Test
    void readSignedPhotoReturnsImage() throws Exception {
        Path photo = Files.createTempFile("profile-photo-", ".png");
        Mockito.when(profileService.readSignedPhoto("u/photo.png", 123L, "sig"))
                .thenReturn(new ProfileService.ProfilePhotoPayload(photo, "image/png"));

        mockMvc.perform(get("/api/v1/profile/photo")
                        .param("key", "u/photo.png")
                        .param("expiresAt", "123")
                        .param("signature", "sig"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void deleteAccountDelegatesToServiceAndReturnsOk() throws Exception {
        Mockito.doNothing().when(accountDeletionService).deleteAccount(eq("alice@example.com"), eq("secret123"));

        mockMvc.perform(delete("/api/v1/profile")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirmPassword":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Account deleted."));

        Mockito.verify(accountDeletionService).deleteAccount("alice@example.com", "secret123");
    }

    @Test
    void deleteAccountReturns401WhenPasswordIsWrong() throws Exception {
        Mockito.doThrow(new BadCredentialsException("Mot de passe invalide"))
                .when(accountDeletionService).deleteAccount(eq("alice@example.com"), any());

        mockMvc.perform(delete("/api/v1/profile")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirmPassword":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private ProfileResponse profileResponse() {
        List<DayProfileResponse> dayProfiles = List.of(
                new DayProfileResponse("WORKDAY", "07:00"),
                new DayProfileResponse("VACATION", "09:00"),
                new DayProfileResponse("WEEKEND_HOLIDAY", "08:30")
        );
        return new ProfileResponse(
                UUID.randomUUID(),
                "alice",
                "alice@example.com",
                LocalDate.of(1995, 5, 12),
                "METROPOLE",
                "Europe/Paris",
                "http://localhost:8080/api/v1/profile/photo?key=test",
                dayProfiles,
                new SchedulingProfileResponse("METROPOLE", "Europe/Paris", dayProfiles),
                new HolidaySyncStatusResponse("SYNCED", 2026, null, null, null, false),
                List.of()
        );
    }
}
