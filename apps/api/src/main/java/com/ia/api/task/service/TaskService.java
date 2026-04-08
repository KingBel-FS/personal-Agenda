package com.ia.api.task.service;

import com.ia.api.auth.api.MessageResponse;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.task.api.CreateTaskRequest;
import com.ia.api.task.api.DeleteTaskOccurrenceRequest;
import com.ia.api.task.api.TaskMutationScope;
import com.ia.api.task.api.TaskOccurrenceListRequest;
import com.ia.api.task.api.TaskOccurrencePageResponse;
import com.ia.api.task.api.TaskOccurrenceResponse;
import com.ia.api.task.api.TaskPreviewRequest;
import com.ia.api.task.api.TaskPreviewResponse;
import com.ia.api.task.api.TaskResponse;
import com.ia.api.task.api.UpdateTaskOccurrenceRequest;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.domain.TaskOverrideEntity;
import com.ia.api.task.domain.TaskRuleEntity;
import com.ia.api.task.api.TimeSlotRequest;
import com.ia.api.task.api.TimeSlotSummary;
import com.ia.api.task.domain.TaskTimeSlotEntity;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskOverrideRepository;
import com.ia.api.task.repository.TaskRuleRepository;
import com.ia.api.task.repository.TaskTimeSlotRepository;
import com.ia.api.sync.service.RealtimeSyncService;
import com.ia.api.user.domain.AssetEntity;
import com.ia.api.user.domain.DayCategory;
import com.ia.api.user.domain.DayProfileEntity;
import com.ia.api.user.repository.AssetRepository;
import com.ia.api.user.repository.DayProfileRepository;
import com.ia.api.user.service.ProfilePhotoStorageService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TaskService {

    public static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskRuleRepository taskRuleRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final TaskOverrideRepository taskOverrideRepository;
    private final TaskTimeSlotRepository taskTimeSlotRepository;
    private final UserRepository userRepository;
    private final DayProfileRepository dayProfileRepository;
    private final AssetRepository assetRepository;
    private final ProfilePhotoStorageService photoStorageService;
    private final OccurrenceRefreshService occurrenceRefreshService;
    private final RealtimeSyncService realtimeSyncService;

    public TaskService(
            TaskDefinitionRepository taskDefinitionRepository,
            TaskRuleRepository taskRuleRepository,
            TaskOccurrenceRepository taskOccurrenceRepository,
            TaskOverrideRepository taskOverrideRepository,
            TaskTimeSlotRepository taskTimeSlotRepository,
            UserRepository userRepository,
            DayProfileRepository dayProfileRepository,
            AssetRepository assetRepository,
            ProfilePhotoStorageService photoStorageService,
            OccurrenceRefreshService occurrenceRefreshService,
            RealtimeSyncService realtimeSyncService
    ) {
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskRuleRepository = taskRuleRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
        this.taskOverrideRepository = taskOverrideRepository;
        this.taskTimeSlotRepository = taskTimeSlotRepository;
        this.userRepository = userRepository;
        this.dayProfileRepository = dayProfileRepository;
        this.assetRepository = assetRepository;
        this.photoStorageService = photoStorageService;
        this.occurrenceRefreshService = occurrenceRefreshService;
        this.realtimeSyncService = realtimeSyncService;
    }

    @Transactional
    public TaskResponse createTask(String email, CreateTaskRequest request) throws IOException {
        UserEntity user = getUser(email);

        LocalDate today = LocalDate.now(PARIS);
        if (request.getStartDate().isBefore(today)) {
            throw new IllegalArgumentException("La date de debut ne peut pas etre dans le passe.");
        }
        guardNotBeforeAccountCreation(user, request.getStartDate());

        boolean isRecurring = request.getRecurrenceType() != null;
        String taskType = isRecurring ? "RECURRING" : "ONE_TIME";

        TaskDefinitionEntity definition = new TaskDefinitionEntity();
        definition.setId(UUID.randomUUID());
        definition.setUserId(user.getId());
        definition.setTitle(request.getTitle().trim());
        definition.setIcon(request.getIcon().trim());
        definition.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        definition.setTaskType(taskType);
        definition.setCreatedAt(Instant.now());
        definition.setUpdatedAt(Instant.now());
        taskDefinitionRepository.save(definition);

        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            ProfilePhotoStorageService.StoredPhoto stored = photoStorageService.store(definition.getId(), request.getPhoto());
            AssetEntity asset = new AssetEntity();
            asset.setId(UUID.randomUUID());
            asset.setUserId(user.getId());
            asset.setAssetType("TASK_PHOTO");
            asset.setStorageKey(stored.storageKey());
            asset.setCreatedAt(Instant.now());
            assetRepository.save(asset);
            definition.setPhotoAssetId(asset.getId());
        }

        TaskRuleEntity rule = buildRule(definition.getId(), request);
        taskRuleRepository.save(rule);

        if (request.getTimeSlots() != null && !request.getTimeSlots().isEmpty()) {
            List<TaskTimeSlotEntity> slots = new ArrayList<>();
            int order = 1;
            for (TimeSlotRequest slotReq : request.getTimeSlots()) {
                TaskTimeSlotEntity slot = new TaskTimeSlotEntity();
                slot.setTaskRuleId(rule.getId());
                slot.setSlotOrder(order++);
                slot.setTimeMode(slotReq.getTimeMode());
                slot.setFixedTime(slotReq.getFixedTime());
                slot.setWakeUpOffsetMinutes(slotReq.getWakeUpOffsetMinutes());
                slot.setAfterPreviousMinutes(slotReq.getAfterPreviousMinutes());
                slot.setCreatedAt(Instant.now());
                slots.add(slot);
            }
            taskTimeSlotRepository.saveAll(slots);
        }

        occurrenceRefreshService.refreshFutureOccurrences(rule, user.getId(), user.getGeographicZone(), request.getStartDate());

        TaskResponse response = toResponse(definition, rule, null);
        publishSync(email, "TASKS");
        return response;
    }

    public TaskPreviewResponse previewNextOccurrence(String email, TaskPreviewRequest request) {
        UserEntity user = getUser(email);

        LocalDate today = LocalDate.now(PARIS);
        if (request.startDate().isBefore(today)) {
            throw new IllegalArgumentException("La date de debut ne peut pas etre dans le passe.");
        }
        guardNotBeforeAccountCreation(user, request.startDate());

        LocalDate occurrenceDate = computeNextOccurrenceDate(request);
        LocalTime occurrenceTime = computeOccurrenceTime(user.getId(), request);
        String label = buildOccurrenceLabel(occurrenceDate, occurrenceTime);

        return new TaskPreviewResponse(occurrenceDate, occurrenceTime, label);
    }

    @Transactional
    public TaskOccurrencePageResponse listOccurrences(String email, TaskOccurrenceListRequest request) {
        UserEntity user = getUser(email);
        LocalDate today = LocalDate.now(PARIS);

        int page = request.getPage() == null || request.getPage() < 0 ? 0 : request.getPage();
        int size = normalizePageSize(request.getSize());

        // P-7 : filtrage date côté DB — évite le full-scan mémoire.
        LocalDate effectiveFrom = request.getOccurrenceDateFrom() != null
                ? request.getOccurrenceDateFrom()
                : today;
        LocalDate effectiveTo = request.getOccurrenceDateTo();

        List<TaskOccurrenceEntity> occurrences;
        if (effectiveTo != null) {
            occurrences = taskOccurrenceRepository
                    .findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                            user.getId(), "canceled", effectiveFrom, effectiveTo);
        } else {
            occurrences = taskOccurrenceRepository
                    .findAllByUserIdAndStatusNotAndOccurrenceDateGreaterThanEqualOrderByOccurrenceDateAscOccurrenceTimeAsc(
                            user.getId(), "canceled", effectiveFrom);
        }

        if (occurrences.isEmpty()) {
            return new TaskOccurrencePageResponse(List.of(), page, size, 0, 0);
        }

        Map<UUID, TaskDefinitionEntity> definitions = taskDefinitionRepository
                .findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .collect(Collectors.toMap(TaskDefinitionEntity::getId, d -> d));

        List<UUID> ruleIds = occurrences.stream()
                .map(TaskOccurrenceEntity::getTaskRuleId)
                .distinct()
                .toList();

        Map<UUID, TaskRuleEntity> rules = taskRuleRepository.findAllById(ruleIds).stream()
                .collect(Collectors.toMap(TaskRuleEntity::getId, r -> r));

        Map<String, TaskOverrideEntity> overrides = taskOverrideRepository
                .findAllByTaskRuleIdIn(ruleIds).stream()
                // P-12 : merge function pour éviter IllegalStateException sur doublon
                .collect(Collectors.toMap(
                        o -> o.getTaskRuleId() + "::" + o.getOccurrenceDate(),
                        o -> o,
                        (existing, duplicate) -> existing
                ));

        // Slots batch — one query for all rules in this page
        Map<UUID, List<TaskTimeSlotEntity>> slotsByRule = taskTimeSlotRepository.findAllByTaskRuleIdIn(ruleIds).stream()
                .collect(Collectors.groupingBy(TaskTimeSlotEntity::getTaskRuleId));

        // P-6 : batch query futureScopeAvailable — une requête au lieu de N.
        Map<UUID, LocalDate> maxOccurrenceDatePerRule = buildMaxDatePerRule(ruleIds);

        // Intra-day slot order : computed dynamically from occurrences grouped by (ruleId, date)
        // Works for both FIXED slots and EVERY_N_MINUTES (which can generate N occurrences per slot).
        Map<UUID, Integer> occSlotOrders = new HashMap<>();
        Map<UUID, Integer> occTotalSlots = new HashMap<>();
        occurrences.stream()
                .collect(Collectors.groupingBy(o -> o.getTaskRuleId() + "::" + o.getOccurrenceDate()))
                .forEach((key, group) -> {
                    if (group.size() > 1) {
                        group.sort(Comparator.comparing(TaskOccurrenceEntity::getOccurrenceTime));
                        for (int i = 0; i < group.size(); i++) {
                            occSlotOrders.put(group.get(i).getId(), i + 1);
                            occTotalSlots.put(group.get(i).getId(), group.size());
                        }
                    }
                });

        List<TaskOccurrenceResponse> filtered = occurrences.stream()
                .map(o -> toOccurrenceResponse(o, definitions, rules, overrides, today, maxOccurrenceDatePerRule, occSlotOrders, occTotalSlots, slotsByRule))
                .filter(Objects::nonNull)
                .filter(r -> matchesFilters(r, request, effectiveFrom))
                .sorted(Comparator
                        .comparing(TaskOccurrenceResponse::occurrenceDate)
                        .thenComparing(TaskOccurrenceResponse::occurrenceTime))
                .toList();

        int totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        return new TaskOccurrencePageResponse(
                filtered.subList(fromIndex, toIndex),
                page,
                size,
                totalElements,
                totalPages
        );
    }

    @Transactional
    public TaskOccurrenceResponse updateOccurrence(String email, UUID occurrenceId, UpdateTaskOccurrenceRequest request) {
        UserEntity user = getUser(email);
        validateTimeMode(request.timeMode(), request.fixedTime(), request.wakeUpOffsetMinutes());

        TaskOccurrenceEntity occurrence = findOwnedOccurrence(user.getId(), occurrenceId);
        guardNotPast(occurrence.getOccurrenceDate());

        TaskDefinitionEntity definition = findOwnedDefinition(user.getId(), occurrence.getTaskDefinitionId());
        // P-1 : vérification ownership de la règle via la définition
        TaskRuleEntity rule = taskRuleRepository.findById(occurrence.getTaskRuleId())
                .filter(r -> r.getTaskDefinitionId().equals(definition.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Regle de tache introuvable."));

        if (request.scope() == TaskMutationScope.THIS_OCCURRENCE) {

            // ── 3-way split : slots pour cette occurrence uniquement ──────────
            if (request.timeSlots() != null) {
                LocalDate date = occurrence.getOccurrenceDate();
                LocalDate originalEndDate = rule.getEndDate();
                boolean startsBeforeDate = rule.getStartDate().isBefore(date);

                // Trim ou avance la règle originale pour exclure ce jour
                if (startsBeforeDate) {
                    rule.setEndDate(date.minusDays(1));
                } else {
                    // règle qui démarre exactement ce jour
                    if (originalEndDate != null && !originalEndDate.isAfter(date)) {
                        rule.setEndDate(date.minusDays(1)); // règle 1 jour → désactiver
                    } else {
                        rule.setStartDate(date.plusDays(1));
                    }
                }
                taskRuleRepository.save(rule);

                // Définition + règle patch (1 jour)
                TaskDefinitionEntity patchDef = cloneDefinition(definition, request.title(), request.icon(), request.description());
                taskDefinitionRepository.save(patchDef);

                TaskRuleEntity patchRule = cloneRule(rule, patchDef.getId());
                patchRule.setId(UUID.randomUUID());
                patchRule.setStartDate(date);
                patchRule.setEndDate(date);
                patchRule.setTimeMode(request.timeMode());
                patchRule.setFixedTime("FIXED".equals(request.timeMode()) ? request.fixedTime() : null);
                patchRule.setWakeUpOffsetMinutes("WAKE_UP_OFFSET".equals(request.timeMode()) ? request.wakeUpOffsetMinutes() : null);
                patchRule.setCreatedAt(Instant.now());
                taskRuleRepository.save(patchRule);

                if (!request.timeSlots().isEmpty()) {
                    List<TaskTimeSlotEntity> patchSlots = new ArrayList<>();
                    int order = 1;
                    for (TimeSlotRequest slotReq : request.timeSlots()) {
                        TaskTimeSlotEntity s = new TaskTimeSlotEntity();
                        s.setTaskRuleId(patchRule.getId());
                        s.setSlotOrder(order++);
                        s.setTimeMode(slotReq.getTimeMode());
                        s.setFixedTime(slotReq.getFixedTime());
                        s.setAfterPreviousMinutes(slotReq.getAfterPreviousMinutes());
                        s.setCreatedAt(Instant.now());
                        patchSlots.add(s);
                    }
                    taskTimeSlotRepository.saveAll(patchSlots);
                }

                if (startsBeforeDate) {
                    deleteFutureOccurrences(rule.getId(), date);
                    if (originalEndDate == null || originalEndDate.isAfter(date)) {
                        TaskRuleEntity tailRule = cloneRule(rule, definition.getId());
                        tailRule.setId(UUID.randomUUID());
                        tailRule.setStartDate(date.plusDays(1));
                        tailRule.setEndDate(originalEndDate);
                        tailRule.setCreatedAt(Instant.now());
                        taskRuleRepository.save(tailRule);

                        List<TaskTimeSlotEntity> srcSlots = taskTimeSlotRepository.findAllByTaskRuleIdOrderBySlotOrderAsc(rule.getId());
                        if (!srcSlots.isEmpty()) {
                            List<TaskTimeSlotEntity> tailSlots = new ArrayList<>();
                            for (TaskTimeSlotEntity src : srcSlots) {
                                TaskTimeSlotEntity s = new TaskTimeSlotEntity();
                                s.setTaskRuleId(tailRule.getId());
                                s.setSlotOrder(src.getSlotOrder());
                                s.setTimeMode(src.getTimeMode());
                                s.setFixedTime(src.getFixedTime());
                                s.setAfterPreviousMinutes(src.getAfterPreviousMinutes());
                                s.setCreatedAt(Instant.now());
                                tailSlots.add(s);
                            }
                            taskTimeSlotRepository.saveAll(tailSlots);
                        }
                        occurrenceRefreshService.refreshFutureOccurrences(tailRule, user.getId(), user.getGeographicZone(), date.plusDays(1));
                    }
                } else {
                    List<TaskOccurrenceEntity> staleDateOccs = taskOccurrenceRepository.findAllByTaskRuleIdAndOccurrenceDate(rule.getId(), date);
                    taskOccurrenceRepository.deleteAll(staleDateOccs);
                }

                occurrenceRefreshService.refreshFutureOccurrences(patchRule, user.getId(), user.getGeographicZone(), date);

                TaskOccurrenceEntity firstPatch = taskOccurrenceRepository
                        .findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(patchRule.getId(), date)
                        .stream()
                        .filter(o -> !"skipped".equals(o.getStatus()))
                        .findFirst()
                        .orElseGet(() -> taskOccurrenceRepository
                                .findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(patchRule.getId(), date)
                                .stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Aucune occurrence generee pour le creneau du jour.")));

                TaskDefinitionEntity patchMappedDef = taskDefinitionRepository.findById(firstPatch.getTaskDefinitionId()).orElse(patchDef);
                TaskRuleEntity patchMappedRule = taskRuleRepository.findById(firstPatch.getTaskRuleId()).orElse(patchRule);

                TaskOccurrenceResponse response = toOccurrenceResponse(
                        firstPatch,
                        Map.of(patchMappedDef.getId(), patchMappedDef),
                        Map.of(patchMappedRule.getId(), patchMappedRule),
                        Map.of(),
                        LocalDate.now(PARIS),
                        buildMaxDatePerRule(List.of(patchMappedRule.getId())),
                        Map.of(),
                        Map.of(),
                        Map.of()
                );
                publishSync(email, "TASKS");
                return response;
            }

            UUID slotId = occurrence.getTaskTimeSlotId();
            TaskOverrideEntity override = (slotId != null
                    ? taskOverrideRepository.findByTaskRuleIdAndOccurrenceDateAndTaskTimeSlotId(rule.getId(), occurrence.getOccurrenceDate(), slotId)
                    : taskOverrideRepository.findByTaskRuleIdAndOccurrenceDate(rule.getId(), occurrence.getOccurrenceDate()))
                    .orElseGet(TaskOverrideEntity::new);
            if (override.getId() == null) {
                override.setId(UUID.randomUUID());
                override.setUserId(user.getId());
                override.setTaskDefinitionId(definition.getId());
                override.setTaskRuleId(rule.getId());
                override.setOccurrenceDate(occurrence.getOccurrenceDate());
                override.setTaskTimeSlotId(slotId);
                override.setCreatedAt(Instant.now());
            }
            override.setOverrideAction("EDIT");
            override.setTitle(request.title().trim());
            override.setIcon(request.icon().trim());
            override.setDescription(blankToNull(request.description()));
            override.setTimeMode(request.timeMode());
            // P-2 : ne stocker fixedTime dans l'override QUE si le mode est FIXED.
            // En mode WAKE_UP_OFFSET, on laisse null pour que le refresh recalcule
            // l'heure depuis le profil de réveil.
            override.setFixedTime("FIXED".equals(request.timeMode()) ? request.fixedTime() : null);
            override.setWakeUpOffsetMinutes("WAKE_UP_OFFSET".equals(request.timeMode()) ? request.wakeUpOffsetMinutes() : null);
            override.setOverrideStatus(occurrence.getStatus());
            override.setUpdatedAt(Instant.now());
            taskOverrideRepository.save(override);

            occurrence.setOccurrenceTime(resolveOccurrenceTime(user.getId(), request.timeMode(), request.fixedTime(), request.wakeUpOffsetMinutes(), occurrence.getDayCategory()));
            occurrence.setUpdatedAt(Instant.now());
            taskOccurrenceRepository.save(occurrence);

            TaskOccurrenceResponse response = toOccurrenceResponse(
                    occurrence,
                    Map.of(definition.getId(), definition),
                    Map.of(rule.getId(), rule),
                    Map.of(rule.getId() + "::" + occurrence.getOccurrenceDate(), override),
                    LocalDate.now(PARIS),
                    buildMaxDatePerRule(List.of(rule.getId())),
                    Map.of(),
                    Map.of(),
                    Map.of()
            );
            publishSync(email, "TASKS");
            return response;
        } else {
            if (!isRecurring(rule)) {
                throw new IllegalArgumentException("Le scope futur requiert une tache recurrente.");
            }
            // P-4 : dayCategories obligatoire pour THIS_AND_FOLLOWING (guard explicite)
            if (request.dayCategories() == null || request.dayCategories().isEmpty()) {
                throw new IllegalArgumentException("Selectionne au moins un type de jour pour la serie.");
            }
            validateRecurringRuleUpdate(occurrence.getOccurrenceDate(), request);

            TaskDefinitionEntity futureDefinition = cloneDefinition(definition, request.title(), request.icon(), request.description());
            taskDefinitionRepository.save(futureDefinition);

            rule.setEndDate(occurrence.getOccurrenceDate().minusDays(1));
            taskRuleRepository.save(rule);

            TaskRuleEntity futureRule = cloneRule(rule, futureDefinition.getId());
            futureRule.setId(UUID.randomUUID());
            futureRule.setStartDate(occurrence.getOccurrenceDate());
            futureRule.setTimeMode(request.timeMode());
            futureRule.setFixedTime("FIXED".equals(request.timeMode()) ? request.fixedTime() : null);
            futureRule.setWakeUpOffsetMinutes("WAKE_UP_OFFSET".equals(request.timeMode()) ? request.wakeUpOffsetMinutes() : null);
            futureRule.setDayCategories(String.join(",", request.dayCategories()));
            futureRule.setRecurrenceType(request.recurrenceType());
            futureRule.setDaysOfWeek(request.daysOfWeek() == null ? null
                    : request.daysOfWeek().stream().map(String::valueOf).collect(Collectors.joining(",")));
            futureRule.setDayOfMonth(request.dayOfMonth());
            futureRule.setEndDate(request.endDate());
            futureRule.setEndTime(request.endTime());
            futureRule.setCreatedAt(Instant.now());
            taskRuleRepository.save(futureRule);

            // Slots for the new rule: use provided slots or copy from original rule
            if (request.timeSlots() != null) {
                if (!request.timeSlots().isEmpty()) {
                    List<TaskTimeSlotEntity> newSlots = new ArrayList<>();
                    int order = 1;
                    for (TimeSlotRequest slotReq : request.timeSlots()) {
                        TaskTimeSlotEntity slot = new TaskTimeSlotEntity();
                        slot.setTaskRuleId(futureRule.getId());
                        slot.setSlotOrder(order++);
                        slot.setTimeMode(slotReq.getTimeMode());
                        slot.setFixedTime(slotReq.getFixedTime());
                        slot.setAfterPreviousMinutes(slotReq.getAfterPreviousMinutes());
                        slot.setCreatedAt(Instant.now());
                        newSlots.add(slot);
                    }
                    taskTimeSlotRepository.saveAll(newSlots);
                }
                // empty list → no slots for future rule (user cleared them)
            } else {
                // null → copy slots from original rule unchanged
                List<TaskTimeSlotEntity> sourceSlots = taskTimeSlotRepository.findAllByTaskRuleIdOrderBySlotOrderAsc(rule.getId());
                if (!sourceSlots.isEmpty()) {
                    List<TaskTimeSlotEntity> copiedSlots = new ArrayList<>();
                    for (TaskTimeSlotEntity src : sourceSlots) {
                        TaskTimeSlotEntity slot = new TaskTimeSlotEntity();
                        slot.setTaskRuleId(futureRule.getId());
                        slot.setSlotOrder(src.getSlotOrder());
                        slot.setTimeMode(src.getTimeMode());
                        slot.setFixedTime(src.getFixedTime());
                        slot.setAfterPreviousMinutes(src.getAfterPreviousMinutes());
                        slot.setCreatedAt(Instant.now());
                        copiedSlots.add(slot);
                    }
                    taskTimeSlotRepository.saveAll(copiedSlots);
                }
            }

            deleteFutureOccurrences(rule.getId(), occurrence.getOccurrenceDate());
            occurrenceRefreshService.refreshFutureOccurrences(futureRule, user.getId(), user.getGeographicZone(), occurrence.getOccurrenceDate());

            // P-9 : chercher la première occurrence non-skipped après le refresh
            TaskOccurrenceEntity firstFutureOccurrence = taskOccurrenceRepository
                    .findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(futureRule.getId(), occurrence.getOccurrenceDate())
                    .stream()
                    .filter(o -> !"skipped".equals(o.getStatus()))
                    .findFirst()
                    .orElseGet(() ->
                        // Fallback : prendre la première quelle que soit le statut
                        taskOccurrenceRepository
                                .findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(futureRule.getId(), occurrence.getOccurrenceDate())
                                .stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Aucune occurrence future n'a ete regeneree."))
                    );

            TaskDefinitionEntity mappedDefinition = taskDefinitionRepository.findById(firstFutureOccurrence.getTaskDefinitionId())
                    .orElse(futureDefinition);
            TaskRuleEntity mappedRule = taskRuleRepository.findById(firstFutureOccurrence.getTaskRuleId())
                    .orElse(futureRule);

            TaskOccurrenceResponse response = toOccurrenceResponse(
                    firstFutureOccurrence,
                    Map.of(firstFutureOccurrence.getTaskDefinitionId(), mappedDefinition),
                    Map.of(firstFutureOccurrence.getTaskRuleId(), mappedRule),
                    Map.of(),
                    LocalDate.now(PARIS),
                    buildMaxDatePerRule(List.of(mappedRule.getId())),
                    Map.of(),
                    Map.of(),
                    Map.of()
            );
            publishSync(email, "TASKS");
            return response;
        }
    }

    @Transactional
    public TaskOccurrenceResponse updateOccurrencePhoto(String email, UUID occurrenceId, TaskMutationScope scope, MultipartFile photo) throws IOException {
        UserEntity user = getUser(email);
        TaskOccurrenceEntity occurrence = findOwnedOccurrence(user.getId(), occurrenceId);
        guardNotPast(occurrence.getOccurrenceDate());

        TaskDefinitionEntity definition = findOwnedDefinition(user.getId(), occurrence.getTaskDefinitionId());
        TaskRuleEntity rule = taskRuleRepository.findById(occurrence.getTaskRuleId())
                .filter(r -> r.getTaskDefinitionId().equals(definition.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Regle de tache introuvable."));

        if (scope == TaskMutationScope.THIS_OCCURRENCE && isRecurring(rule)) {
            LocalDate date = occurrence.getOccurrenceDate();
            LocalDate originalEndDate = rule.getEndDate();
            boolean startsBeforeDate = rule.getStartDate().isBefore(date);

            if (startsBeforeDate) {
                rule.setEndDate(date.minusDays(1));
            } else if (originalEndDate != null && !originalEndDate.isAfter(date)) {
                rule.setEndDate(date.minusDays(1));
            } else {
                rule.setStartDate(date.plusDays(1));
            }
            taskRuleRepository.save(rule);

            TaskDefinitionEntity patchDefinition = cloneDefinition(definition, definition.getTitle(), definition.getIcon(), definition.getDescription());
            taskDefinitionRepository.save(patchDefinition);
            applyTaskPhoto(user, patchDefinition, photo);

            TaskRuleEntity patchRule = cloneRule(rule, patchDefinition.getId());
            patchRule.setId(UUID.randomUUID());
            patchRule.setStartDate(date);
            patchRule.setEndDate(date);
            patchRule.setCreatedAt(Instant.now());
            taskRuleRepository.save(patchRule);
            copyRuleSlots(rule.getId(), patchRule.getId());

            if (startsBeforeDate) {
                deleteFutureOccurrences(rule.getId(), date);
                if (originalEndDate == null || originalEndDate.isAfter(date)) {
                    TaskRuleEntity tailRule = cloneRule(rule, definition.getId());
                    tailRule.setId(UUID.randomUUID());
                    tailRule.setStartDate(date.plusDays(1));
                    tailRule.setEndDate(originalEndDate);
                    tailRule.setCreatedAt(Instant.now());
                    taskRuleRepository.save(tailRule);
                    copyRuleSlots(rule.getId(), tailRule.getId());
                    occurrenceRefreshService.refreshFutureOccurrences(tailRule, user.getId(), user.getGeographicZone(), date.plusDays(1));
                }
            } else {
                List<TaskOccurrenceEntity> staleDateOccs = taskOccurrenceRepository.findAllByTaskRuleIdAndOccurrenceDate(rule.getId(), date);
                taskOccurrenceRepository.deleteAll(staleDateOccs);
            }

            occurrenceRefreshService.refreshFutureOccurrences(patchRule, user.getId(), user.getGeographicZone(), date);

            TaskOccurrenceEntity firstPatch = taskOccurrenceRepository
                    .findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(patchRule.getId(), date)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Aucune occurrence generee pour cette photo."));

            TaskOccurrenceResponse response = toOccurrenceResponse(
                    firstPatch,
                    Map.of(patchDefinition.getId(), patchDefinition),
                    Map.of(patchRule.getId(), patchRule),
                    Map.of(),
                    LocalDate.now(PARIS),
                    buildMaxDatePerRule(List.of(patchRule.getId())),
                    Map.of(),
                    Map.of(),
                    Map.of()
            );
            publishSync(email, "TASKS");
            return response;
        }

        if (scope == TaskMutationScope.THIS_AND_FOLLOWING && isRecurring(rule) && rule.getStartDate().isBefore(occurrence.getOccurrenceDate())) {
            rule.setEndDate(occurrence.getOccurrenceDate().minusDays(1));
            taskRuleRepository.save(rule);

            TaskDefinitionEntity futureDefinition = cloneDefinition(definition, definition.getTitle(), definition.getIcon(), definition.getDescription());
            taskDefinitionRepository.save(futureDefinition);
            applyTaskPhoto(user, futureDefinition, photo);

            TaskRuleEntity futureRule = cloneRule(rule, futureDefinition.getId());
            futureRule.setId(UUID.randomUUID());
            futureRule.setStartDate(occurrence.getOccurrenceDate());
            futureRule.setCreatedAt(Instant.now());
            taskRuleRepository.save(futureRule);
            copyRuleSlots(rule.getId(), futureRule.getId());

            deleteFutureOccurrences(rule.getId(), occurrence.getOccurrenceDate());
            occurrenceRefreshService.refreshFutureOccurrences(futureRule, user.getId(), user.getGeographicZone(), occurrence.getOccurrenceDate());

            TaskOccurrenceEntity firstFuture = taskOccurrenceRepository
                    .findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(futureRule.getId(), occurrence.getOccurrenceDate())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Aucune occurrence future n'a ete regeneree."));

            TaskOccurrenceResponse response = toOccurrenceResponse(
                    firstFuture,
                    Map.of(futureDefinition.getId(), futureDefinition),
                    Map.of(futureRule.getId(), futureRule),
                    Map.of(),
                    LocalDate.now(PARIS),
                    buildMaxDatePerRule(List.of(futureRule.getId())),
                    Map.of(),
                    Map.of(),
                    Map.of()
            );
            publishSync(email, "TASKS");
            return response;
        }

        applyTaskPhoto(user, definition, photo);
        TaskOccurrenceResponse response = toOccurrenceResponse(
                occurrence,
                Map.of(definition.getId(), definition),
                Map.of(rule.getId(), rule),
                Map.of(),
                LocalDate.now(PARIS),
                buildMaxDatePerRule(List.of(rule.getId())),
                Map.of(),
                Map.of(),
                Map.of()
        );
        publishSync(email, "TASKS");
        return response;
    }

    @Transactional
    public MessageResponse deleteOccurrence(String email, UUID occurrenceId, DeleteTaskOccurrenceRequest request) {
        UserEntity user = getUser(email);
        TaskOccurrenceEntity occurrence = findOwnedOccurrence(user.getId(), occurrenceId);
        guardNotPast(occurrence.getOccurrenceDate());

        TaskDefinitionEntity definition = findOwnedDefinition(user.getId(), occurrence.getTaskDefinitionId());
        // P-1 : ownership de la règle vérifiée via la définition
        TaskRuleEntity rule = taskRuleRepository.findById(occurrence.getTaskRuleId())
                .filter(r -> r.getTaskDefinitionId().equals(definition.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Regle de tache introuvable."));

        if (request.scope() == TaskMutationScope.THIS_OCCURRENCE) {
            UUID slotId = occurrence.getTaskTimeSlotId();
            TaskOverrideEntity override = (slotId != null
                    ? taskOverrideRepository.findByTaskRuleIdAndOccurrenceDateAndTaskTimeSlotId(rule.getId(), occurrence.getOccurrenceDate(), slotId)
                    : taskOverrideRepository.findByTaskRuleIdAndOccurrenceDate(rule.getId(), occurrence.getOccurrenceDate()))
                    .orElseGet(TaskOverrideEntity::new);
            if (override.getId() == null) {
                override.setId(UUID.randomUUID());
                override.setUserId(user.getId());
                override.setTaskDefinitionId(occurrence.getTaskDefinitionId());
                override.setTaskRuleId(rule.getId());
                override.setOccurrenceDate(occurrence.getOccurrenceDate());
                override.setTaskTimeSlotId(slotId);
                override.setCreatedAt(Instant.now());
            }
            override.setOverrideAction("DELETE");
            override.setOverrideStatus("canceled");
            override.setUpdatedAt(Instant.now());
            taskOverrideRepository.save(override);

            occurrence.setStatus("canceled");
            occurrence.setUpdatedAt(Instant.now());
            taskOccurrenceRepository.save(occurrence);
            publishSync(email, "TASKS");
            return new MessageResponse("Occurrence unique supprimee.");
        }

        if (!isRecurring(rule)) {
            throw new IllegalArgumentException("Le scope futur requiert une tache recurrente.");
        }

        // P-10 : éviter endDate < startDate si on supprime depuis la première occurrence.
        LocalDate newEndDate = occurrence.getOccurrenceDate().minusDays(1);
        if (!newEndDate.isBefore(rule.getStartDate())) {
            rule.setEndDate(newEndDate);
        } else {
            // On supprime tout depuis le début : fermer la règle sur sa startDate.
            rule.setEndDate(rule.getStartDate());
        }
        taskRuleRepository.save(rule);

        // P-17 : purger les overrides orphelins de la série future
        taskOverrideRepository.deleteAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(
                rule.getId(), occurrence.getOccurrenceDate());
        deleteFutureOccurrences(rule.getId(), occurrence.getOccurrenceDate());

        publishSync(email, "TASKS");
        return new MessageResponse("Occurrence et suivantes supprimees.");
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private TaskOccurrenceResponse toOccurrenceResponse(
            TaskOccurrenceEntity occurrence,
            Map<UUID, TaskDefinitionEntity> definitions,
            Map<UUID, TaskRuleEntity> rules,
            Map<String, TaskOverrideEntity> overrides,
            LocalDate today,
            Map<UUID, LocalDate> maxOccurrenceDatePerRule,
            Map<UUID, Integer> occSlotOrders,
            Map<UUID, Integer> occTotalSlots,
            Map<UUID, List<TaskTimeSlotEntity>> slotsByRule
    ) {
        TaskDefinitionEntity definition = definitions.get(occurrence.getTaskDefinitionId());
        TaskRuleEntity rule = rules.get(occurrence.getTaskRuleId());

        // P-5 : ignorer l'occurrence si la définition ou la règle est introuvable
        if (definition == null || rule == null) {
            return null;
        }

        TaskOverrideEntity override = overrides.get(occurrence.getTaskRuleId() + "::" + occurrence.getOccurrenceDate());

        String title = override != null && override.getTitle() != null ? override.getTitle() : definition.getTitle();
        String icon = override != null && override.getIcon() != null ? override.getIcon() : definition.getIcon();
        String description = override != null && override.getDescription() != null ? override.getDescription() : definition.getDescription();
        LocalTime time = override != null && override.getFixedTime() != null ? override.getFixedTime() : occurrence.getOccurrenceTime();
        String timeMode = override != null && override.getTimeMode() != null ? override.getTimeMode() : rule.getTimeMode();
        LocalTime fixedTime = "FIXED".equals(timeMode)
                ? (override != null && override.getFixedTime() != null ? override.getFixedTime() : rule.getFixedTime())
                : null;
        Integer wakeUpOffsetMinutes = "WAKE_UP_OFFSET".equals(timeMode)
                ? (override != null && override.getWakeUpOffsetMinutes() != null ? override.getWakeUpOffsetMinutes() : rule.getWakeUpOffsetMinutes())
                : null;
        boolean pastLocked = occurrence.getOccurrenceDate().isBefore(today);
        boolean recurring = isRecurring(rule);
        List<String> dayCategories = Arrays.stream(rule.getDayCategories().split(","))
                .map(String::trim)  // P-13 : trim le CSV
                .toList();
        List<Integer> daysOfWeek = rule.getDaysOfWeek() == null || rule.getDaysOfWeek().isBlank()
                ? null
                : Stream.of(rule.getDaysOfWeek().split(",")).map(s -> Integer.parseInt(s.trim())).toList();

        // P-6 : futureScopeAvailable via max-date batch (plus de N+1)
        LocalDate maxDate = maxOccurrenceDatePerRule.getOrDefault(occurrence.getTaskRuleId(), LocalDate.MIN);
        boolean futureScopeAvailable = recurring && !pastLocked && maxDate.isAfter(occurrence.getOccurrenceDate());

        // Intra-day slot info — derived dynamically from occurrence grouping (handles EVERY_N_MINUTES)
        Integer slotOrder = occSlotOrders.get(occurrence.getId()); // null when only 1 per day
        int totalSlotsPerDay = occTotalSlots.getOrDefault(occurrence.getId(), 1);

        // Rule's time-slot definitions (for edit sheet population)
        List<TimeSlotSummary> timeSlots = slotsByRule.getOrDefault(rule.getId(), List.of()).stream()
                .map(s -> new TimeSlotSummary(s.getTimeMode(), s.getFixedTime(), s.getAfterPreviousMinutes()))
                .toList();

        return new TaskOccurrenceResponse(
                occurrence.getId(),
                occurrence.getTaskDefinitionId(),
                occurrence.getTaskRuleId(),
                title,
                icon,
                description,
                definition.getTaskType(),
                timeMode,
                fixedTime,
                wakeUpOffsetMinutes,
                dayCategories,
                rule.getRecurrenceType(),
                daysOfWeek,
                rule.getDayOfMonth(),
                rule.getEndDate(),
                rule.getEndTime(),
                occurrence.getOccurrenceDate(),
                time,
                occurrence.getStatus(),
                occurrence.getDayCategory(),
                pastLocked,
                recurring,
                futureScopeAvailable,
                slotOrder,
                totalSlotsPerDay,
                timeSlots
        );
    }

    /** P-6 : construit la map {ruleId → maxOccurrenceDate} en une requête batch. */
    private Map<UUID, LocalDate> buildMaxDatePerRule(List<UUID> ruleIds) {
        if (ruleIds.isEmpty()) return Map.of();
        Map<UUID, LocalDate> result = new HashMap<>();
        for (Object[] row : taskOccurrenceRepository.findMaxOccurrenceDatePerRule(ruleIds)) {
            result.put((UUID) row[0], (LocalDate) row[1]);
        }
        return result;
    }

    // ── Clone helpers ─────────────────────────────────────────────────────────

    private TaskDefinitionEntity cloneDefinition(TaskDefinitionEntity source, String title, String icon, String description) {
        TaskDefinitionEntity clone = new TaskDefinitionEntity();
        clone.setId(UUID.randomUUID());
        clone.setUserId(source.getUserId());
        clone.setTitle(title.trim());
        clone.setIcon(icon.trim());
        clone.setDescription(blankToNull(description));
        clone.setTaskType(source.getTaskType());
        clone.setPhotoAssetId(source.getPhotoAssetId());
        clone.setCreatedAt(Instant.now());
        clone.setUpdatedAt(Instant.now());
        return clone;
    }

    private void copyRuleSlots(UUID sourceRuleId, UUID targetRuleId) {
        List<TaskTimeSlotEntity> sourceSlots = taskTimeSlotRepository.findAllByTaskRuleIdOrderBySlotOrderAsc(sourceRuleId);
        if (sourceSlots.isEmpty()) {
            return;
        }
        List<TaskTimeSlotEntity> copies = new ArrayList<>();
        for (TaskTimeSlotEntity source : sourceSlots) {
            TaskTimeSlotEntity slot = new TaskTimeSlotEntity();
            slot.setTaskRuleId(targetRuleId);
            slot.setSlotOrder(source.getSlotOrder());
            slot.setTimeMode(source.getTimeMode());
            slot.setFixedTime(source.getFixedTime());
            slot.setAfterPreviousMinutes(source.getAfterPreviousMinutes());
            slot.setCreatedAt(Instant.now());
            copies.add(slot);
        }
        taskTimeSlotRepository.saveAll(copies);
    }

    private void applyTaskPhoto(UserEntity user, TaskDefinitionEntity definition, MultipartFile photo) throws IOException {
        if (photo == null || photo.isEmpty()) {
            return;
        }

        if (definition.getPhotoAssetId() != null) {
            assetRepository.findById(definition.getPhotoAssetId()).ifPresent(asset -> {
                try {
                    photoStorageService.delete(asset.getStorageKey());
                } catch (IOException ignored) {
                }
                assetRepository.delete(asset);
            });
        }

        ProfilePhotoStorageService.StoredPhoto stored = photoStorageService.store(definition.getId(), photo);
        AssetEntity asset = new AssetEntity();
        asset.setId(UUID.randomUUID());
        asset.setUserId(user.getId());
        asset.setAssetType("TASK_PHOTO");
        asset.setStorageKey(stored.storageKey());
        asset.setCreatedAt(Instant.now());
        assetRepository.save(asset);

        definition.setPhotoAssetId(asset.getId());
        definition.setUpdatedAt(Instant.now());
        taskDefinitionRepository.save(definition);
    }

    private TaskRuleEntity cloneRule(TaskRuleEntity source, UUID taskDefinitionId) {
        TaskRuleEntity clone = new TaskRuleEntity();
        clone.setTaskDefinitionId(taskDefinitionId);
        clone.setDayCategories(source.getDayCategories());
        clone.setStartDate(source.getStartDate());
        clone.setTimeMode(source.getTimeMode());
        clone.setFixedTime(source.getFixedTime());
        clone.setWakeUpOffsetMinutes(source.getWakeUpOffsetMinutes());
        clone.setRecurrenceType(source.getRecurrenceType());
        clone.setDaysOfWeek(source.getDaysOfWeek());
        clone.setDayOfMonth(source.getDayOfMonth());
        clone.setEndDate(source.getEndDate());
        clone.setEndTime(source.getEndTime());
        return clone;
    }

    private TaskRuleEntity buildRule(UUID taskDefinitionId, CreateTaskRequest request) {
        TaskRuleEntity rule = new TaskRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setTaskDefinitionId(taskDefinitionId);
        rule.setDayCategories(String.join(",", request.getDayCategories()));
        rule.setStartDate(request.getStartDate());
        rule.setTimeMode(request.getTimeMode());
        rule.setFixedTime(request.getFixedTime());
        rule.setWakeUpOffsetMinutes(request.getWakeUpOffsetMinutes());
        rule.setRecurrenceType(request.getRecurrenceType());
        if (request.getDaysOfWeek() != null) {
            rule.setDaysOfWeek(request.getDaysOfWeek().stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        rule.setDayOfMonth(request.getDayOfMonth());
        rule.setEndDate(request.getEndDate());
        rule.setEndTime(request.getEndTime());
        rule.setCreatedAt(Instant.now());
        return rule;
    }

    // ── Date / time helpers ───────────────────────────────────────────────────

    private LocalDate computeNextOccurrenceDate(TaskPreviewRequest request) {
        if ("WEEKLY".equals(request.recurrenceType()) && request.daysOfWeek() != null) {
            return nextWeeklyOccurrence(request.startDate(), request.daysOfWeek());
        }
        if ("MONTHLY".equals(request.recurrenceType()) && request.dayOfMonth() != null) {
            return nextMonthlyOccurrence(request.startDate(), request.dayOfMonth());
        }
        return request.startDate();
    }

    private LocalDate nextWeeklyOccurrence(LocalDate from, List<Integer> daysOfWeek) {
        LocalDate date = from;
        for (int i = 0; i < 7; i++) {
            if (daysOfWeek.contains(date.getDayOfWeek().getValue())) {
                return date;
            }
            date = date.plusDays(1);
        }
        return from;
    }

    private LocalDate nextMonthlyOccurrence(LocalDate from, int dayOfMonth) {
        int capped = Math.min(dayOfMonth, from.lengthOfMonth());
        LocalDate candidate = from.withDayOfMonth(capped);
        if (!candidate.isBefore(from)) {
            return candidate;
        }
        LocalDate nextMonth = from.plusMonths(1);
        return nextMonth.withDayOfMonth(Math.min(dayOfMonth, nextMonth.lengthOfMonth()));
    }

    private LocalTime computeOccurrenceTime(UUID userId, TaskPreviewRequest request) {
        if ("FIXED".equals(request.timeMode())) {
            return request.fixedTime();
        }
        String firstCategory = request.dayCategories().getFirst();
        DayCategory dayCategory = DayCategory.valueOf(firstCategory);

        Map<DayCategory, LocalTime> wakeUpTimes = dayProfileRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(DayProfileEntity::getDayCategory, DayProfileEntity::getWakeUpTime));

        LocalTime wakeUp = wakeUpTimes.getOrDefault(dayCategory, LocalTime.of(7, 0));
        int offset = request.wakeUpOffsetMinutes() != null ? request.wakeUpOffsetMinutes() : 0;
        return wakeUp.plusMinutes(offset);
    }

    private LocalTime resolveOccurrenceTime(UUID userId, String timeMode, LocalTime fixedTime, Integer wakeUpOffsetMinutes, String dayCategory) {
        if ("FIXED".equals(timeMode)) {
            return fixedTime;
        }
        Map<DayCategory, LocalTime> wakeUpTimes = dayProfileRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(DayProfileEntity::getDayCategory, DayProfileEntity::getWakeUpTime));
        LocalTime wakeUp = wakeUpTimes.getOrDefault(DayCategory.valueOf(dayCategory), LocalTime.of(7, 0));
        return wakeUp.plusMinutes(wakeUpOffsetMinutes != null ? wakeUpOffsetMinutes : 0);
    }

    private String buildOccurrenceLabel(LocalDate date, LocalTime time) {
        return String.format(
                "Le %02d/%02d/%d a %s",
                date.getDayOfMonth(),
                date.getMonthValue(),
                date.getYear(),
                time.toString().substring(0, 5)
        );
    }

    // ── Suppression / validation ──────────────────────────────────────────────

    private void deleteFutureOccurrences(UUID taskRuleId, LocalDate fromDate) {
        List<TaskOccurrenceEntity> stale = taskOccurrenceRepository
                .findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(taskRuleId, fromDate);
        taskOccurrenceRepository.deleteAll(stale);
    }

    private void validateTimeMode(String timeMode, LocalTime fixedTime, Integer wakeUpOffsetMinutes) {
        // P-14 : validation explicite des valeurs autorisées
        if (!"FIXED".equals(timeMode) && !"WAKE_UP_OFFSET".equals(timeMode)) {
            throw new IllegalArgumentException("Le mode horaire est invalide. Valeurs acceptees : FIXED, WAKE_UP_OFFSET.");
        }
        if ("FIXED".equals(timeMode) && fixedTime == null) {
            throw new IllegalArgumentException("Une heure fixe est requise.");
        }
        if ("WAKE_UP_OFFSET".equals(timeMode) && wakeUpOffsetMinutes == null) {
            throw new IllegalArgumentException("Le decalage reveil est requis.");
        }
    }

    private void validateRecurringRuleUpdate(LocalDate occurrenceDate, UpdateTaskOccurrenceRequest request) {
        if (request.dayCategories() == null || request.dayCategories().isEmpty()) {
            throw new IllegalArgumentException("Selectionne au moins un type de jour pour la serie.");
        }
        if ("WEEKLY".equals(request.recurrenceType()) && (request.daysOfWeek() == null || request.daysOfWeek().isEmpty())) {
            throw new IllegalArgumentException("Selectionne au moins un jour de la semaine pour la serie.");
        }
        if ("MONTHLY".equals(request.recurrenceType()) && request.dayOfMonth() == null) {
            throw new IllegalArgumentException("Le jour du mois est requis pour une recurrence mensuelle.");
        }
        if (request.endDate() != null && request.endDate().isBefore(occurrenceDate)) {
            throw new IllegalArgumentException("La date de fin doit etre posterieure ou egale a l'occurrence de depart.");
        }
    }

    private boolean isRecurring(TaskRuleEntity rule) {
        return rule.getRecurrenceType() != null && !rule.getRecurrenceType().isBlank();
    }

    private void guardNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now(PARIS))) {
            throw new IllegalArgumentException("Les occurrences passees ne peuvent pas etre modifiees ou supprimees.");
        }
    }

    private void guardNotBeforeAccountCreation(UserEntity user, LocalDate date) {
        if (user.getCreatedAt() == null) {
            return;
        }
        LocalDate accountCreationDate = user.getCreatedAt().atZone(PARIS).toLocalDate();
        if (date.isBefore(accountCreationDate)) {
            throw new IllegalArgumentException("La date de debut ne peut pas etre anterieure a la creation du compte.");
        }
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    private UserEntity getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private TaskOccurrenceEntity findOwnedOccurrence(UUID userId, UUID occurrenceId) {
        return taskOccurrenceRepository.findById(occurrenceId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Occurrence introuvable."));
    }

    private TaskDefinitionEntity findOwnedDefinition(UUID userId, UUID definitionId) {
        return taskDefinitionRepository.findById(definitionId)
                .filter(d -> d.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Tache introuvable."));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void publishSync(String email, String scope) {
        try { realtimeSyncService.publish(email, scope); } catch (Exception ignored) {}
    }

    private TaskResponse toResponse(TaskDefinitionEntity definition, TaskRuleEntity rule, String photoUrl) {
        List<String> dayCategories = Arrays.asList(rule.getDayCategories().split(","));
        List<Integer> daysOfWeek = rule.getDaysOfWeek() != null
                ? Stream.of(rule.getDaysOfWeek().split(",")).map(Integer::parseInt).toList()
                : null;
        return new TaskResponse(
                definition.getId(),
                definition.getTitle(),
                definition.getIcon(),
                definition.getDescription(),
                definition.getTaskType(),
                rule.getStartDate(),
                dayCategories,
                rule.getTimeMode(),
                rule.getFixedTime(),
                rule.getWakeUpOffsetMinutes(),
                rule.getRecurrenceType(),
                daysOfWeek,
                rule.getDayOfMonth(),
                rule.getEndDate(),
                rule.getEndTime(),
                photoUrl
        );
    }

    // ── Filtres ───────────────────────────────────────────────────────────────

    private boolean matchesFilters(TaskOccurrenceResponse response, TaskOccurrenceListRequest request, LocalDate effectiveFrom) {
        return matchesSearch(response, request.getSearch())
                && matchesOccurrenceDate(response, effectiveFrom, request.getOccurrenceDateTo())
                && matchesTaskKind(response, request.getTaskKind(), request.getSelectedDate())
                && matchesTimeFilter(response, request.getTimeMode(), request.getFixedTime(), request.getWakeUpOffsetMinutes());
    }

    private boolean matchesSearch(TaskOccurrenceResponse response, String search) {
        if (search == null || search.isBlank()) return true;
        return response.title().toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT));
    }

    private boolean matchesTaskKind(TaskOccurrenceResponse response, String taskKind, LocalDate selectedDate) {
        String normalized = taskKind == null ? "ALL" : taskKind.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized)) return true;
        if ("ONE_TIME".equals(normalized)) return "ONE_TIME".equalsIgnoreCase(response.taskType());
        if ("RECURRING_AFTER_DATE".equals(normalized)) {
            LocalDate boundary = selectedDate != null ? selectedDate : LocalDate.now(PARIS);
            // P-11 : les séries sans date de fin (infinies) sont toujours incluses
            return response.recurring()
                    && (response.endDate() == null || response.endDate().isAfter(boundary));
        }
        return true;
    }

    private boolean matchesOccurrenceDate(TaskOccurrenceResponse response, LocalDate effectiveFrom, LocalDate occurrenceDateTo) {
        if (response.occurrenceDate().isBefore(effectiveFrom)) return false;
        return occurrenceDateTo == null || !response.occurrenceDate().isAfter(occurrenceDateTo);
    }

    private boolean matchesTimeFilter(
            TaskOccurrenceResponse response,
            String timeMode,
            LocalTime fixedTime,
            Integer wakeUpOffsetMinutes
    ) {
        String normalized = timeMode == null ? "ALL" : timeMode.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized)) return true;
        if (!normalized.equalsIgnoreCase(response.timeMode())) return false;
        if ("FIXED".equals(normalized) && fixedTime != null) return fixedTime.equals(response.fixedTime());
        if ("WAKE_UP_OFFSET".equals(normalized) && wakeUpOffsetMinutes != null) {
            return wakeUpOffsetMinutes.equals(response.wakeUpOffsetMinutes());
        }
        return true;
    }

    private int normalizePageSize(Integer requestedSize) {
        if (requestedSize == null) return 10;
        return switch (requestedSize) {
            case 5, 10, 25, 50, 100 -> requestedSize;
            default -> 10;
        };
    }
}
