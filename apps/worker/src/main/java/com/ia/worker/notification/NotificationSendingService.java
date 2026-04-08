package com.ia.worker.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Picks up PENDING notification jobs whose scheduled_at has passed,
 * sends web push to all active user subscriptions, and updates status.
 * Uses FOR UPDATE SKIP LOCKED for concurrent-safe processing.
 */
@Service
public class NotificationSendingService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSendingService.class);
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final WebPushSender webPushSender;
    private final int batchSize;

    public NotificationSendingService(
            JdbcTemplate jdbcTemplate,
            WebPushSender webPushSender,
            @Value("${notification.sending.batch-size:50}") int batchSize
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.webPushSender = webPushSender;
        this.batchSize = batchSize;
    }

    @Scheduled(
            initialDelayString = "${notification.sending.initial-delay-ms:10000}",
            fixedDelayString = "${notification.sending.fixed-delay-ms:10000}"
    )
    @Transactional
    public void processReadyJobs() {
        List<NotificationJobRow> jobs = pickReadyJobs();
        if (jobs.isEmpty()) return;

        log.info("notification.sending: processing {} ready jobs", jobs.size());

        for (NotificationJobRow job : jobs) {
            try {
                sendJob(job);
            } catch (Exception e) {
                markFailed(job.id(), e.getMessage());
                log.error("notification.sending: job {} failed", job.id(), e);
            }
        }
    }

    private List<NotificationJobRow> pickReadyJobs() {
        return jdbcTemplate.query("""
                SELECT nj.id, nj.user_id, nj.occurrence_id, nj.trigger_type, nj.scheduled_at,
                       td.title AS task_name, o.occurrence_date, o.occurrence_time,
                       o.task_definition_id
                FROM notification_jobs nj
                JOIN task_occurrences o ON o.id = nj.occurrence_id
                JOIN task_definitions td ON td.id = o.task_definition_id
                WHERE nj.status = 'PENDING'
                  AND nj.scheduled_at <= now()
                ORDER BY nj.scheduled_at ASC
                LIMIT ?
                FOR UPDATE OF nj SKIP LOCKED
                """,
                (rs, rowNum) -> new NotificationJobRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("occurrence_id", UUID.class),
                        rs.getString("trigger_type"),
                        rs.getTimestamp("scheduled_at").toInstant(),
                        rs.getString("task_name"),
                        rs.getDate("occurrence_date").toLocalDate(),
                        rs.getTime("occurrence_time").toLocalTime(),
                        rs.getObject("task_definition_id", UUID.class)
                ),
                batchSize
        );
    }

    private void sendJob(NotificationJobRow job) {
        // Check if occurrence was already completed — cancel remaining jobs
        String occurrenceStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM task_occurrences WHERE id = ?",
                String.class,
                job.occurrenceId()
        );

        if (!"planned".equals(occurrenceStatus)) {
            markCanceled(job.id());
            log.info("notification.sending: canceled job {} — occurrence {} status is '{}'",
                    job.id(), job.occurrenceId(), occurrenceStatus);
            return;
        }

        // Build push payload
        String title = buildTitle(job);
        String body = buildBody(job);

        // Profile photo as image (loaded from user)
        String imageUrl = loadProfilePhotoUrl(job.userId());

        // Actions: DONE / MISSED buttons on the notification
        List<WebPushSender.PushAction> actions = List.of(
                new WebPushSender.PushAction("DONE", "Exécuté ✅"),
                new WebPushSender.PushAction("MISSED", "Non exécuté ❌")
        );

        var payload = new WebPushSender.PushPayload(
                title, body,
                "/icons/icon-192x192.png",
                "/icons/badge-72x72.png",
                imageUrl,
                job.id().toString(),
                job.occurrenceId().toString(),
                "/",
                true,
                actions
        );

        // Send to all active subscriptions
        List<SubscriptionRow> subs = loadActiveSubscriptions(job.userId());
        if (subs.isEmpty()) {
            log.warn("notification.sending: no active subscriptions for user {}", job.userId());
            markFailed(job.id(), "NO_ACTIVE_SUBSCRIPTIONS");
            return;
        }

        boolean anySent = false;
        for (SubscriptionRow sub : subs) {
            int status = webPushSender.sendWithStatus(sub.endpoint(), sub.authKey(), sub.p256dhKey(), payload);
            if (status == 200 || status == 201) {
                anySent = true;
            } else if (status == 404 || status == 410) {
                revokeSubscription(sub.id());
                log.info("notification.sending: revoked dead subscription {}", sub.id());
            }
        }

        if (anySent) {
            markSent(job.id());
            insertNotificationCenter(job, title, body);
            log.info("notification.sending: sent job {} ({}) for task '{}'",
                    job.id(), job.triggerType(), job.taskName());
        } else {
            markFailed(job.id(), "ALL_SUBSCRIPTIONS_FAILED");
        }
    }

    private String buildTitle(NotificationJobRow job) {
        String time = job.occurrenceTime().format(TIME_FMT);
        return switch (job.triggerType()) {
            case "BEFORE_15" -> "⏰ " + job.taskName() + " dans 15 min";
            case "BEFORE_2"  -> "🔔 " + job.taskName() + " dans 2 min";
            case "ON_TIME"   -> "▶️ " + job.taskName() + " — c'est l'heure !";
            case "AFTER_60"  -> "⚠️ " + job.taskName() + " — pas encore fait ?";
            default -> job.taskName();
        };
    }

    private String buildBody(NotificationJobRow job) {
        String time = job.occurrenceTime().format(TIME_FMT);
        return switch (job.triggerType()) {
            case "BEFORE_15" -> "Prévu à " + time + ". Prépare-toi !";
            case "BEFORE_2"  -> "C'est presque l'heure (" + time + ")";
            case "ON_TIME"   -> "Marque comme fait quand c'est terminé";
            case "AFTER_60"  -> "Tu avais prévu ça à " + time + ". Oublié ?";
            default -> "";
        };
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

    private void markSent(UUID jobId) {
        jdbcTemplate.update(
                "UPDATE notification_jobs SET status = 'SENT', sent_at = now(), updated_at = now() WHERE id = ?",
                jobId
        );
    }

    private void markFailed(UUID jobId, String errorMessage) {
        jdbcTemplate.update(
                "UPDATE notification_jobs SET status = 'FAILED', error_message = ?, updated_at = now() WHERE id = ?",
                errorMessage, jobId
        );
    }

    private void markCanceled(UUID jobId) {
        jdbcTemplate.update(
                "UPDATE notification_jobs SET status = 'CANCELED', canceled_at = now(), updated_at = now() WHERE id = ?",
                jobId
        );
    }

    private void insertNotificationCenter(NotificationJobRow job, String title, String body) {
        jdbcTemplate.update("""
                INSERT INTO notification_center (id, user_id, title, body, notification_type, related_task_id, status, created_at)
                VALUES (?, ?, ?, ?, 'TASK_REMINDER', ?, 'RECEIVED', now())
                """,
                UUID.randomUUID(),
                job.userId(),
                title,
                body,
                job.taskDefinitionId() != null ? job.taskDefinitionId() : null
        );
    }

    private String loadProfilePhotoUrl(UUID userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT profile_photo_url FROM users WHERE id = ?",
                    String.class,
                    userId
            );
        } catch (Exception e) {
            return null;
        }
    }

    private void revokeSubscription(UUID subId) {
        jdbcTemplate.update(
                "UPDATE push_subscriptions SET revoked_at = now(), updated_at = now() WHERE id = ?",
                subId
        );
    }

    record NotificationJobRow(
            UUID id, UUID userId, UUID occurrenceId, String triggerType, Instant scheduledAt,
            String taskName, java.time.LocalDate occurrenceDate, java.time.LocalTime occurrenceTime,
            UUID taskDefinitionId
    ) {}

    record SubscriptionRow(UUID id, String endpoint, String authKey, String p256dhKey) {}
}
