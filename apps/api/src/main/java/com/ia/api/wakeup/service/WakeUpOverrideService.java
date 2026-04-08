package com.ia.api.wakeup.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.sync.service.RealtimeSyncService;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskRuleRepository;
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
    private final DayProfileRepository dayProfileRepository;
    private final RealtimeSyncService realtimeSyncService;

    public WakeUpOverrideService(
            WakeUpOverrideRepository wakeUpOverrideRepository,
            UserRepository userRepository,
            TaskOccurrenceRepository taskOccurrenceRepository,
            TaskRuleRepository taskRuleRepository,
            DayProfileRepository dayProfileRepository,
            RealtimeSyncService realtimeSyncService
    ) {
        this.wakeUpOverrideRepository = wakeUpOverrideRepository;
        this.userRepository = userRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
        this.taskRuleRepository = taskRuleRepository;
        this.dayProfileRepository = dayProfileRepository;
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

        recalculateOccurrences(user.getId(), date, wakeUpTime);
        realtimeSyncService.publish(email, "TODAY");

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
        realtimeSyncService.publish(email, "TODAY");
    }

    private void recalculateOccurrences(UUID userId, LocalDate date, LocalTime wakeUpTime) {
        List<TaskOccurrenceEntity> occurrences = taskOccurrenceRepository
                .findAllByUserIdAndOccurrenceDateAndStatusNot(userId, date, "canceled");

        List<TaskOccurrenceEntity> toSave = new ArrayList<>();
        for (TaskOccurrenceEntity occ : occurrences) {
            TaskRuleEntity rule = taskRuleRepository.findById(occ.getTaskRuleId()).orElse(null);
            if (rule == null || !"WAKE_UP_OFFSET".equals(rule.getTimeMode())) continue;
            int offset = rule.getWakeUpOffsetMinutes() != null ? rule.getWakeUpOffsetMinutes() : 0;
            occ.setOccurrenceTime(wakeUpTime.plusMinutes(offset));
            occ.setUpdatedAt(Instant.now());
            toSave.add(occ);
        }
        taskOccurrenceRepository.saveAll(toSave);
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
