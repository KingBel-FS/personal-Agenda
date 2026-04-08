package com.ia.api.stats.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.stats.api.StatsDashboardResponse;
import com.ia.api.stats.api.StatsDeltaResponse;
import com.ia.api.stats.api.StatsHistoryPointResponse;
import com.ia.api.stats.api.StatsPeriodResponse;
import com.ia.api.stats.api.StatsSnapshotResponse;
import com.ia.api.stats.api.StatsTaskDetailResponse;
import com.ia.api.stats.api.StatsTaskRecentOccurrenceResponse;
import com.ia.api.stats.api.StatsTaskSummaryResponse;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.repository.OccurrenceAggregateProjection;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskStatsProjection;
import com.ia.api.task.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private static final String EXCLUDED_STATUS = "canceled";

    private final UserRepository userRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;

    public StatsService(
            UserRepository userRepository,
            TaskDefinitionRepository taskDefinitionRepository,
            TaskOccurrenceRepository taskOccurrenceRepository
    ) {
        this.userRepository = userRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
    }

    public StatsDashboardResponse getDashboard(
            String email,
            LocalDate dailyAnchor,
            LocalDate weeklyAnchor,
            LocalDate monthlyAnchor,
            LocalDate yearlyAnchor
    ) {
        UserEntity user = getUser(email);
        LocalDate today = LocalDate.now(TaskService.PARIS);
        LocalDate accountCreatedDate = user.getCreatedAt().atZone(TaskService.PARIS).toLocalDate();

        return new StatsDashboardResponse(
                Instant.now().toString(),
                user.getCreatedAt().toString(),
                buildPeriodResponse(user.getId(), accountCreatedDate, normalizeAnchor(dailyAnchor, today), PeriodType.DAILY),
                buildPeriodResponse(user.getId(), accountCreatedDate, normalizeAnchor(weeklyAnchor, today), PeriodType.WEEKLY),
                buildPeriodResponse(user.getId(), accountCreatedDate, normalizeAnchor(monthlyAnchor, today), PeriodType.MONTHLY),
                buildPeriodResponse(user.getId(), accountCreatedDate, normalizeAnchor(yearlyAnchor, today), PeriodType.YEARLY)
        );
    }

    public StatsTaskDetailResponse getTaskDetail(String email, UUID taskDefinitionId, String periodTypeRaw, LocalDate anchorDate) {
        UserEntity user = getUser(email);
        TaskDefinitionEntity definition = taskDefinitionRepository.findById(taskDefinitionId)
                .filter(item -> item.getUserId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Tache introuvable pour ce drill-down."));

        LocalDate today = LocalDate.now(TaskService.PARIS);
        LocalDate accountCreatedDate = user.getCreatedAt().atZone(TaskService.PARIS).toLocalDate();
        PeriodType periodType = PeriodType.from(periodTypeRaw);
        LocalDate normalizedAnchor = normalizeAnchor(anchorDate, today);

        DateRange currentDisplayRange = periodType.currentRange(normalizedAnchor);
        DateRange previousDisplayRange = periodType.previousRange(currentDisplayRange);
        DateRange currentQueryRange = clampForQuery(currentDisplayRange, accountCreatedDate);
        DateRange previousQueryRange = clampForQuery(previousDisplayRange, accountCreatedDate);

        StatsSnapshotResponse current = toTaskSnapshot(user.getId(), taskDefinitionId, currentDisplayRange, currentQueryRange);
        StatsSnapshotResponse previous = toTaskSnapshot(user.getId(), taskDefinitionId, previousDisplayRange, previousQueryRange);

        List<StatsTaskRecentOccurrenceResponse> recentOccurrences = isEmpty(currentQueryRange)
                ? List.of()
                : taskOccurrenceRepository
                .findTop8ByUserIdAndTaskDefinitionIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateDescOccurrenceTimeDesc(
                        user.getId(),
                        taskDefinitionId,
                        EXCLUDED_STATUS,
                        currentQueryRange.start(),
                        currentQueryRange.end()
                )
                .stream()
                .map(occurrence -> new StatsTaskRecentOccurrenceResponse(
                        occurrence.getOccurrenceDate().toString(),
                        occurrence.getOccurrenceTime().toString(),
                        occurrence.getStatus(),
                        occurrence.getDayCategory()
                ))
                .toList();

        return new StatsTaskDetailResponse(
                taskDefinitionId,
                definition.getTitle(),
                definition.getIcon(),
                periodType.name(),
                periodType.label(),
                current,
                previous,
                new StatsDeltaResponse(
                        current.totalCount() - previous.totalCount(),
                        current.doneCount() - previous.doneCount(),
                        current.completionRate() - previous.completionRate()
                ),
                recentOccurrences
        );
    }

    private StatsPeriodResponse buildPeriodResponse(
            UUID userId,
            LocalDate accountCreatedDate,
            LocalDate anchorDate,
            PeriodType periodType
    ) {
        DateRange currentDisplayRange = periodType.currentRange(anchorDate);
        DateRange previousDisplayRange = periodType.previousRange(currentDisplayRange);
        DateRange currentQueryRange = clampForQuery(currentDisplayRange, accountCreatedDate);
        DateRange previousQueryRange = clampForQuery(previousDisplayRange, accountCreatedDate);

        StatsSnapshotResponse current = toSnapshot(aggregateRange(userId, currentQueryRange), currentDisplayRange);
        StatsSnapshotResponse previous = toSnapshot(aggregateRange(userId, previousQueryRange), previousDisplayRange);

        Map<UUID, TaskStatsProjection> previousTasks = aggregateTaskRange(userId, previousQueryRange)
                .stream()
                .collect(Collectors.toMap(TaskStatsProjection::getTaskDefinitionId, Function.identity()));

        List<StatsTaskSummaryResponse> taskBreakdown = aggregateTaskRange(userId, currentQueryRange)
                .stream()
                .map(projection -> toTaskSummary(projection, previousTasks.get(projection.getTaskDefinitionId())))
                .sorted(Comparator.comparingLong(StatsTaskSummaryResponse::doneCount).reversed()
                        .thenComparingInt(StatsTaskSummaryResponse::completionRate).reversed()
                        .thenComparing(StatsTaskSummaryResponse::title, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new StatsPeriodResponse(
                periodType.name(),
                periodType.label(),
                current,
                previous,
                new StatsDeltaResponse(
                        current.totalCount() - previous.totalCount(),
                        current.doneCount() - previous.doneCount(),
                        current.completionRate() - previous.completionRate()
                ),
                periodType.comparisonLabel(),
                taskBreakdown,
                buildHistory(userId, accountCreatedDate, anchorDate, periodType)
        );
    }

    private List<StatsHistoryPointResponse> buildHistory(
            UUID userId,
            LocalDate accountCreatedDate,
            LocalDate anchorDate,
            PeriodType periodType
    ) {
        return periodType.historyRanges(anchorDate).stream()
                .map(displayRange -> {
                    DateRange queryRange = clampForQuery(displayRange, accountCreatedDate);
                    StatsSnapshotResponse snapshot = toSnapshot(aggregateRange(userId, queryRange), displayRange);
                    return new StatsHistoryPointResponse(
                            periodType.historyLabel(displayRange),
                            displayRange.start().toString(),
                            displayRange.end().toString(),
                            snapshot.totalCount(),
                            snapshot.doneCount(),
                            snapshot.missedCount(),
                            snapshot.skippedCount(),
                            snapshot.plannedCount(),
                            snapshot.completionRate()
                    );
                })
                .toList();
    }

    private StatsTaskSummaryResponse toTaskSummary(TaskStatsProjection current, TaskStatsProjection previous) {
        int currentCompletion = completionRate(current.getDoneCount(), current.getTotalCount());
        int previousCompletion = previous == null ? 0 : completionRate(previous.getDoneCount(), previous.getTotalCount());
        long previousDoneCount = previous == null ? 0 : previous.getDoneCount();

        return new StatsTaskSummaryResponse(
                current.getTaskDefinitionId(),
                current.getTitle(),
                current.getIcon(),
                current.getTotalCount(),
                current.getDoneCount(),
                current.getMissedCount(),
                current.getSkippedCount(),
                current.getPlannedCount(),
                currentCompletion,
                current.getDoneCount() - previousDoneCount,
                currentCompletion - previousCompletion
        );
    }

    private StatsSnapshotResponse toTaskSnapshot(UUID userId, UUID taskDefinitionId, DateRange displayRange, DateRange queryRange) {
        TaskStatsProjection projection = aggregateTaskRange(userId, queryRange)
                .stream()
                .filter(item -> item.getTaskDefinitionId().equals(taskDefinitionId))
                .findFirst()
                .orElse(null);

        if (projection == null) {
            return emptySnapshot(displayRange);
        }

        return new StatsSnapshotResponse(
                displayRange.start().toString(),
                displayRange.end().toString(),
                projection.getTotalCount(),
                projection.getDoneCount(),
                projection.getMissedCount(),
                projection.getSkippedCount(),
                projection.getPlannedCount(),
                1,
                completionRate(projection.getDoneCount(), projection.getTotalCount())
        );
    }

    private StatsSnapshotResponse toSnapshot(OccurrenceAggregateProjection projection, DateRange displayRange) {
        if (projection == null) {
            return emptySnapshot(displayRange);
        }

        long totalCount = projection.getTotalCount();
        long doneCount = projection.getDoneCount();
        long missedCount = projection.getMissedCount();
        long skippedCount = projection.getSkippedCount();
        long plannedCount = projection.getPlannedCount();
        long taskCount = projection.getDistinctTaskCount();

        return new StatsSnapshotResponse(
                displayRange.start().toString(),
                displayRange.end().toString(),
                totalCount,
                doneCount,
                missedCount,
                skippedCount,
                plannedCount,
                taskCount,
                completionRate(doneCount, totalCount)
        );
    }

    private StatsSnapshotResponse emptySnapshot(DateRange displayRange) {
        return new StatsSnapshotResponse(
                displayRange.start().toString(),
                displayRange.end().toString(),
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }

    private OccurrenceAggregateProjection aggregateRange(UUID userId, DateRange queryRange) {
        if (isEmpty(queryRange)) {
            return null;
        }
        return taskOccurrenceRepository.aggregateByUserIdAndOccurrenceDateBetween(
                userId,
                queryRange.start(),
                queryRange.end(),
                EXCLUDED_STATUS
        );
    }

    private List<TaskStatsProjection> aggregateTaskRange(UUID userId, DateRange queryRange) {
        if (isEmpty(queryRange)) {
            return List.of();
        }
        return taskOccurrenceRepository.aggregateTaskStatsByUserIdAndOccurrenceDateBetween(
                userId,
                queryRange.start(),
                queryRange.end(),
                EXCLUDED_STATUS
        );
    }

    private DateRange clampForQuery(DateRange displayRange, LocalDate accountCreatedDate) {
        if (displayRange.end().isBefore(accountCreatedDate)) {
            return new DateRange(accountCreatedDate, accountCreatedDate.minusDays(1));
        }
        LocalDate start = displayRange.start().isBefore(accountCreatedDate) ? accountCreatedDate : displayRange.start();
        return new DateRange(start, displayRange.end());
    }

    private boolean isEmpty(DateRange range) {
        return range.end().isBefore(range.start());
    }

    private int completionRate(long doneCount, long totalCount) {
        if (totalCount <= 0) {
            return 0;
        }
        return (int) Math.min((doneCount * 100) / totalCount, 100);
    }

    private UserEntity getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
    }

    private LocalDate normalizeAnchor(LocalDate anchorDate, LocalDate today) {
        if (anchorDate == null || anchorDate.isAfter(today)) {
            return today;
        }
        return anchorDate;
    }

    private record DateRange(LocalDate start, LocalDate end) {}

    private enum PeriodType {
        DAILY("Quotidien", "Cette journee vs la precedente") {
            @Override
            DateRange currentRange(LocalDate anchorDate) {
                return new DateRange(anchorDate, anchorDate);
            }

            @Override
            List<DateRange> historyRanges(LocalDate anchorDate) {
                return backwardRanges(anchorDate, 13, 1);
            }

            @Override
            String historyLabel(DateRange range) {
                return formatFr(range.start());
            }
        },
        WEEKLY("Hebdomadaire", "Cette semaine vs la precedente") {
            @Override
            DateRange currentRange(LocalDate anchorDate) {
                LocalDate start = anchorDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                return new DateRange(start, start.plusDays(6));
            }

            @Override
            List<DateRange> historyRanges(LocalDate anchorDate) {
                return backwardRanges(currentRange(anchorDate).start(), 7, 7);
            }

            @Override
            String historyLabel(DateRange range) {
                return formatShort(range.start());
            }
        },
        MONTHLY("Mensuel", "Ce mois vs le mois precedent") {
            @Override
            DateRange currentRange(LocalDate anchorDate) {
                LocalDate start = anchorDate.withDayOfMonth(1);
                return new DateRange(start, anchorDate.with(TemporalAdjusters.lastDayOfMonth()));
            }

            @Override
            DateRange previousRange(DateRange currentRange) {
                LocalDate previousMonth = currentRange.start().minusMonths(1);
                return new DateRange(previousMonth.withDayOfMonth(1), previousMonth.with(TemporalAdjusters.lastDayOfMonth()));
            }

            @Override
            List<DateRange> historyRanges(LocalDate anchorDate) {
                LocalDate start = anchorDate.withDayOfMonth(1);
                return java.util.stream.IntStream.rangeClosed(0, 11)
                        .mapToObj(index -> {
                            LocalDate current = start.minusMonths(11L - index);
                            return new DateRange(current.withDayOfMonth(1), current.with(TemporalAdjusters.lastDayOfMonth()));
                        })
                        .toList();
            }

            @Override
            String historyLabel(DateRange range) {
                return monthLabel(range.start());
            }
        },
        YEARLY("Annuel", "Cette annee vs l'annee precedente") {
            @Override
            DateRange currentRange(LocalDate anchorDate) {
                LocalDate start = anchorDate.withDayOfYear(1);
                return new DateRange(start, anchorDate.with(TemporalAdjusters.lastDayOfYear()));
            }

            @Override
            DateRange previousRange(DateRange currentRange) {
                LocalDate previousYear = currentRange.start().minusYears(1);
                return new DateRange(previousYear.withDayOfYear(1), previousYear.with(TemporalAdjusters.lastDayOfYear()));
            }

            @Override
            List<DateRange> historyRanges(LocalDate anchorDate) {
                LocalDate start = anchorDate.withDayOfYear(1);
                return java.util.stream.IntStream.rangeClosed(0, 4)
                        .mapToObj(index -> {
                            LocalDate current = start.minusYears(4L - index);
                            return new DateRange(current.withDayOfYear(1), current.with(TemporalAdjusters.lastDayOfYear()));
                        })
                        .toList();
            }

            @Override
            String historyLabel(DateRange range) {
                return String.valueOf(range.start().getYear());
            }
        };

        private final String label;
        private final String comparisonLabel;

        PeriodType(String label, String comparisonLabel) {
            this.label = label;
            this.comparisonLabel = comparisonLabel;
        }

        abstract DateRange currentRange(LocalDate anchorDate);

        DateRange previousRange(DateRange currentRange) {
            long spanDays = java.time.temporal.ChronoUnit.DAYS.between(currentRange.start(), currentRange.end()) + 1;
            LocalDate end = currentRange.start().minusDays(1);
            LocalDate start = end.minusDays(spanDays - 1);
            return new DateRange(start, end);
        }

        List<DateRange> historyRanges(LocalDate anchorDate) {
            return List.of(currentRange(anchorDate));
        }

        String historyLabel(DateRange range) {
            return formatFr(range.start());
        }

        String label() {
            return label;
        }

        String comparisonLabel() {
            return comparisonLabel;
        }

        static PeriodType from(String raw) {
            String normalized = raw == null ? "" : raw.trim().toUpperCase();
            return switch (normalized) {
                case "DAILY" -> DAILY;
                case "WEEKLY" -> WEEKLY;
                case "MONTHLY" -> MONTHLY;
                case "YEARLY", "ANNUAL" -> YEARLY;
                default -> throw new IllegalArgumentException("Periode de statistiques invalide.");
            };
        }

        private static List<DateRange> backwardRanges(LocalDate anchorDate, int periodsBack, int stepDays) {
            return java.util.stream.IntStream.rangeClosed(0, periodsBack)
                    .mapToObj(index -> {
                        LocalDate start = anchorDate.minusDays((long) (periodsBack - index) * stepDays);
                        return new DateRange(start, start.plusDays(stepDays - 1L));
                    })
                    .toList();
        }

        private static String formatFr(LocalDate date) {
            return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        }

        private static String formatShort(LocalDate date) {
            return String.format("%02d/%02d", date.getDayOfMonth(), date.getMonthValue());
        }

        private static String monthLabel(LocalDate date) {
            return switch (date.getMonthValue()) {
                case 1 -> "Jan";
                case 2 -> "Fev";
                case 3 -> "Mar";
                case 4 -> "Avr";
                case 5 -> "Mai";
                case 6 -> "Juin";
                case 7 -> "Juil";
                case 8 -> "Aou";
                case 9 -> "Sep";
                case 10 -> "Oct";
                case 11 -> "Nov";
                default -> "Dec";
            };
        }
    }
}
