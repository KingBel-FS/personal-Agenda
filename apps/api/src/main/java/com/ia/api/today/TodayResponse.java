package com.ia.api.today;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TodayResponse(
        String date,
        boolean beforeAccountCreation,
        String dayCategory,
        int activeCount,
        int skippedCount,
        int doneCount,
        int missedCount,
        int totalCount,
        int progressPercent,
        List<TodayOccurrenceItem> occurrences,
        StreakInfo streak,
        List<String> newBadges
) {

    public record StreakInfo(
            int currentStreak,
            int longestStreak,
            boolean streakActive,
            List<String> badges
    ) {}
}
