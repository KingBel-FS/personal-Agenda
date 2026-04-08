package com.ia.api.today;

import com.ia.api.auth.repository.UserRepository;
import com.ia.api.notification.service.NotificationJobService;
import com.ia.api.sync.service.RealtimeSyncService;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.domain.TaskOverrideEntity;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskOverrideRepository;
import com.ia.api.task.repository.TaskRuleRepository;
import com.ia.api.task.service.DayClassificationReadService;
import com.ia.api.task.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

@Service
public class OccurrenceStatusService {

    private static final Set<String> ALLOWED_TARGETS = Set.of("done", "missed", "canceled");
    private static final Set<String> MUTABLE_FROM = Set.of("planned", "done", "missed");

    private final UserRepository userRepository;
    private final TaskOccurrenceRepository occurrenceRepository;
    private final OccurrenceStatusEventRepository eventRepository;
    private final TaskOverrideRepository overrideRepository;
    private final TaskRuleRepository ruleRepository;
    private final DayClassificationReadService dayClassificationReadService;
    private final TodayService todayService;
    private final NotificationJobService notificationJobService;
    private final RealtimeSyncService realtimeSyncService;

    public OccurrenceStatusService(
            UserRepository userRepository,
            TaskOccurrenceRepository occurrenceRepository,
            OccurrenceStatusEventRepository eventRepository,
            TaskOverrideRepository overrideRepository,
            TaskRuleRepository ruleRepository,
            DayClassificationReadService dayClassificationReadService,
            TodayService todayService,
            NotificationJobService notificationJobService,
            RealtimeSyncService realtimeSyncService
    ) {
        this.userRepository = userRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.eventRepository = eventRepository;
        this.overrideRepository = overrideRepository;
        this.ruleRepository = ruleRepository;
        this.dayClassificationReadService = dayClassificationReadService;
        this.todayService = todayService;
        this.notificationJobService = notificationJobService;
        this.realtimeSyncService = realtimeSyncService;
    }

    @Transactional
    public TodayResponse changeStatus(String email, UUID occurrenceId, String newStatus) {
        if (!ALLOWED_TARGETS.contains(newStatus)) {
            throw new IllegalArgumentException("Statut cible invalide : " + newStatus);
        }

        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        var occurrence = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new IllegalArgumentException("Occurrence introuvable."));

        if (!occurrence.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Occurrence introuvable.");
        }

        LocalDate today = LocalDate.now(TaskService.PARIS);
        LocalDate occurrenceDate = occurrence.getOccurrenceDate();
        if ("canceled".equals(newStatus) && occurrenceDate.isBefore(today)) {
            throw new IllegalStateException("Une occurrence passee ne peut pas etre supprimee.");
        }

        String previousStatus = occurrence.getStatus();
        if (previousStatus.equals(newStatus)) {
            return todayService.getForDate(email, occurrenceDate);
        }

        if (!MUTABLE_FROM.contains(previousStatus)) {
            throw new IllegalStateException(
                    "Impossible de modifier une occurrence au statut « " + previousStatus + " ».");
        }

        occurrence.setStatus(newStatus);
        occurrence.setUpdatedAt(Instant.now());
        occurrenceRepository.save(occurrence);

        // Persist a DELETE override so the cancellation survives occurrence refresh
        if ("canceled".equals(newStatus)) {
            var override = overrideRepository
                    .findByTaskRuleIdAndOccurrenceDateAndTaskTimeSlotId(
                            occurrence.getTaskRuleId(), occurrenceDate, occurrence.getTaskTimeSlotId())
                    .orElseGet(() -> {
                        var o = new TaskOverrideEntity();
                        o.setUserId(user.getId());
                        o.setTaskDefinitionId(occurrence.getTaskDefinitionId());
                        o.setTaskRuleId(occurrence.getTaskRuleId());
                        o.setOccurrenceDate(occurrenceDate);
                        o.setTaskTimeSlotId(occurrence.getTaskTimeSlotId());
                        o.setCreatedAt(Instant.now());
                        return o;
                    });
            override.setOverrideAction("DELETE");
            override.setOverrideStatus("canceled");
            override.setUpdatedAt(Instant.now());
            overrideRepository.save(override);
        }

        var event = new OccurrenceStatusEventEntity();
        event.setOccurrenceId(occurrenceId);
        event.setUserId(user.getId());
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        eventRepository.save(event);

        if (!"planned".equals(newStatus)) {
            notificationJobService.cancelPendingJobsForOccurrence(occurrenceId);
        }

