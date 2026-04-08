package com.ia.api.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record VacationPeriodRequest(
        @NotBlank @Size(max = 100) String label,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
