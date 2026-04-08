package com.ia.api.agenda;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.service.DayClassificationReadService;
import com.ia.api.task.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgendaService {

    private final UserRepository userRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final DayClassificationReadService dayClassificationReadService;

    public AgendaService(
            UserRepository userRepository,
            TaskOccurrenceRepository taskOccurrenceRepository,
            TaskDefinitionRepository taskDefinitionRepository,
            DayClassificationReadService dayClassificationReadService
    ) {
        this.userRepository = userRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.dayClassificationReadService = dayClassificationReadService;
    }

    public AgendaRangeResponse getWeek(String email, LocalDate anchorDate) {
        LocalDate effectiveAnchor = anchorDate != null ? anchorDate : LocalDate.now(TaskService.PARIS);
        LocalDate rangeStart = effectiveAnchor.with(DayOfWeek.MONDAY);
        LocalDate rangeEnd = rangeStart.plusDays(6);
        return buildRange("week", email, effectiveAnchor, rangeStart, rangeEnd, false);
    }

    public AgendaRangeResponse getMonth(String email, LocalDate anchorDate) {
        LocalDate effectiveAnchor = anchorDate != null ? anchorDate : LocalDate.now(TaskService.PARIS);
        LocalDate firstOfMonth = effectiveAnchor.withDayOfMonth(1);
        LocalDate lastOfMonth = effectiveAnchor.withDayOfMonth(effectiveAnchor.lengthOfMonth());
        LocalDate rangeStart = firstOfMonth.with(DayOfWeek.MONDAY);
        LocalDate rangeEnd = lastOfMonth.with(DayOfWeek.SUNDAY);
        return buildRange("month", email, effectiveAnchor, rangeStart, rangeEnd, true);
    }

    private AgendaRangeResponse buildRange(
            String view,
            String email,
            LocalDate anchorDate,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            boolean markCurrentMonth
    ) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        LocalDate today = LocalDate.now(TaskService.PARIS);
        LocalDate accountCreationDate = user.getCreatedAt() != null
                ? user.getCreatedAt().atZone(TaskService.PARIS).toLocalDate()
                : LocalDate.MIN;
        List<TaskOccurrenceEntity> occurrences = taskOccurrenceRepository
                .findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                        user.getId(), "canceled", rangeStart, rangeEnd);

        Map<LocalDate, List<TaskOccurrenceEntity>> occurrencesByDate = occurrences.stream()
                .collect(Collectors.groupingBy(TaskOccurrenceEntity::getOccurrenceDate));

        Set<UUID> definitionIds = occurrences.stream()
                .map(TaskOccurrenceEntity::getTaskDefinitionId)
                .collect(Collectors.toSet());

        Map<UUID, TaskDefinitionEntity> definitions = definitionIds.isEmpty()
                ? Map.of()
                : taskDefinitionRepository.findAllById(definitionIds).stream()
                .collect(Collectors.toMap(TaskDefinitionEntity::getId, definition -> definition));

        Map<String, String> classificationCache = new HashMap<>();
        List<AgendaDaySummary> days = new ArrayList<>();

        for (LocalDate date = rangeStart; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            List<TaskOccurrenceEntity> dayOccurrences = occurrencesByDate.getOrDefault(date, List.of());
            boolean beforeAccountCreation = date.isBefore(accountCreationDate);
            String dayCategory = dayClassificationReadService.classifyDate(
                    date,
                    user.getId(),
                    user.getGeographicZone(),
                    classificationCache
            );

            int totalCount = dayOccurrences.size();
            int plannedCount = countByStatus(dayOccurrences, "planned");
            int doneCount = countByStatus(dayOccurrences, "done");
            int missedCount = countByStatus(dayOccurrences, "missed");
            int skippedCount = countByStatus(dayOccurrences, "skipped");

            List<String> icons = dayOccurrences.stream()
                    .map(occurrence -> definitions.get(occurrence.getTaskDefinitionId()))
                    .filter(definition -> definition != null && definition.getIcon() != null && !definition.getIcon().isBlank())
                    .map(TaskDefinitionEntity::getIcon)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new),
                            list -> list.stream().limit(3).toList()
                    ));

            days.add(new AgendaDaySummary(
                    date,
                    !markCurrentMonth || date.getMonth().equals(anchorDate.getMonth()),
                    date.isBefore(today),
                    date.equals(today),
                    beforeAccountCreation,
                    dayCategory,
                    totalCount,
                    plannedCount,
                    doneCount,
                    missedCount,
                    skippedCount,
                    resolveStatusTone(totalCount, plannedCount, doneCount, missedCount, skippedCount),
                    icons
            ));
        }

        return new AgendaRangeResponse(view, anchorDate, rangeStart, rangeEnd, days);
    }

    private int countByStatus(List<TaskOccurrenceEntity> occurrences, String status) {
        return (int) occurrences.stream()
                .filter(occurrence -> status.equalsIgnoreCase(occurrence.getStatus()))
                .count();
    }

    private String resolveStatusTone(int totalCount, int plannedCount, int doneCount, int missedCount, int skippedCount) {
        if (totalCount == 0) {
            return "empty";
        }
        if (doneCount == totalCount) {
            return "done";
        }
        if (missedCount > 0 && plannedCount == 0 && doneCount == 0) {
            return "missed";
        }
        if (plannedCount == totalCount) {
            return "planned";
        }
        if (skippedCount == totalCount) {
            return "skipped";
        }
        return "mixed";
    }
}
