package com.ia.worker.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recovery service that handles notification jobs stuck after a worker restart.
 * <p>
 * Since PENDING jobs use FOR UPDATE SKIP LOCKED, a crash during processing
 * releases the lock automatically. This service also:
 * - Cancels PENDING jobs for occurrences that are no longer 'planned'
 * - Purges old completed/failed jobs to prevent table bloat
 */
@Service
public class NotificationRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationRecoveryService.class);

    private final JdbcTemplate jdbcTemplate;

    public NotificationRecoveryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Runs every 5 minutes — cancels orphaned PENDING jobs whose occurrence
     * is no longer 'planned' (e.g. user marked done/missed while job was waiting).
     */
    @Scheduled(initialDelay = 30_000, fixedDelay = 300_000)
    @Transactional
    public void cancelOrphanedJobs() {
        int canceled = jdbcTemplate.update("""
                UPDATE notification_jobs nj
                SET status = 'CANCELED', canceled_at = now(), updated_at = now()
                WHERE nj.status = 'PENDING'
                  AND EXISTS (
                      SELECT 1 FROM task_occurrences o
                      WHERE o.id = nj.occurrence_id AND o.status != 'planned'
                  )
                """);

        if (canceled > 0) {
            log.info("notification.recovery: canceled {} orphaned jobs", canceled);
        }
    }

    /**
     * Runs daily — deletes notification_jobs older than 7 days that are SENT, CANCELED, or FAILED.
     */
    @Scheduled(initialDelay = 60_000, fixedDelay = 86_400_000)
    @Transactional
    public void purgeOldJobs() {
        int deleted = jdbcTemplate.update("""
                DELETE FROM notification_jobs
                WHERE status IN ('SENT', 'CANCELED', 'FAILED')
                  AND updated_at < now() - interval '7 days'
                """);

        if (deleted > 0) {
            log.info("notification.recovery: purged {} old jobs", deleted);
        }
    }
}
