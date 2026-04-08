package com.ia.api.agenda;

import java.time.LocalDate;
import java.util.List;

public record AgendaDaySummary(
        LocalDate date,
        boolean currentMonth,
        boolean past,
        boolean today,
        boolean beforeAccountCreation,
        String dayCategory,
        int totalCount,
        int plannedCount,
        int doneCount,
        int missedCount,
        int skippedCount,
        String statusTone,
        List<String> icons
) {
}
