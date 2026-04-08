package com.ia.api.user.api;

import java.util.List;

public record SchedulingProfileResponse(
        String geographicZone,
        String timezoneName,
        List<DayProfileResponse> dayProfiles
) {
}
