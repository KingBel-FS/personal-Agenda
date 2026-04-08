package com.ia.api.stats.service;

import com.ia.api.auth.domain.AccountStatus;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.stats.api.StatsDashboardResponse;
import com.ia.api.stats.api.StatsTaskDetailResponse;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.repository.OccurrenceAggregateProjection;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.repository.TaskStatsProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TaskDefinitionRepository taskDefinitionRepository;
    @Mock private TaskOccurrenceRepository taskOccurrenceRepository;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(userRepository, taskDefinitionRepository, taskOccurrenceRepository);
    }

    @Test
    void buildsDashboardWithDailyWeeklyMonthlyAndYearlyPeriods() {
        UserEntity user = activeUser();
        UUID taskId = UUID.randomUUID();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.aggregateByUserIdAndOccurrenceDateBetween(eq(user.getId()), any(), any(), eq("canceled")))
                .thenReturn(aggregate(12, 9, 1, 1, 1, 4));
        when(taskOccurrenceRepository.aggregateTaskStatsByUserIdAndOccurrenceDateBetween(eq(user.getId()), any(), any(), eq("canceled")))
                .thenReturn(List.of(taskStats(taskId, "Lecture", "📚", 4, 3, 1, 0, 0)));

        StatsDashboardResponse response = statsService.getDashboard("alice@example.com", null, null, null, null);

        assertThat(response.daily().label()).isEqualTo("Quotidien");
        assertThat(response.weekly().label()).isEqualTo("Hebdomadaire");
        assertThat(response.monthly().taskBreakdown()).hasSize(1);
        assertThat(response.yearly().history()).isNotEmpty();
    }

    @Test
    void returnsZeroForPeriodsBeforeAccountCreation() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        StatsDashboardResponse response = statsService.getDashboard(
                "alice@example.com",
                LocalDate.now().minusYears(2),
                LocalDate.now().minusYears(2),
                LocalDate.now().minusYears(2),
                LocalDate.now().minusYears(2)
        );

        assertThat(response.daily().current().totalCount()).isZero();
        assertThat(response.weekly().current().doneCount()).isZero();
        assertThat(response.monthly().current().completionRate()).isZero();
        assertThat(response.yearly().current().taskCount()).isZero();
    }

    @Test
    void buildsTaskDrillDown() {
        UserEntity user = activeUser();
        UUID taskId = UUID.randomUUID();
        TaskDefinitionEntity definition = new TaskDefinitionEntity();
        definition.setId(taskId);
        definition.setUserId(user.getId());
        definition.setTitle("Lecture");
        definition.setIcon("📚");

        TaskOccurrenceEntity occurrence = new TaskOccurrenceEntity();
        occurrence.setId(UUID.randomUUID());
        occurrence.setUserId(user.getId());
        occurrence.setTaskDefinitionId(taskId);
        occurrence.setTaskRuleId(UUID.randomUUID());
        occurrence.setOccurrenceDate(LocalDate.now().minusDays(1));
        occurrence.setOccurrenceTime(LocalTime.of(8, 0));
        occurrence.setStatus("done");
        occurrence.setDayCategory("WORKDAY");
        occurrence.setCreatedAt(Instant.now());
        occurrence.setUpdatedAt(Instant.now());

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskDefinitionRepository.findById(taskId)).thenReturn(Optional.of(definition));
        when(taskOccurrenceRepository.aggregateTaskStatsByUserIdAndOccurrenceDateBetween(eq(user.getId()), any(), any(), eq("canceled")))
                .thenReturn(List.of(taskStats(taskId, "Lecture", "📚", 5, 4, 1, 0, 0)));
        when(taskOccurrenceRepository.findTop8ByUserIdAndTaskDefinitionIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateDescOccurrenceTimeDesc(
                eq(user.getId()), eq(taskId), eq("canceled"), any(), any()))
                .thenReturn(List.of(occurrence));

        StatsTaskDetailResponse response = statsService.getTaskDetail("alice@example.com", taskId, "MONTHLY", null);

        assertThat(response.title()).isEqualTo("Lecture");
        assertThat(response.current().completionRate()).isEqualTo(80);
        assertThat(response.recentOccurrences()).hasSize(1);
    }

    @Test
    void usesFullCalendarWeekForWeeklyPeriods() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskOccurrenceRepository.aggregateByUserIdAndOccurrenceDateBetween(eq(user.getId()), any(), any(), eq("canceled")))
                .thenReturn(aggregate(0, 0, 0, 0, 0, 0));
        when(taskOccurrenceRepository.aggregateTaskStatsByUserIdAndOccurrenceDateBetween(eq(user.getId()), any(), any(), eq("canceled")))
                .thenReturn(List.of());

        statsService.getDashboard("alice@example.com", null, LocalDate.of(2026, 3, 27), null, null);

        verify(taskOccurrenceRepository, atLeastOnce()).aggregateByUserIdAndOccurrenceDateBetween(
                eq(user.getId()),
                eq(LocalDate.of(2026, 3, 23)),
                eq(LocalDate.of(2026, 3, 29)),
                eq("canceled")
        );
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setPseudo("alice");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(Instant.now().minus(90, ChronoUnit.DAYS));
        return user;
    }

    private OccurrenceAggregateProjection aggregate(long total, long done, long missed, long skipped, long planned, long tasks) {
        return new OccurrenceAggregateProjection() {
            @Override public long getTotalCount() { return total; }
            @Override public long getDoneCount() { return done; }
            @Override public long getMissedCount() { return missed; }
            @Override public long getSkippedCount() { return skipped; }
            @Override public long getPlannedCount() { return planned; }
            @Override public long getDistinctTaskCount() { return tasks; }
        };
    }

    private TaskStatsProjection taskStats(UUID taskId, String title, String icon, long total, long done, long missed, long skipped, long planned) {
        return new TaskStatsProjection() {
            @Override public UUID getTaskDefinitionId() { return taskId; }
            @Override public String getTitle() { return title; }
            @Override public String getIcon() { return icon; }
            @Override public long getTotalCount() { return total; }
            @Override public long getDoneCount() { return done; }
            @Override public long getMissedCount() { return missed; }
            @Override public long getSkippedCount() { return skipped; }
            @Override public long getPlannedCount() { return planned; }
        };
    }
}
