package com.ia.api.user.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String pseudo,
        String email,
        LocalDate birthDate,
        String geographicZone,
        String timezoneName,
        String profilePhotoSignedUrl,
        List<DayProfileResponse> dayProfiles,
        SchedulingProfileResponse schedulingProfile,
        HolidaySyncStatusResponse holidaySyncStatus,
        List<VacationPeriodResponse> vacationPeriods
) {
}