        TodayResponse response = todayService.getForDate(email, occurrenceDate);
        try { realtimeSyncService.publish(email, "TODAY"); } catch (Exception ignored) {}
        return response;
    }

    @Transactional
    public TodayResponse updateDescription(String email, UUID occurrenceId, String description) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        var occurrence = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new IllegalArgumentException("Occurrence introuvable."));

        if (!occurrence.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Occurrence introuvable.");
        }

        LocalDate today = LocalDate.now(TaskService.PARIS);
        LocalDate occurrenceDate = occurrence.getOccurrenceDate();
        if (!occurrenceDate.equals(today)) {
            throw new IllegalStateException("La description n'est modifiable que pour les occurrences du jour.");
        }

        if ("canceled".equals(occurrence.getStatus()) || "skipped".equals(occurrence.getStatus())) {
            throw new IllegalStateException("Impossible de modifier cette occurrence.");
        }

        var override = overrideRepository
                .findByTaskRuleIdAndOccurrenceDateAndTaskTimeSlotId(
                        occurrence.getTaskRuleId(), occurrenceDate, occurrence.getTaskTimeSlotId())
                .orElseGet(() -> {
                    var o = new TaskOverrideEntity();
                    o.setUserId(user.getId());
                    o.setTaskDefinitionId(occurrence.getTaskDefinitionId());
                    o.setTaskRuleId(occurrence.getTaskRuleId());
                    o.setOccurrenceDate(occurrenceDate);
                    o.setTaskTimeSlotId(occurrence.getTaskTimeSlotId());
                    o.setOverrideAction("MODIFY");
                    o.setCreatedAt(Instant.now());
                    return o;
                });
        override.setDescription(description);
        override.setUpdatedAt(Instant.now());
        overrideRepository.save(override);

        TodayResponse response = todayService.getForDate(email, occurrenceDate);
        try { realtimeSyncService.publish(email, "TODAY"); } catch (Exception ignored) {}
        return response;
    }

    @Transactional
    public TodayResponse updateTime(String email, UUID occurrenceId, LocalTime newTime) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        var occurrence = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new IllegalArgumentException("Occurrence introuvable."));

        if (!occurrence.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Occurrence introuvable.");
        }

        LocalDate occurrenceDate = occurrence.getOccurrenceDate();
        if ("canceled".equals(occurrence.getStatus()) || "skipped".equals(occurrence.getStatus())) {
            throw new IllegalStateException("Impossible de modifier cette occurrence.");
        }

        occurrence.setOccurrenceTime(newTime);
        occurrence.setUpdatedAt(Instant.now());
        occurrenceRepository.save(occurrence);

        notificationJobService.cancelPendingJobsForOccurrence(occurrenceId);

        var override = overrideRepository
                .findByTaskRuleIdAndOccurrenceDateAndTaskTimeSlotId(
                        occurrence.getTaskRuleId(), occurrenceDate, occurrence.getTaskTimeSlotId())
                .orElseGet(() -> {
                    var o = new TaskOverrideEntity();
                    o.setUserId(user.getId());
                    o.setTaskDefinitionId(occurrence.getTaskDefinitionId());
                    o.setTaskRuleId(occurrence.getTaskRuleId());
                    o.setOccurrenceDate(occurrenceDate);
                    o.setTaskTimeSlotId(occurrence.getTaskTimeSlotId());
                    o.setOverrideAction("MODIFY");
                    o.setCreatedAt(Instant.now());
                    return o;
                });
        override.setTimeMode("FIXED");
        override.setFixedTime(newTime);
        override.setUpdatedAt(Instant.now());
        overrideRepository.save(override);

        TodayResponse response = todayService.getForDate(email, occurrenceDate);
        try { realtimeSyncService.publish(email, "TODAY"); } catch (Exception ignored) {}
        return response;
    }

    @Transactional
    public TodayResponse revertToPlanned(String email, UUID occurrenceId) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        var occurrence = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new IllegalArgumentException("Occurrence introuvable."));

        if (!occurrence.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Occurrence introuvable.");
        }

        String previousStatus = occurrence.getStatus();
        if (!"done".equals(previousStatus) && !"missed".equals(previousStatus)) {
            throw new IllegalStateException("Seules les occurrences terminees ou manquees peuvent etre remises a faire.");
        }

        occurrence.setStatus("planned");
        occurrence.setUpdatedAt(Instant.now());
        occurrenceRepository.save(occurrence);

        var event = new OccurrenceStatusEventEntity();
        event.setOccurrenceId(occurrenceId);
        event.setUserId(user.getId());
        event.setPreviousStatus(previousStatus);
        event.setNewStatus("planned");
        eventRepository.save(event);

        TodayResponse response = todayService.getForDate(email, occurrence.getOccurrenceDate());
        try { realtimeSyncService.publish(email, "TODAY"); } catch (Exception ignored) {}
        return response;
    }

    @Transactional
    public TodayResponse reschedule(String email, UUID occurrenceId, LocalDate newDate, LocalTime newTime) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        var occurrence = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new IllegalArgumentException("Occurrence introuvable."));

        if (!occurrence.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Occurrence introuvable.");
        }

        if (!"planned".equals(occurrence.getStatus())) {
            throw new IllegalStateException("Seules les occurrences a faire peuvent etre decalees.");
        }

        TaskRuleEntity rule = ruleRepository.findById(occurrence.getTaskRuleId())
                .orElseThrow(() -> new IllegalStateException("Regle introuvable."));
        if (rule.getRecurrenceType() != null) {
            throw new IllegalStateException("Seules les taches ponctuelles peuvent etre decalees en date.");
        }

        LocalDate accountCreationDate = user.getCreatedAt() != null
                ? user.getCreatedAt().atZone(TaskService.PARIS).toLocalDate()
                : LocalDate.now(TaskService.PARIS);
        if (newDate.isBefore(accountCreationDate)) {
            throw new IllegalArgumentException("La nouvelle date ne peut pas etre anterieure a la creation du compte.");
        }

        String newDayCategory = dayClassificationReadService.classifyDate(
                newDate, user.getId(), user.getGeographicZone());

        occurrence.setOccurrenceDate(newDate);
        occurrence.setOccurrenceTime(newTime);
        occurrence.setDayCategory(newDayCategory);
        occurrence.setUpdatedAt(Instant.now());
        occurrenceRepository.save(occurrence);

        notificationJobService.cancelPendingJobsForOccurrence(occurrenceId);

        TodayResponse response = todayService.getForDate(email, newDate);
        try { realtimeSyncService.publish(email, "TODAY"); } catch (Exception ignored) {}
        return response;
    }
}
