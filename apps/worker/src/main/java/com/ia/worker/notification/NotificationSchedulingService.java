package com.ia.worker.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scans planned occurrences within a configurable horizon and creates
 * sequential notification jobs: -15 min, -2 min, on time, +60 min.
 * Idempotent — unique constraint on (occurrence_id, trigger_type) prevents duplicates.
 */
@Service
public class NotificationSchedulingService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchedulingService.class);
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static final String BEFORE_15 = "BEFORE_15";
    private static final String BEFORE_2  = "BEFORE_2";
    private static final String ON_TIME   = "ON_TIME";
    private static final String AFTER_60  = "AFTER_60";

    private final JdbcTemplate jdbcTemplate;
    private final int horizonHours;

    public NotificationSchedulingService(
            JdbcTemplate jdbcTemplate,
            @Value("${notification.scheduling.horizon-hours:2}") int horizonHours
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.horizonHours = horizonHours;
    }

    @Scheduled(
            initialDelayString = "${notification.scheduling.initial-delay-ms:10000}",
            fixedDelayString = "${notification.scheduling.fixed-delay-ms:30000}"
    )
    @Transactional
    public void scheduleNotifications() {
        ZonedDateTime now = ZonedDateTime.now(PARIS);
        ZonedDateTime horizon = now.plusHours(horizonHours);
        LocalDate today = now.toLocalDate();

        List<OccurrenceRow> occurrences = loadPlannedOccurrences(today, now.toLocalTime(), horizon);

        int created = 0;
        for (OccurrenceRow occ : occurrences) {
            ZonedDateTime occDateTime = ZonedDateTime.of(occ.occurrenceDate(), occ.occurrenceTime(), PARIS);
            created += createJobIfAbsent(occ, BEFORE_15, occDateTime.minusMinutes(15).toInstant());
            created += createJobIfAbsent(occ, BEFORE_2,  occDateTime.minusMinutes(2).toInstant());
            created += createJobIfAbsent(occ, ON_TIME,   occDateTime.toInstant());
            created += createJobIfAbsent(occ, AFTER_60,  occDateTime.plusMinutes(60).toInstant());
        }

        if (created > 0) {
            log.info("notification.scheduling: created {} jobs for {} occurrences", created, occurrences.size());
        }
    }

    private List<OccurrenceRow> loadPlannedOccurrences(LocalDate today, LocalTime nowTime, ZonedDateTime horizon) {
        return jdbcTemplate.query("""
                SELECT o.id, o.user_id, o.task_definition_id, o.occurrence_date, o.occurrence_time,
                       td.title AS task_name
                FROM task_occurrences o
                JOIN task_definitions td ON td.id = o.task_definition_id
                WHERE o.status = 'planned'
                  AND o.occurrence_date >= ?
                  AND NOT EXISTS (
                      SELECT 1 FROM notification_jobs nj
                      WHERE nj.occurrence_id = o.id AND nj.trigger_type = 'ON_TIME'
                        AND nj.status = 'PENDING'
                  )
                """,
                (rs, rowNum) -> new OccurrenceRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("task_definition_id", UUID.class),
                        rs.getDate("occurrence_date").toLocalDate(),
                        rs.getTime("occurrence_time").toLocalTime(),
                        rs.getString("task_name")
                ),
                today
        );
    }

    /**
     * Creates a notification job if one doesn't already exist for this (occurrence, trigger_type).
     * Returns 1 if inserted, 0 if duplicate.
     */
    private int createJobIfAbsent(OccurrenceRow occ, String triggerType, Instant scheduledAt) {
        // Skip jobs scheduled in the past (except AFTER_60 which may still be relevant)
        if (!triggerType.equals(AFTER_60) && scheduledAt.isBefore(Instant.now())) {
            return 0;
        }

        int inserted = jdbcTemplate.update("""
                INSERT INTO notification_jobs (id, user_id, occurrence_id, trigger_type, scheduled_at, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'PENDING', now(), now())
                ON CONFLICT (occurrence_id, trigger_type)
                DO UPDATE SET scheduled_at = EXCLUDED.scheduled_at,
                              status = 'PENDING',
                              sent_at = NULL,
                              canceled_at = NULL,
                              error_message = NULL,
                              updated_at = now()
                WHERE notification_jobs.status IN ('CANCELED', 'FAILED')
                """,
                UUID.randomUUID(),
                occ.userId(),
                occ.id(),
                triggerType,
                java.sql.Timestamp.from(scheduledAt)
        );

        if (inserted > 0) {
            log.debug("notification.scheduling: created {} job for occurrence {} at {}",
                    triggerType, occ.id(), scheduledAt);
        }
        return inserted;
    }

    record OccurrenceRow(
            UUID id,
            UUID userId,
            UUID taskDefinitionId,
            LocalDate occurrenceDate,
            LocalTime occurrenceTime,
            String taskName
    ) {}
}
