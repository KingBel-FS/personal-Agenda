package com.ia.api.user.service;

import com.ia.api.auth.domain.AccountStatus;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.holiday.domain.HolidaySyncStateEntity;
import com.ia.api.holiday.domain.HolidaySyncStatus;
import com.ia.api.holiday.repository.HolidaySyncStateRepository;
import com.ia.api.holiday.service.HolidaySyncStateService;
import com.ia.api.user.api.DayProfileRequest;
import com.ia.api.user.api.ProfileResponse;
import com.ia.api.user.api.UpdateProfileRequest;
import com.ia.api.user.api.ZoneImpactResponse;
import com.ia.api.user.domain.DayCategory;
import com.ia.api.user.domain.DayProfileEntity;
import com.ia.api.user.repository.AssetRepository;
import com.ia.api.user.repository.DayProfileRepository;
import com.ia.api.vacation.repository.VacationPeriodRepository;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskRuleRepository;
import com.ia.api.task.service.OccurrenceRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private DayProfileRepository dayProfileRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private HolidaySyncStateRepository holidaySyncStateRepository;
    @Mock
    private VacationPeriodRepository vacationPeriodRepository;
    @Mock
    private ProfilePhotoStorageService profilePhotoStorageService;
    @Mock
    private SignedAssetUrlService signedAssetUrlService;
    @Mock
    private HolidaySyncStateService holidaySyncStateService;
    @Mock
    private TaskDefinitionRepository taskDefinitionRepository;
    @Mock
    private TaskRuleRepository taskRuleRepository;
    @Mock
    private OccurrenceRefreshService occurrenceRefreshService;
    @Captor
    private ArgumentCaptor<com.ia.api.user.domain.AssetEntity> assetCaptor;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                userRepository,
                dayProfileRepository,
                assetRepository,
                holidaySyncStateRepository,
                vacationPeriodRepository,
                profilePhotoStorageService,
                signedAssetUrlService,
                holidaySyncStateService,
                taskDefinitionRepository,
                taskRuleRepository,
                occurrenceRefreshService
        );
    }

    @Test
    void getProfileReturnsSignedPhotoAndSchedulingProjection() {
        UserEntity user = activeUser();
        user.setProfilePhotoUrl("user/photo.png");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(dayProfileRepository.findAllByUserId(user.getId())).thenReturn(defaultDayProfiles(user.getId()));
        when(signedAssetUrlService.signProfilePhotoUrl("user/photo.png")).thenReturn("signed-url");
        when(holidaySyncStateRepository.findByUserId(user.getId())).thenReturn(Optional.of(syncState(user.getId())));
        when(vacationPeriodRepository.findAllByUserIdOrderByStartDateAsc(user.getId())).thenReturn(List.of());

        ProfileResponse response = profileService.getProfile("alice@example.com");

        assertThat(response.profilePhotoSignedUrl()).isEqualTo("signed-url");
        assertThat(response.schedulingProfile().dayProfiles()).hasSize(3);
        assertThat(response.timezoneName()).isEqualTo("Europe/Paris");
    }

    @Test
    void updateProfileRequiresZoneConfirmationWhenZoneChanges() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.updateProfile("alice@example.com", updateRequest(false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le changement de zone doit être confirmé");
    }

    @Test
    void updateProfilePersistsWakeUpTimesAndNewPhoto() throws IOException {
        UserEntity user = activeUser();
        List<DayProfileEntity> dayProfiles = defaultDayProfiles(user.getId());

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(dayProfileRepository.findAllByUserId(user.getId())).thenReturn(dayProfiles);
        when(profilePhotoStorageService.store(eq(user.getId()), any()))
                .thenReturn(new ProfilePhotoStorageService.StoredPhoto("updated/photo.png"));
        when(signedAssetUrlService.signProfilePhotoUrl("updated/photo.png")).thenReturn("signed-url");
        when(holidaySyncStateRepository.findByUserId(user.getId())).thenReturn(Optional.of(syncState(user.getId())));
        when(vacationPeriodRepository.findAllByUserIdOrderByStartDateAsc(user.getId())).thenReturn(List.of());
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());

        ProfileResponse response = profileService.updateProfile("alice@example.com", updateRequest(true));

        verify(assetRepository).save(assetCaptor.capture());
        assertThat(assetCaptor.getValue().getStorageKey()).isEqualTo("updated/photo.png");
        assertThat(response.pseudo()).isEqualTo("alice-updated");
        assertThat(response.geographicZone()).isEqualTo("ALSACE_LORRAINE");
        assertThat(response.schedulingProfile().dayProfiles().get(0).wakeUpTime()).isEqualTo("07:15");
        assertThat(user.getTimezoneName()).isEqualTo("Europe/Paris");
        verify(holidaySyncStateService).markPending(user.getId(), "ALSACE_LORRAINE");
    }

    @Test
    void previewZoneImpactExplainsHolidayEffect() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        ZoneImpactResponse response = profileService.previewZoneImpact("alice@example.com", "ALSACE_LORRAINE");

        assertThat(response.holidayRulesWillChange()).isTrue();
        assertThat(response.impactMessage()).contains("jours feries");
    }

    @Test
    void readSignedPhotoRejectsExpiredSignature() {
        when(signedAssetUrlService.isValid("user/photo.png", 10L, "bad")).thenReturn(false);

        assertThatThrownBy(() -> profileService.readSignedPhoto("user/photo.png", 10L, "bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readSignedPhotoReturnsStoredResource() throws IOException {
        when(signedAssetUrlService.isValid("user/photo.png", 10L, "sig")).thenReturn(true);
        when(profilePhotoStorageService.load("user/photo.png"))
                .thenReturn(new ProfilePhotoStorageService.StoredPhotoResource(Path.of("C:/tmp/photo.png"), "image/png"));

        ProfileService.ProfilePhotoPayload payload = profileService.readSignedPhoto("user/photo.png", 10L, "sig");

        assertThat(payload.contentType()).isEqualTo("image/png");
    }

    private UpdateProfileRequest updateRequest(boolean confirmed) {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPseudo("alice-updated");
        request.setGeographicZone("ALSACE_LORRAINE");
        request.setTimezoneName("Europe/Paris");
        request.setZoneChangeConfirmed(confirmed);
        request.setDayProfiles(List.of(
                dayProfileRequest("WORKDAY", "07:15"),
                dayProfileRequest("VACATION", "09:15"),
                dayProfileRequest("WEEKEND_HOLIDAY", "08:45")
        ));
        request.setProfilePhoto(new MockMultipartFile("profilePhoto", "avatar.png", "image/png", "new".getBytes()));
        return request;
    }

    private DayProfileRequest dayProfileRequest(String category, String wakeUpTime) {
        DayProfileRequest request = new DayProfileRequest();
        request.setDayCategory(category);
        request.setWakeUpTime(wakeUpTime);
        return request;
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setPseudo("alice");
        user.setEmail("alice@example.com");
        user.setBirthDate(LocalDate.of(1995, 5, 12));
        user.setGeographicZone("METROPOLE");
        user.setTimezoneName("Europe/Paris");
        user.setAccountStatus(AccountStatus.ACTIVE);
        return user;
    }

    private List<DayProfileEntity> defaultDayProfiles(UUID userId) {
        return List.of(
                createDayProfile(userId, DayCategory.WORKDAY, LocalTime.of(7, 0)),
                createDayProfile(userId, DayCategory.VACATION, LocalTime.of(9, 0)),
                createDayProfile(userId, DayCategory.WEEKEND_HOLIDAY, LocalTime.of(8, 30))
        );
    }

    private DayProfileEntity createDayProfile(UUID userId, DayCategory category, LocalTime wakeUpTime) {
        DayProfileEntity entity = new DayProfileEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setDayCategory(category);
        entity.setWakeUpTime(wakeUpTime);
        return entity;
    }

    private HolidaySyncStateEntity syncState(UUID userId) {
        HolidaySyncStateEntity entity = new HolidaySyncStateEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setGeographicZone("METROPOLE");
        entity.setStatus(HolidaySyncStatus.SYNCED);
        entity.setLastSyncedYear(2026);
        return entity;
    }
}
