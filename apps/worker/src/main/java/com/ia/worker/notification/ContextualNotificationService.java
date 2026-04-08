package com.ia.worker.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Generates contextual push notifications:
 * - Streak danger at 20:00 (if streak active and no task done today)
 * - Birthday reminder (1h after wake-up on birth date anniversary)
 *
 * Sends directly via WebPushSender and records in notification_center.
 * Dedup: checks notification_center for today's date + notification_type per user.
 */
@Service
public class ContextualNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ContextualNotificationService.class);
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalTime STREAK_DANGER_TIME = LocalTime.of(20, 0);

    private final JdbcTemplate jdbcTemplate;
    private final WebPushSender webPushSender;

    public ContextualNotificationService(JdbcTemplate jdbcTemplate, WebPushSender webPushSender) {
        this.jdbcTemplate = jdbcTemplate;
        this.webPushSender = webPushSender;
    }

    @Scheduled(
            initialDelayString = "${contextual.notification.initial-delay-ms:30000}",
            fixedDelayString = "${contextual.notification.fixed-delay-ms:60000}"
    )
    public void checkAndSend() {
        ZonedDateTime now = ZonedDateTime.now(PARIS);
        checkAndSend(now.toLocalDate(), now.toLocalTime());
    }

    /** Package-private for testing with controlled time. */
    void checkAndSend(LocalDate today, LocalTime currentTime) {
        checkStreakDanger(today, currentTime);
        checkBirthday(today, currentTime);
        checkWeeklyGoals(today, currentTime);
        checkMonthlyGoals(today, currentTime);
    }

    // ── Streak danger at 20h ──────────────────────────────

    private void checkStreakDanger(LocalDate today, LocalTime currentTime) {
        if (currentTime.isBefore(STREAK_DANGER_TIME)) return;

        List<StreakDangerCandidate> candidates = jdbcTemplate.query("""
                SELECT ss.user_id, ss.current_streak
                FROM streak_snapshots ss
                WHERE ss.snapshot_date = ?
                  AND ss.streak_active = true
                  AND ss.current_streak > 0
                  AND NOT EXISTS (
                      SELECT 1 FROM task_occurrences o
                      WHERE o.user_id = ss.user_id
                        AND o.occurrence_date = ?
                        AND o.status = 'done'
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM notification_center nc
                      WHERE nc.user_id = ss.user_id
                        AND nc.notification_type = 'STREAK_DANGER'
                        AND nc.created_at::date = ?
                  )
                """,
                (rs, rowNum) -> new StreakDangerCandidate(
                        rs.getObject("user_id", UUID.class),
                        rs.getInt("current_streak")
                ),
                today, today, today
        );

        for (StreakDangerCandidate c : candidates) {
            sendStreakDanger(c);
        }

        if (!candidates.isEmpty()) {
            log.info("contextual.notification: sent {} streak danger notifications", candidates.size());
        }
    }

    private void sendStreakDanger(StreakDangerCandidate candidate) {
        String title = "🔥 Ta série de " + candidate.currentStreak() + " jours est en danger !";
        String body = "Tu n'as terminé aucune tâche aujourd'hui. Fais-en une pour garder ta série.";

        var payload = new WebPushSender.PushPayload(
                title, body,
                "/icons/icon-192x192.png",
                "/icons/badge-72x72.png",
                null, null, null, "/", false, List.of()
        );

        sendToUser(candidate.userId(), payload, "STREAK_DANGER", title, body);
    }

    // ── Birthday reminder ─────────────────────────────────

    private void checkBirthday(LocalDate today, LocalTime currentTime) {
        List<BirthdayCandidate> candidates = jdbcTemplate.query("""
                SELECT u.id AS user_id, u.pseudo,
                       dp.wake_up_time
                FROM users u
                LEFT JOIN day_profiles dp ON dp.user_id = u.id
                    AND dp.day_category = (
                        CASE EXTRACT(DOW FROM ?::date)
                            WHEN 0 THEN 'WEEKEND_HOLIDAY'
                            WHEN 6 THEN 'WEEKEND_HOLIDAY'
                            ELSE 'WORKDAY'
                        END
                    )
                WHERE u.account_status = 'ACTIVE'
                  AND EXTRACT(MONTH FROM u.birth_date) = EXTRACT(MONTH FROM ?::date)
                  AND EXTRACT(DAY FROM u.birth_date) = EXTRACT(DAY FROM ?::date)
                  AND NOT EXISTS (
                      SELECT 1 FROM notification_center nc
                      WHERE nc.user_id = u.id
                        AND nc.notification_type = 'BIRTHDAY'
                        AND nc.created_at::date = ?
                  )
                """,
                (rs, rowNum) -> new BirthdayCandidate(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("pseudo"),
                        rs.getTime("wake_up_time") != null
                                ? rs.getTime("wake_up_time").toLocalTime()
                                : LocalTime.of(8, 0)
                ),
                today, today, today, today
        );

        for (BirthdayCandidate c : candidates) {
            // Send 1h after wake-up
            LocalTime sendAfter = c.wakeUpTime().plusHours(1);
            if (currentTime.isBefore(sendAfter)) continue;

            sendBirthday(c);
        }
    }

    private void sendBirthday(BirthdayCandidate candidate) {
        String title = "🎂 Joyeux anniversaire " + candidate.pseudo() + " !";
        String body = "Toute l'équipe te souhaite une excellente journée. Continue comme ça !";

        var payload = new WebPushSender.PushPayload(
                title, body,
                "/icons/icon-192x192.png",
                "/icons/badge-72x72.png",
                null, null, null, "/", false, List.of()
        );

        sendToUser(candidate.userId(), payload, "BIRTHDAY", title, body);
    }

    // ── Shared send logic ─────────────────────────────────

    private void checkWeeklyGoals(LocalDate today, LocalTime currentTime) {
        if (today.getDayOfWeek().getValue() != 7 || currentTime.isBefore(STREAK_DANGER_TIME)) return;
        processGoalReminders(today.minusDays(6), today, "WEEKLY", "WEEKLY_GOAL", "Objectif hebdomadaire");
    }

    private void checkMonthlyGoals(LocalDate today, LocalTime currentTime) {
        if (!today.equals(today.withDayOfMonth(today.lengthOfMonth())) || currentTime.isBefore(STREAK_DANGER_TIME)) return;
        processGoalReminders(today.withDayOfMonth(1), today, "MONTHLY", "MONTHLY_GOAL", "Objectif mensuel");
    }

    private void processGoalReminders(
            LocalDate periodStart,
            LocalDate periodEnd,
            String periodType,
            String notificationType,
            String titlePrefix
    ) {
        List<UUID> userIds = loadGoalUsers(periodType);
        for (UUID userId : userIds) {
            if (hasNotificationToday(userId, notificationType, periodEnd)) continue;
            List<GoalReminderCandidate> unmetGoals = loadUnmetGoals(userId, periodType, periodStart, periodEnd);
            if (unmetGoals.isEmpty()) continue;
            sendGoalReminder(userId, unmetGoals, notificationType, titlePrefix);
        }
    }

    private void sendGoalReminder(UUID userId, List<GoalReminderCandidate> unmetGoals, String notificationType, String titlePrefix) {
        String title = unmetGoals.size() == 1
                ? titlePrefix + " non atteint"
                : titlePrefix + "s non atteints";
        String body = unmetGoals.stream()
                .limit(2)
                .map(goal -> {
                    String label = goal.taskTitle() != null ? goal.taskTitle() : "Objectif global";
                    return label + " " + goal.completedCount() + "/" + goal.targetCount();
                })
                .reduce((left, right) -> left + " • " + right)
                .orElse("Des objectifs restent en retard.");

        var payload = new WebPushSender.PushPayload(
                title, body,
                "/icons/icon-192x192.png",
                "/icons/badge-72x72.png",
                null, null, null, "/notifications", false, List.of()
        );

        sendToUser(userId, payload, notificationType, title, body);
    }

    private List<UUID> loadGoalUsers(String periodType) {
        return jdbcTemplate.query("""
                SELECT DISTINCT user_id
                FROM goals
                WHERE active = true
                  AND period_type = ?
                """,
                (rs, rowNum) -> rs.getObject("user_id", UUID.class),
                periodType
        );
    }

    private List<GoalReminderCandidate> loadUnmetGoals(UUID userId, String periodType, LocalDate periodStart, LocalDate periodEnd) {
        List<GoalReminderCandidate> goals = jdbcTemplate.query("""
                SELECT g.id,
                       g.goal_scope,
                       g.target_count,
                       g.task_definition_id,
                       td.title AS task_title,
                       COALESCE((
                           SELECT COUNT(*)
                           FROM task_occurrences o
                           WHERE o.user_id = g.user_id
                             AND o.status = 'done'
                             AND o.occurrence_date BETWEEN ? AND ?
                             AND (
                                 g.task_definition_id IS NULL
                                 OR o.task_definition_id = g.task_definition_id
                             )
                       ), 0) AS completed_count
                FROM goals g
                LEFT JOIN task_definitions td ON td.id = g.task_definition_id
                WHERE g.user_id = ?
                  AND g.active = true
                  AND g.period_type = ?
                """,
                (rs, rowNum) -> new GoalReminderCandidate(
                        rs.getObject("id", UUID.class),
                        rs.getString("goal_scope"),
                        rs.getInt("target_count"),
                        rs.getObject("task_definition_id", UUID.class),
                        rs.getString("task_title"),
                        rs.getInt("completed_count")
                ),
                periodStart, periodEnd, userId, periodType
        );

        return goals.stream()
                .filter(goal -> goal.completedCount() < goal.targetCount())
                .toList();
    }

    private boolean hasNotificationToday(UUID userId, String notificationType, LocalDate today) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM notification_center
                WHERE user_id = ?
                  AND notification_type = ?
                  AND created_at::date = ?
                """,
                Integer.class,
                userId, notificationType, today
        );
        return count != null && count > 0;
    }

    private void sendToUser(UUID userId, WebPushSender.PushPayload payload, String notificationType,
                            String title, String body) {
        List<SubscriptionRow> subs = loadActiveSubscriptions(userId);
        if (subs.isEmpty()) return;

        boolean anySent = false;
        for (SubscriptionRow sub : subs) {
            int status = webPushSender.sendWithStatus(sub.endpoint(), sub.authKey(), sub.p256dhKey(), payload);
            if (status == 200 || status == 201) {
                anySent = true;
            } else if (status == 404 || status == 410) {
                revokeSubscription(sub.id());
                log.info("contextual.notification: revoked dead subscription {}", sub.id());
            }
        }

        if (anySent) {
            insertNotificationCenter(userId, title, body, notificationType);
        }
    }

    private List<SubscriptionRow> loadActiveSubscriptions(UUID userId) {
        return jdbcTemplate.query("""
                SELECT id, endpoint, auth_key, p256dh_key
                FROM push_subscriptions
                WHERE user_id = ? AND revoked_at IS NULL
                """,
                (rs, rowNum) -> new SubscriptionRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("endpoint"),
                        rs.getString("auth_key"),
                        rs.getString("p256dh_key")
                ),
                userId
        );
    }

    private void insertNotificationCenter(UUID userId, String title, String body, String notificationType) {
        jdbcTemplate.update("""
                INSERT INTO notification_center (id, user_id, title, body, notification_type, status, created_at)
                VALUES (?, ?, ?, ?, ?, 'RECEIVED', now())
                """,
                UUID.randomUUID(), userId, title, body, notificationType
        );
    }

    private void revokeSubscription(UUID subId) {
        jdbcTemplate.update(
                "UPDATE push_subscriptions SET revoked_at = now(), updated_at = now() WHERE id = ?",
                subId
        );
    }

    // ── Records ───────────────────────────────────────────

    record StreakDangerCandidate(UUID userId, int currentStreak) {}
    record BirthdayCandidate(UUID userId, String pseudo, LocalTime wakeUpTime) {}
    record GoalReminderCandidate(
            UUID goalId,
            String goalScope,
            int targetCount,
            UUID taskDefinitionId,
            String taskTitle,
            int completedCount
    ) {}
    record SubscriptionRow(UUID id, String endpoint, String authKey, String p256dhKey) {}
}
