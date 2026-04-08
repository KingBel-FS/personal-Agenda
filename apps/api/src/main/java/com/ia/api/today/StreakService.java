package com.ia.api.today;

import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StreakService {

    private static final Set<String> STREAK_OK = Set.of("done", "suspended");
    private static final Set<String> STREAK_FAIL = Set.of("missed", "planned");
    private static final Set<String> TRANSPARENT = Set.of("skipped", "canceled");
    private static final int SUSPENSION_WINDOW_DAYS = 14;
    private static final int SUSPENSION_THRESHOLD = 3;

    /** Badge milestones: streak length → badge type */
    static final Map<Integer, String> BADGE_MILESTONES;
    static {
        BADGE_MILESTONES = new LinkedHashMap<>();
        BADGE_MILESTONES.put(3, "STREAK_3");
        BADGE_MILESTONES.put(7, "STREAK_7");
        BADGE_MILESTONES.put(14, "STREAK_14");
        BADGE_MILESTONES.put(30, "STREAK_30");
        BADGE_MILESTONES.put(60, "STREAK_60");
        BADGE_MILESTONES.put(100, "STREAK_100");
        BADGE_MILESTONES.put(365, "STREAK_365");
    }

    private final TaskOccurrenceRepository occurrenceRepository;
    private final StreakSnapshotRepository snapshotRepository;
    private final BadgeRepository badgeRepository;

    public StreakService(
            TaskOccurrenceRepository occurrenceRepository,
            StreakSnapshotRepository snapshotRepository,
            BadgeRepository badgeRepository
    ) {
        this.occurrenceRepository = occurrenceRepository;
        this.snapshotRepository = snapshotRepository;
        this.badgeRepository = badgeRepository;
    }

    /**
     * Recalculate streak for a user, persist snapshot, unlock badges if needed.
     * Returns the computed result for immediate use.
     */
    @Transactional
    public StreakResult recalculate(UUID userId) {
        LocalDate today = LocalDate.now(TaskService.PARIS);

        // Load all non-canceled occurrences up to AND including today
        List<TaskOccurrenceEntity> allOccurrences = occurrenceRepository
                .findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                        userId, "canceled", LocalDate.of(2020, 1, 1), today);

        // Group by date
        Map<LocalDate, List<TaskOccurrenceEntity>> byDate = allOccurrences.stream()
                .collect(Collectors.groupingBy(
                        TaskOccurrenceEntity::getOccurrenceDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        // Walk backwards from today to compute current streak
        // Today counts only if ALL applicable tasks are done/suspended (no planned left)
        int currentStreak = 0;
        boolean streakActive = true;

        for (LocalDate date = today; ; date = date.minusDays(1)) {
            List<TaskOccurrenceEntity> dayOccs = byDate.get(date);

            if (dayOccs == null || dayOccs.isEmpty()) {
                break;
            }

            // Filter out transparent statuses
            List<TaskOccurrenceEntity> applicable = dayOccs.stream()
                    .filter(o -> !TRANSPARENT.contains(o.getStatus()))
                    .toList();

            if (applicable.isEmpty()) {
                // All skipped/canceled — transparent day, continue looking back
                continue;
            }

            // Check if all applicable occurrences are streak-OK (done or suspended)
            // For today: "planned" means unfinished → day not complete yet → don't count
            boolean allOk = applicable.stream()
                    .allMatch(o -> STREAK_OK.contains(o.getStatus()));

            if (allOk) {
                currentStreak++;
            } else {
                // For today: if tasks are still planned, streak isn't broken — just don't count today
                if (date.equals(today)) {
                    // Today has unfinished tasks — don't count today but don't break streak either
                    continue;
                }
                streakActive = currentStreak > 0;
                break;
            }
        }

        if (currentStreak == 0) {
            streakActive = false;
        }

        // Compute longest streak from full history
        int longestStreak = computeLongestStreak(byDate);
        longestStreak = Math.max(longestStreak, currentStreak);

        // Also check stored longest (in case history was pruned)
        var existingSnapshot = snapshotRepository.findTopByUserIdOrderBySnapshotDateDesc(userId);
        if (existingSnapshot.isPresent()) {
            longestStreak = Math.max(longestStreak, existingSnapshot.get().getLongestStreak());
        }

        // Persist snapshot
        var snapshot = snapshotRepository.findByUserIdAndSnapshotDate(userId, today)
                .orElseGet(() -> {
                    var s = new StreakSnapshotEntity();
                    s.setUserId(userId);
                    s.setSnapshotDate(today);
                    return s;
                });
        snapshot.setCurrentStreak(currentStreak);
        snapshot.setLongestStreak(longestStreak);
        snapshot.setStreakActive(streakActive);
        snapshot.setUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        // Check and unlock badges
        List<String> newBadges = new ArrayList<>();
        for (var entry : BADGE_MILESTONES.entrySet()) {
            if (currentStreak >= entry.getKey()) {
                if (!badgeRepository.existsByUserIdAndBadgeType(userId, entry.getValue())) {
                    var badge = new BadgeEntity();
                    badge.setUserId(userId);
                    badge.setBadgeType(entry.getValue());
                    badgeRepository.save(badge);
                    newBadges.add(entry.getValue());
                }
            }
        }

        // Load all badges for the user
        List<String> allBadges = badgeRepository.findAllByUserId(userId).stream()
                .map(BadgeEntity::getBadgeType)
                .toList();

        return new StreakResult(currentStreak, longestStreak, streakActive, allBadges, newBadges);
    }

    /**
     * Get current streak info without recalculating (read-only, uses latest snapshot).
     */
    public StreakResult getLatest(UUID userId) {
        var snapshot = snapshotRepository.findTopByUserIdOrderBySnapshotDateDesc(userId);
        List<String> allBadges = badgeRepository.findAllByUserId(userId).stream()
                .map(BadgeEntity::getBadgeType)
                .toList();

        if (snapshot.isPresent()) {
            var s = snapshot.get();
            return new StreakResult(s.getCurrentStreak(), s.getLongestStreak(), s.isStreakActive(), allBadges, List.of());
        }
        return new StreakResult(0, 0, false, allBadges, List.of());
    }

    /**
     * Detect tasks with excessive suspensions in the rolling window.
     * Returns map of taskDefinitionId → suspension count (only for flagged tasks).
     */
    public Map<UUID, Integer> detectSuspensionAbuse(UUID userId) {
        LocalDate today = LocalDate.now(TaskService.PARIS);
        LocalDate windowStart = today.minusDays(SUSPENSION_WINDOW_DAYS);

        List<TaskOccurrenceEntity> suspendedOccs = occurrenceRepository
                .findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                        userId, "canceled", windowStart, today)
                .stream()
                .filter(o -> "suspended".equals(o.getStatus()))
                .toList();

        // Count suspensions per task definition
        Map<UUID, Integer> counts = suspendedOccs.stream()
                .collect(Collectors.groupingBy(
                        TaskOccurrenceEntity::getTaskDefinitionId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Only return those exceeding threshold
        Map<UUID, Integer> warnings = new HashMap<>();
        for (var entry : counts.entrySet()) {
            if (entry.getValue() >= SUSPENSION_THRESHOLD) {
                warnings.put(entry.getKey(), entry.getValue());
            }
        }
        return warnings;
    }

    private int computeLongestStreak(Map<LocalDate, List<TaskOccurrenceEntity>> byDate) {
        if (byDate.isEmpty()) return 0;

        int longest = 0;
        int current = 0;

        // Walk forward through all dates with occurrences
        List<LocalDate> sortedDates = byDate.keySet().stream().sorted().toList();

        LocalDate previousDate = null;
        for (LocalDate date : sortedDates) {
            List<TaskOccurrenceEntity> applicable = byDate.get(date).stream()
                    .filter(o -> !TRANSPARENT.contains(o.getStatus()))
                    .toList();

            if (applicable.isEmpty()) {
                // Transparent day — don't break or extend
                continue;
            }

            boolean allOk = applicable.stream()
                    .allMatch(o -> STREAK_OK.contains(o.getStatus()));

            if (allOk) {
                if (previousDate != null) {
                    // Check if this date is consecutive (allowing transparent gaps)
                    // Since we skip transparent days, we just check if there was a break
                    current++;
                } else {
                    current = 1;
                }
                previousDate = date;
            } else {
                longest = Math.max(longest, current);
                current = 0;
                previousDate = null;
            }
        }
        longest = Math.max(longest, current);
        return longest;
    }

    /** Immutable result of streak computation */
    public record StreakResult(
            int currentStreak,
            int longestStreak,
            boolean streakActive,
            List<String> allBadges,
            List<String> newBadges
    ) {}
}
