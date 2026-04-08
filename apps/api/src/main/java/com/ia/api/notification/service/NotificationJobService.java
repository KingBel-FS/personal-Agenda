package com.ia.api.notification.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.notification.api.NotificationController.NotificationJobItem;
import com.ia.api.notification.domain.NotificationJobEntity;
import com.ia.api.notification.repository.NotificationJobRepository;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.today.OccurrenceStatusEventEntity;
import com.ia.api.today.OccurrenceStatusEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationJobService {

    private static final Logger log = LoggerFactory.getLogger(NotificationJobService.class);

    private final NotificationJobRepository jobRepo;
    private final UserRepository userRepo;
    private final TaskOccurrenceRepository occurrenceRepo;
    private final OccurrenceStatusEventRepository eventRepo;

    public NotificationJobService(
            NotificationJobRepository jobRepo,
            UserRepository userRepo,
            TaskOccurrenceRepository occurrenceRepo,
            OccurrenceStatusEventRepository eventRepo
    ) {
        this.jobRepo = jobRepo;
        this.userRepo = userRepo;
        this.occurrenceRepo = occurrenceRepo;
        this.eventRepo = eventRepo;
    }

    /**
     * Returns notification jobs for a given occurrence (observability endpoint).
     * Validates that the requesting user owns the occurrence.
     */
    public List<NotificationJobItem> getJobsForOccurrence(String email, UUID occurrenceId) {
        UUID userId = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"))
                .getId();

        return jobRepo.findAllByOccurrenceIdOrderByScheduledAtAsc(occurrenceId).stream()
                .filter(j -> j.getUserId().equals(userId))
                .map(j -> new NotificationJobItem(
                        j.getId(),
                        j.getTriggerType(),
                        j.getScheduledAt().toString(),
                        j.getStatus(),
                        j.getSentAt() != null ? j.getSentAt().toString() : null,
                        j.getCanceledAt() != null ? j.getCanceledAt().toString() : null,
                        j.getErrorMessage()
                ))
                .toList();
    }

    /**
     * Executes a push action (DONE/MISSED) triggered from the service worker.
     * Uses notificationJobId as an implicit auth token — only the push recipient knows it.
     * Returns the new status string, or null if the action was rejected.
     */
    @Transactional
    public String executeAction(UUID notificationJobId, String action, UUID taskOccurrenceId) {
        NotificationJobEntity job = jobRepo.findById(notificationJobId).orElse(null);
        if (job == null) {
            log.warn("notification.action: unknown job {}", notificationJobId);
            return null;
        }

        // Validate occurrence matches the job
        if (!job.getOccurrenceId().equals(taskOccurrenceId)) {
            log.warn("notification.action: occurrence mismatch for job {}", notificationJobId);
            return null;
        }

        // Map action to status
        String newStatus = switch (action) {
            case "DONE" -> "done";
            case "MISSED" -> "missed";
            default -> null;
        };
        if (newStatus == null) {
            log.warn("notification.action: unknown action '{}' for job {}", action, notificationJobId);
            return null;
        }

        // Load the occurrence
        TaskOccurrenceEntity occurrence = occurrenceRepo.findById(taskOccurrenceId).orElse(null);
        if (occurrence == null || !"planned".equals(occurrence.getStatus())) {
            log.info("notification.action: occurrence {} not planned, skipping", taskOccurrenceId);
            return occurrence != null ? occurrence.getStatus() : null;
        }

        // Persist status change
        String previousStatus = occurrence.getStatus();
        occurrence.setStatus(newStatus);
        occurrence.setUpdatedAt(Instant.now());
        occurrenceRepo.save(occurrence);

        // Record audit event
        var event = new OccurrenceStatusEventEntity();
        event.setOccurrenceId(taskOccurrenceId);
        event.setUserId(job.getUserId());
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        eventRepo.save(event);

        // Cancel remaining PENDING notification jobs for this occurrence
        cancelPendingJobsForOccurrence(taskOccurrenceId);

        log.info("notification.action: {} occurrence {} via push action (job {})",
                newStatus, taskOccurrenceId, notificationJobId);
        return newStatus;
    }

    /**
     * Cancels all PENDING notification jobs for a given occurrence.
     * Called by OccurrenceStatusService when a task is marked done/missed/canceled.
     */
    @Transactional
    public void cancelPendingJobsForOccurrence(UUID occurrenceId) {
        List<NotificationJobEntity> pendingJobs =
                jobRepo.findAllByOccurrenceIdOrderByScheduledAtAsc(occurrenceId).stream()
                        .filter(j -> "PENDING".equals(j.getStatus()))
                        .toList();

        Instant now = Instant.now();
        for (NotificationJobEntity job : pendingJobs) {
            job.setStatus("CANCELED");
            job.setCanceledAt(now);
            job.setUpdatedAt(now);
        }
        jobRepo.saveAll(pendingJobs);
    }
}
