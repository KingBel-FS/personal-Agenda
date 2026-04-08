package com.ia.api.user.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.holiday.domain.HolidaySyncStateEntity;
import com.ia.api.holiday.domain.HolidaySyncStatus;
import com.ia.api.holiday.repository.HolidaySyncStateRepository;
import com.ia.api.holiday.service.HolidaySyncStateService;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskRuleRepository;
import com.ia.api.task.service.OccurrenceRefreshService;
import com.ia.api.user.api.DayProfileRequest;
import com.ia.api.user.api.DayProfileResponse;
import com.ia.api.user.api.HolidaySyncStatusResponse;
import com.ia.api.user.api.ProfileResponse;
import com.ia.api.user.api.SchedulingProfileResponse;
import com.ia.api.user.api.UpdateProfileRequest;
import com.ia.api.user.api.VacationPeriodResponse;
import com.ia.api.user.api.ZoneImpactResponse;
import com.ia.api.user.domain.AssetEntity;
import com.ia.api.user.domain.DayCategory;
import com.ia.api.user.domain.DayProfileEntity;
import com.ia.api.user.repository.AssetRepository;
import com.ia.api.user.repository.DayProfileRepository;
import com.ia.api.vacation.repository.VacationPeriodRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final DayProfileRepository dayProfileRepository;
    private final AssetRepository assetRepository;
    private final HolidaySyncStateRepository holidaySyncStateRepository;
    private final VacationPeriodRepository vacationPeriodRepository;
    private final ProfilePhotoStorageService profilePhotoStorageService;
    private final SignedAssetUrlService signedAssetUrlService;
    private final HolidaySyncStateService holidaySyncStateService;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskRuleRepository taskRuleRepository;
    private final OccurrenceRefreshService occurrenceRefreshService;

    public ProfileService(
            UserRepository userRepository,
            DayProfileRepository dayProfileRepository,
            AssetRepository assetRepository,
            HolidaySyncStateRepository holidaySyncStateRepository,
            VacationPeriodRepository vacationPeriodRepository,
            ProfilePhotoStorageService profilePhotoStorageService,
            SignedAssetUrlService signedAssetUrlService,
            HolidaySyncStateService holidaySyncStateService,
            TaskDefinitionRepository taskDefinitionRepository,
            TaskRuleRepository taskRuleRepository,
            OccurrenceRefreshService occurrenceRefreshService
    ) {
        this.userRepository = userRepository;
        this.dayProfileRepository = dayProfileRepository;
        this.assetRepository = assetRepository;
        this.holidaySyncStateRepository = holidaySyncStateRepository;
        this.vacationPeriodRepository = vacationPeriodRepository;
        this.profilePhotoStorageService = profilePhotoStorageService;
        this.signedAssetUrlService = signedAssetUrlService;
        this.holidaySyncStateService = holidaySyncStateService;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskRuleRepository = taskRuleRepository;
        this.occurrenceRefreshService = occurrenceRefreshService;
    }

    @Transactional
    public ProfileResponse getProfile(String email) {
        UserEntity user = getUser(email);
        return toProfileResponse(user, ensureDayProfiles(user.getId()));
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        UserEntity user = getUser(email);
        if (!user.getGeographicZone().equals(request.getGeographicZone())
                && !Boolean.TRUE.equals(request.getZoneChangeConfirmed())) {
            throw new IllegalArgumentException("Le changement de zone doit être confirmé après examen de l'impact sur les jours fériés");
        }

        validateDayProfiles(request.getDayProfiles());
        List<DayProfileEntity> existingDayProfiles = ensureDayProfiles(user.getId());
        boolean wakeUpScheduleChanged = hasWakeUpScheduleChanged(existingDayProfiles, request.getDayProfiles());

        user.setPseudo(request.getPseudo().trim());
        boolean zoneChanged = !user.getGeographicZone().equals(request.getGeographicZone());
        user.setGeographicZone(request.getGeographicZone());
        user.setTimezoneName(validateTimezone(request.getTimezoneName()));
        if (zoneChanged) {
            holidaySyncStateService.markPending(user.getId(), user.getGeographicZone());
        }

        upsertDayProfiles(user.getId(), request.getDayProfiles());
        attachProfilePhotoIfPresent(user, request.getProfilePhoto());
        if (zoneChanged || wakeUpScheduleChanged) {
            refreshFutureScheduling(user.getId(), user.getGeographicZone(), zoneChanged);
        }

        return toProfileResponse(user, ensureDayProfiles(user.getId()));
    }

    @Transactional
    public ZoneImpactResponse previewZoneImpact(String email, String targetZone) {
        UserEntity user = getUser(email);
        boolean changed = !user.getGeographicZone().equals(targetZone);
        return new ZoneImpactResponse(
                user.getGeographicZone(),
                targetZone,
                changed,
                changed
                        ? "Les jours feries synchronises basculeront vers la zone " + labelForZone(targetZone) + "."
                        : "Aucun changement de jours feries attendu."
        );
    }

    public ProfilePhotoPayload readSignedPhoto(String storageKey, long expiresAt, String signature) {
        if (!signedAssetUrlService.isValid(storageKey, expiresAt, signature)) {
            throw new IllegalArgumentException("L'URL signée de la photo est invalide ou expirée");
        }
        try {
            ProfilePhotoStorageService.StoredPhotoResource resource = profilePhotoStorageService.load(storageKey);
            return new ProfilePhotoPayload(resource.path(), resource.contentType());
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de lire la photo de profil", exception);
        }
    }

    private UserEntity getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    private List<DayProfileEntity> ensureDayProfiles(UUID userId) {
        List<DayProfileEntity> existing = dayProfileRepository.findAllByUserId(userId);
        if (existing.size() == DayCategory.values().length) {
            return sortDayProfiles(existing);
        }

        Map<DayCategory, DayProfileEntity> byCategory = existing.stream()
                .collect(Collectors.toMap(DayProfileEntity::getDayCategory, Function.identity()));
        for (DayCategory category : DayCategory.values()) {
            if (!byCategory.containsKey(category)) {
                DayProfileEntity entity = new DayProfileEntity();
                entity.setId(UUID.randomUUID());
                entity.setUserId(userId);
                entity.setDayCategory(category);
                entity.setWakeUpTime(defaultWakeUpTime(category));
                dayProfileRepository.save(entity);
                byCategory.put(category, entity);
            }
        }
        return sortDayProfiles(byCategory.values().stream().toList());
    }

    private void upsertDayProfiles(UUID userId, List<DayProfileRequest> requests) {
        Map<DayCategory, DayProfileEntity> existing = ensureDayProfiles(userId).stream()
                .collect(Collectors.toMap(DayProfileEntity::getDayCategory, Function.identity()));

        for (DayProfileRequest request : requests) {
            DayCategory category = parseCategory(request.getDayCategory());
            DayProfileEntity entity = existing.get(category);
            entity.setWakeUpTime(LocalTime.parse(request.getWakeUpTime()));
        }
    }

    private boolean hasWakeUpScheduleChanged(List<DayProfileEntity> existingProfiles, List<DayProfileRequest> requests) {
        Map<DayCategory, LocalTime> existing = existingProfiles.stream()
                .collect(Collectors.toMap(DayProfileEntity::getDayCategory, DayProfileEntity::getWakeUpTime));
        for (DayProfileRequest request : requests) {
            DayCategory category = parseCategory(request.getDayCategory());
            LocalTime nextTime = LocalTime.parse(request.getWakeUpTime());
            if (!nextTime.equals(existing.get(category))) {
                return true;
            }
        }
        return false;
    }

    private void refreshFutureScheduling(UUID userId, String geographicZone, boolean includeFixedTimeRules) {
        List<UUID> definitionIds = taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(definition -> definition.getId())
                .toList();
        if (definitionIds.isEmpty()) {
            return;
        }

        List<TaskRuleEntity> rules = taskRuleRepository.findAllByTaskDefinitionIdIn(definitionIds).stream()
                .filter(rule -> includeFixedTimeRules || "WAKE_UP_OFFSET".equals(rule.getTimeMode()))
                .toList();
        for (TaskRuleEntity rule : rules) {
            occurrenceRefreshService.refreshFutureOccurrences(rule, userId, geographicZone, java.time.LocalDate.now(com.ia.api.task.service.TaskService.PARIS));
        }
    }

    private void attachProfilePhotoIfPresent(UserEntity user, MultipartFile profilePhoto) {
        if (profilePhoto == null || profilePhoto.isEmpty()) {
            return;
        }
        try {
            ProfilePhotoStorageService.StoredPhoto storedPhoto = profilePhotoStorageService.store(user.getId(), profilePhoto);
            AssetEntity asset = new AssetEntity();
            asset.setId(UUID.randomUUID());
            asset.setUserId(user.getId());
            asset.setAssetType("PROFILE_PHOTO");
            asset.setStorageKey(storedPhoto.storageKey());
            asset.setCreatedAt(Instant.now());
            assetRepository.save(asset);
            user.setProfilePhotoUrl(storedPhoto.storageKey());
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de sauvegarder la photo de profil", exception);
        }
    }

    private void validateDayProfiles(List<DayProfileRequest> dayProfiles) {
        EnumSet<DayCategory> categories = EnumSet.noneOf(DayCategory.class);
        for (DayProfileRequest request : dayProfiles) {
            categories.add(parseCategory(request.getDayCategory()));
        }
        if (categories.size() != DayCategory.values().length) {
            throw new IllegalArgumentException("Une heure de réveil est requise pour chaque catégorie de jour");
        }
    }

    private DayCategory parseCategory(String rawCategory) {
        try {
            return DayCategory.valueOf(rawCategory);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Catégorie de jour non supportée : " + rawCategory);
        }
    }

    private String validateTimezone(String timezoneName) {
        ZoneId.of(timezoneName);
        return timezoneName;
    }

    private LocalTime defaultWakeUpTime(DayCategory category) {
        return switch (category) {
            case WORKDAY -> LocalTime.of(7, 0);
            case VACATION -> LocalTime.of(9, 0);
            case WEEKEND_HOLIDAY -> LocalTime.of(8, 30);
        };
    }

    private List<DayProfileEntity> sortDayProfiles(List<DayProfileEntity> dayProfiles) {
        List<DayCategory> order = Arrays.asList(DayCategory.WORKDAY, DayCategory.VACATION, DayCategory.WEEKEND_HOLIDAY);
        return dayProfiles.stream()
                .sorted(Comparator.comparingInt(item -> order.indexOf(item.getDayCategory())))
                .toList();
    }

    private ProfileResponse toProfileResponse(UserEntity user, List<DayProfileEntity> dayProfiles) {
        List<DayProfileResponse> responses = dayProfiles.stream()
                .map(profile -> new DayProfileResponse(profile.getDayCategory().name(), profile.getWakeUpTime().toString()))
                .toList();
        HolidaySyncStatusResponse holidaySyncStatus = holidaySyncStateRepository.findByUserId(user.getId())
                .map(this::toHolidaySyncStatus)
                .orElse(new HolidaySyncStatusResponse(HolidaySyncStatus.PENDING.name(), null, null, null, null, false));
        List<VacationPeriodResponse> vacationPeriods = vacationPeriodRepository.findAllByUserIdOrderByStartDateAsc(user.getId()).stream()
                .map(vacation -> new VacationPeriodResponse(
                        vacation.getId(),
                        vacation.getLabel(),
                        vacation.getStartDate(),
                        vacation.getEndDate()
                ))
                .toList();
        SchedulingProfileResponse schedulingProfile = new SchedulingProfileResponse(
                user.getGeographicZone(),
                user.getTimezoneName(),
                responses
        );
        return new ProfileResponse(
                user.getId(),
                user.getPseudo(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGeographicZone(),
                user.getTimezoneName(),
                user.getProfilePhotoUrl() == null ? null : signedAssetUrlService.signProfilePhotoUrl(user.getProfilePhotoUrl()),
                responses,
                schedulingProfile,
                holidaySyncStatus,
                vacationPeriods
        );
    }

    private HolidaySyncStatusResponse toHolidaySyncStatus(HolidaySyncStateEntity state) {
        return new HolidaySyncStatusResponse(
                state.getStatus().name(),
                state.getLastSyncedYear(),
                state.getLastSyncedAt(),
                state.getNextRetryAt(),
                state.getLastError(),
                state.getStatus() == HolidaySyncStatus.RETRY_SCHEDULED
        );
    }

    private String labelForZone(String geographicZone) {
        return switch (geographicZone) {
            case "METROPOLE" -> "Metropole";
            case "ALSACE_LORRAINE" -> "Alsace-Lorraine";
            default -> geographicZone;
        };
    }

    public record ProfilePhotoPayload(Path path, String contentType) {
    }
}
