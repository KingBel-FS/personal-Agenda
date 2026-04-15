package com.ia.api.wakeup.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.notification.service.NotificationJobService;
import com.ia.api.sync.service.RealtimeSyncService;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskRuleRepository;
import com.ia.api.task.repository.TaskTimeSlotRepository;
import com.ia.api.user.domain.DayCategory;
import com.ia.api.user.domain.DayProfileEntity;
import com.ia.api.user.repository.DayProfileRepository;
import com.ia.api.wakeup.api.WakeUpOverrideResponse;
import com.ia.api.wakeup.domain.WakeUpOverrideEntity;
import com.ia.api.wakeup.repository.WakeUpOverrideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WakeUpOverrideService {

    private final WakeUpOverrideRepository wakeUpOverrideRepository;
    private final UserRepository userRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final TaskRuleRepository taskRuleRepository;
    private final TaskTimeSlotRepository taskTimeSlotRepository;
    private final DayProfileRepository dayProfileRepository;
    private final NotificationJobService notificationJobService;
    private final RealtimeSyncService realtimeSyncService;

    public WakeUpOverrideService(
            WakeUpOverrideRepository wakeUpOverrideRepository,
            UserRepository userRepository,
            TaskOccurrenceRepository taskOccurrenceRepository,
            TaskRuleRepository taskRuleRepository,
            TaskTimeSlotRepository taskTimeSlotRepository,
            DayProfileRepository dayProfileRepository,
            NotificationJobService notificationJobService,
            RealtimeSyncService realtimeSyncService
    ) {
        this.wakeUpOverrideRepository = wakeUpOverrideRepository;
        this.userRepository = userRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
        this.taskRuleRepository = taskRuleRepository;
        this.taskTimeSlotRepository = taskTimeSlotRepository;
        this.dayProfileRepository = dayProfileRepository;
        this.notificationJobService = notificationJobService;
        this.realtimeSyncService = realtimeSyncService;
    }

    public Optional<WakeUpOverrideResponse> getOverride(String email, LocalDate date) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return wakeUpOverrideRepository.findByUserIdAndOverrideDate(user.getId(), date)
                .map(e -> new WakeUpOverrideResponse(e.getOverrideDate().toString(), e.getWakeUpTime().toString()));
    }

    @Transactional
    public WakeUpOverrideResponse upsertOverride(String email, LocalDate date, LocalTime wakeUpTime) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        validateDateAllowed(date, user.getTimezoneName());

        WakeUpOverrideEntity entity = wakeUpOverrideRepository
                .findByUserIdAndOverrideDate(user.getId(), date)
                .orElseGet(() -> {
                    WakeUpOverrideEntity e = new WakeUpOverrideEntity();
                    e.setId(UUID.randomUUID());
                    e.setUserId(user.getId());
                    e.setOverrideDate(date);
                    e.setCreatedAt(Instant.now());
                    return e;
                });
        entity.setWakeUpTime(wakeUpTime);
        wakeUpOverrideRepository.save(entity);

        List<UUID> affectedIds = recalculateOccurrences(user.getId(), date, wakeUpTime);
        affectedIds.forEach(notificationJobService::cancelPendingJobsForOccurrence);
        try { realtimeSyncService.publish(email, "TODAY"); } catch (Exception ignored) {}

        return new WakeUpOverrideResponse(date.toString(), wakeUpTime.toString());
    }

    @Transactional
    public void deleteOverride(String email, LocalDate date) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        validateDateAllowed(date, user.getTimezoneName());

        wakeUpOverrideRepository.deleteByUserIdAndOverrideDate(user.getId(), date);

        Map<DayCategory, LocalTime> profileWakeUpTimes = dayProfileRepository.findAllByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap(DayProfileEntity::getDayCategory, DayProfileEntity::getWakeUpTime));

        List<TaskOccurrenceEntity> occurrences = taskOccurrenceRepository
                .findAllByUserIdAndOccurrenceDateAndStatusNot(user.getId(), date, "canceled");

        List<TaskOccurrenceEntity> toSave = new ArrayList<>();
        for (TaskOccurrenceEntity occ : occurrences) {
            TaskRuleEntity rule = taskRuleRepository.findById(occ.getTaskRuleId()).orElse(null);
            if (rule == null || !"WAKE_UP_OFFSET".equals(rule.getTimeMode())) continue;
            DayCategory category = DayCategory.valueOf(occ.getDayCategory());
            LocalTime wakeUp = profileWakeUpTimes.getOrDefault(category, LocalTime.of(7, 0));
            int offset = rule.getWakeUpOffsetMinutes() != null ? rule.getWakeUpOffsetMinutes() : 0;
            occ.setOccurrenceTime(wakeUp.plusMinutes(offset));
            occ.setUpdatedAt(Instant.now());
            toSave.add(occ);
        }
        taskOccurrenceRepository.saveAll(toSave);
        toSave.forEach(occ -> notificationJobService.cancelPendingJobsForOccurrence(occ.getId()));
        try { realtimeSyncService.publish(email, "TODAY"); } catch (Exception ignored) {}
    }

    private List<UUID> recalculateOccurrences(UUID userId, LocalDate date, LocalTime wakeUpTime) {
        List<TaskOccurrenceEntity> occurrences = taskOccurrenceRepository
                .findAllByUserIdAndOccurrenceDateAndStatusNot(userId, date, "canceled");

        // Group by ruleId to handle multi-slot tasks
        Map<UUID, List<TaskOccurrenceEntity>> byRule = occurrences.stream()
                .collect(java.util.stream.Collectors.groupingBy(TaskOccurrenceEntity::getTaskRuleId));

        List<TaskOccurrenceEntity> toSave = new ArrayList<>();
        for (var entry : byRule.entrySet()) {
            TaskRuleEntity rule = taskRuleRepository.findById(entry.getKey()).orElse(null);
            if (rule == null || !"WAKE_UP_OFFSET".equals(rule.getTimeMode())) continue;

            int baseOffset = rule.getWakeUpOffsetMinutes() != null ? rule.getWakeUpOffsetMinutes() : 0;
            LocalTime baseTime = wakeUpTime.plusMinutes(baseOffset);

            // Sort occurrences by current time to preserve order
            List<TaskOccurrenceEntity> ruleOccs = entry.getValue().stream()
                    .sorted(java.util.Comparator.comparing(TaskOccurrenceEntity::getOccurrenceTime))
                    .toList();

            // First occurrence = base time from rule offset
            LocalTime previousTime = baseTime;
            for (int i = 0; i < ruleOccs.size(); i++) {
                TaskOccurrenceEntity occ = ruleOccs.get(i);
                if (i == 0) {
                    occ.setOccurrenceTime(baseTime);
                } else {
                    // Multi-slot: keep the same interval from the previous occurrence
                    // by looking at the slot's afterPreviousMinutes or keeping fixed slots unchanged
                    UUID slotId = occ.getTaskTimeSlotId();
                    if (slotId != null) {
                        var slot = taskTimeSlotRepository.findById(slotId).orElse(null);
                        if (slot != null && "EVERY_N_MINUTES".equals(slot.getTimeMode())) {
                            int interval = slot.getAfterPreviousMinutes() != null ? slot.getAfterPreviousMinutes() : 60;
                            occ.setOccurrenceTime(previousTime.plusMinutes(interval));
                        } else if (slot != null && "FIXED".equals(slot.getTimeMode()) && slot.getFixedTime() != null) {
                            occ.setOccurrenceTime(slot.getFixedTime());
                        } else {
                            occ.setOccurrenceTime(baseTime);
                        }
                    } else {
                        occ.setOccurrenceTime(baseTime);
                    }
                }
                previousTime = occ.getOccurrenceTime();
                occ.setUpdatedAt(Instant.now());
                toSave.add(occ);
            }
        }
        taskOccurrenceRepository.saveAll(toSave);
        return toSave.stream().map(TaskOccurrenceEntity::getId).toList();
    }

    private void validateDateAllowed(LocalDate date, String timezoneName) {
        ZoneId zone = ZoneId.of(timezoneName);
        LocalDate today = LocalDate.now(zone);
        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Impossible de modifier le réveil d'un jour passé.");
        }
        // today and future dates are always allowed — no 4AM cutoff
    }
}
