package com.ia.api.user.api;

import java.time.LocalDate;
import java.util.UUID;

public record VacationPeriodResponse(
        UUID id,
        String label,
        LocalDate startDate,
        LocalDate endDate
) {
}
