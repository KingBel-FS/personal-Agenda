package com.ia.api.notification.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.notification.api.NotificationController.NotificationJobItem;
import com.ia.api.notification.domain.NotificationJobEntity;
import com.ia.api.notification.repository.NotificationJobRepository;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.today.OccurrenceStatusEventEntity;
import com.ia.api.today.OccurrenceStatusEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationJobServiceTest {

    @Mock private NotificationJobRepository jobRepo;
    @Mock private UserRepository userRepo;
    @Mock private TaskOccurrenceRepository occurrenceRepo;
    @Mock private OccurrenceStatusEventRepository eventRepo;
    @Captor private ArgumentCaptor<List<NotificationJobEntity>> jobListCaptor;

    private NotificationJobService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        service = new NotificationJobService(jobRepo, userRepo, occurrenceRepo, eventRepo);
        user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
    }

    @Test
    void getJobsForOccurrence_returnsUserOwnedJobs() {
        UUID occurrenceId = UUID.randomUUID();
        when(userRepo.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        NotificationJobEntity job = buildJob(user.getId(), occurrenceId, "ON_TIME", "PENDING");
        when(jobRepo.findAllByOccurrenceIdOrderByScheduledAtAsc(occurrenceId)).thenReturn(List.of(job));

        List<NotificationJobItem> result = service.getJobsForOccurrence("test@example.com", occurrenceId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().triggerType()).isEqualTo("ON_TIME");
        assertThat(result.getFirst().status()).isEqualTo("PENDING");
    }

    @Test
    void getJobsForOccurrence_filtersOutOtherUsersJobs() {
        UUID occurrenceId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(userRepo.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        NotificationJobEntity otherJob = buildJob(otherUserId, occurrenceId, "ON_TIME", "PENDING");
        when(jobRepo.findAllByOccurrenceIdOrderByScheduledAtAsc(occurrenceId)).thenReturn(List.of(otherJob));

        List<NotificationJobItem> result = service.getJobsForOccurrence("test@example.com", occurrenceId);

        assertThat(result).isEmpty();
    }

    @Test
    void getJobsForOccurrence_unknownUser_throws() {
        when(userRepo.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJobsForOccurrence("unknown@example.com", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    @Test
    void cancelPendingJobsForOccurrence_cancelsPendingOnly() {
        UUID occurrenceId = UUID.randomUUID();
        NotificationJobEntity pendingJob = buildJob(user.getId(), occurrenceId, "BEFORE_15", "PENDING");
        NotificationJobEntity sentJob = buildJob(user.getId(), occurrenceId, "ON_TIME", "SENT");

        when(jobRepo.findAllByOccurrenceIdOrderByScheduledAtAsc(occurrenceId))
                .thenReturn(List.of(pendingJob, sentJob));

        service.cancelPendingJobsForOccurrence(occurrenceId);

        verify(jobRepo).saveAll(jobListCaptor.capture());
        List<NotificationJobEntity> saved = jobListCaptor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getStatus()).isEqualTo("CANCELED");
        assertThat(saved.getFirst().getCanceledAt()).isNotNull();
    }

    @Test
    void cancelPendingJobsForOccurrence_noPending_savesEmptyList() {
        UUID occurrenceId = UUID.randomUUID();
        NotificationJobEntity sentJob = buildJob(user.getId(), occurrenceId, "ON_TIME", "SENT");

        when(jobRepo.findAllByOccurrenceIdOrderByScheduledAtAsc(occurrenceId))
                .thenReturn(List.of(sentJob));

        service.cancelPendingJobsForOccurrence(occurrenceId);

        verify(jobRepo).saveAll(jobListCaptor.capture());
        assertThat(jobListCaptor.getValue()).isEmpty();
    }

    @Test
    void getJobsForOccurrence_includesDeliveryTimestamps() {
        UUID occurrenceId = UUID.randomUUID();
        when(userRepo.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        NotificationJobEntity job = buildJob(user.getId(), occurrenceId, "ON_TIME", "SENT");
        Instant sentAt = Instant.now();
        job.setSentAt(sentAt);
        when(jobRepo.findAllByOccurrenceIdOrderByScheduledAtAsc(occurrenceId)).thenReturn(List.of(job));

        List<NotificationJobItem> result = service.getJobsForOccurrence("test@example.com", occurrenceId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().sentAt()).isEqualTo(sentAt.toString());
        assertThat(result.getFirst().status()).isEqualTo("SENT");
    }

    @Test
    void cancelPendingJobsForOccurrence_cancelsAllFourPendingJobs() {
        UUID occurrenceId = UUID.randomUUID();
        NotificationJobEntity before15 = buildJob(user.getId(), occurrenceId, "BEFORE_15", "PENDING");
        NotificationJobEntity before2 = buildJob(user.getId(), occurrenceId, "BEFORE_2", "PENDING");
        NotificationJobEntity onTime = buildJob(user.getId(), occurrenceId, "ON_TIME", "PENDING");
        NotificationJobEntity after60 = buildJob(user.getId(), occurrenceId, "AFTER_60", "PENDING");

        when(jobRepo.findAllByOccurrenceIdOrderByScheduledAtAsc(occurrenceId))
                .thenReturn(List.of(before15, before2, onTime, after60));

        service.cancelPendingJobsForOccurrence(occurrenceId);

        verify(jobRepo).saveAll(jobListCaptor.capture());
        List<NotificationJobEntity> saved = jobListCaptor.getValue();
        assertThat(saved).hasSize(4);
        assertThat(saved).allMatch(j -> "CANCELED".equals(j.getStatus()));
        assertThat(saved).allMatch(j -> j.getCanceledAt() != null);
    }

    // ── executeAction tests ──────────────────────────────

    @Test
    void executeAction_done_updatesOccurrenceAndCancelsRemainingJobs() {
        UUID occurrenceId = UUID.randomUUID();
        NotificationJobEntity job = buildJob(user.getId(), occurrenceId, "ON_TIME", "SENT");

        TaskOccurrenceEntity occurrence = new TaskOccurrenceEntity();
        setField(occurrence, "id", occurrenceId);
        setField(occurrence, "userId", user.getId());
        occurrence.setStatus("planned");

        when(jobRepo.findById(job.getId())).thenReturn(Optional.of(job));
        when(occurrenceRepo.findById(occurrenceId)).thenReturn(Optional.of(occurrence));
        when(jobRepo.findAllByOccurrenceIdOrderByScheduledAtAsc(occurrenceId)).thenReturn(List.of());

        String result = service.executeAction(job.getId(), "DONE", occurrenceId);

        assertThat(result).isEqualTo("done");
        assertThat(occurrence.getStatus()).isEqualTo("done");
        verify(occurrenceRepo).save(occurrence);
        verify(eventRepo).save(any(OccurrenceStatusEventEntity.class));
    }

    @Test
    void executeAction_missed_updatesOccurrenceStatus() {
        UUID occurrenceId = UUID.randomUUID();
        NotificationJobEntity job = buildJob(user.getId(), occurrenceId, "AFTER_60", "SENT");

        TaskOccurrenceEntity occurrence = new TaskOccurrenceEntity();
        setField(occurrence, "id", occurrenceId);
        setField(occurrence, "userId", user.getId());
        occurrence.setStatus("planned");

        when(jobRepo.findById(job.getId())).thenReturn(Optional.of(job));
        when(occurrenceRepo.findById(occurrenceId)).thenReturn(Optional.of(occurrence));
        when(jobRepo.findAllByOccurrenceIdOrderByScheduledAtAsc(occurrenceId)).thenReturn(List.of());

        String result = service.executeAction(job.getId(), "MISSED", occurrenceId);

        assertThat(result).isEqualTo("missed");
        assertThat(occurrence.getStatus()).isEqualTo("missed");
    }

    @Test
    void executeAction_unknownJob_returnsNull() {
        UUID fakeJobId = UUID.randomUUID();
        when(jobRepo.findById(fakeJobId)).thenReturn(Optional.empty());

        String result = service.executeAction(fakeJobId, "DONE", UUID.randomUUID());

        assertThat(result).isNull();
        verify(occurrenceRepo, never()).save(any());
    }

    @Test
    void executeAction_occurrenceMismatch_returnsNull() {
        UUID occurrenceId = UUID.randomUUID();
        UUID wrongOccurrenceId = UUID.randomUUID();
        NotificationJobEntity job = buildJob(user.getId(), occurrenceId, "ON_TIME", "SENT");

        when(jobRepo.findById(job.getId())).thenReturn(Optional.of(job));

        String result = service.executeAction(job.getId(), "DONE", wrongOccurrenceId);

        assertThat(result).isNull();
        verify(occurrenceRepo, never()).save(any());
    }

    @Test
    void executeAction_occurrenceNotPlanned_skips() {
        UUID occurrenceId = UUID.randomUUID();
        NotificationJobEntity job = buildJob(user.getId(), occurrenceId, "ON_TIME", "SENT");

        TaskOccurrenceEntity occurrence = new TaskOccurrenceEntity();
        setField(occurrence, "id", occurrenceId);
        occurrence.setStatus("done");

        when(jobRepo.findById(job.getId())).thenReturn(Optional.of(job));
        when(occurrenceRepo.findById(occurrenceId)).thenReturn(Optional.of(occurrence));

        String result = service.executeAction(job.getId(), "DONE", occurrenceId);

        assertThat(result).isEqualTo("done");
        verify(occurrenceRepo, never()).save(any());
    }

    @Test
    void executeAction_unknownAction_returnsNull() {
        UUID occurrenceId = UUID.randomUUID();
        NotificationJobEntity job = buildJob(user.getId(), occurrenceId, "ON_TIME", "SENT");

        when(jobRepo.findById(job.getId())).thenReturn(Optional.of(job));

        String result = service.executeAction(job.getId(), "INVALID", occurrenceId);

        assertThat(result).isNull();
        verify(occurrenceRepo, never()).save(any());
    }

    // ── Helpers ──────────────────────────────────────────

    private NotificationJobEntity buildJob(UUID userId, UUID occurrenceId, String triggerType, String status) {
        NotificationJobEntity job = new NotificationJobEntity();
        setField(job, "id", UUID.randomUUID());
        setField(job, "userId", userId);
        setField(job, "occurrenceId", occurrenceId);
        setField(job, "triggerType", triggerType);
        setField(job, "scheduledAt", Instant.now());
        setField(job, "createdAt", Instant.now());
        job.setStatus(status);
        job.setUpdatedAt(Instant.now());
        return job;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
