package com.ia.worker.occurrence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OccurrenceMaterializationServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    DayClassificationService dayClassificationService;

    private OccurrenceMaterializationService service;

    @BeforeEach
    void setUp() {
        service = new OccurrenceMaterializationService(jdbcTemplate, dayClassificationService, 30);
    }

    private OccurrenceMaterializationService.TaskRuleRow oneTimeRule(LocalDate start) {
        return new OccurrenceMaterializationService.TaskRuleRow(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "METROPOLE",
                List.of("WORKDAY"), start, "FIXED", LocalTime.of(7, 30),
                null, null, List.of(), null, null
        );
    }

    private OccurrenceMaterializationService.TaskRuleRow weeklyRule(LocalDate start, List<Integer> daysOfWeek) {
        return new OccurrenceMaterializationService.TaskRuleRow(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "METROPOLE",
                List.of("WORKDAY"), start, "FIXED", LocalTime.of(8, 0),
                null, "WEEKLY", daysOfWeek, null, null
        );
    }

    private OccurrenceMaterializationService.TaskRuleRow monthlyRule(LocalDate start, int dayOfMonth) {
        return new OccurrenceMaterializationService.TaskRuleRow(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "METROPOLE",
                List.of("WORKDAY"), start, "FIXED", LocalTime.of(9, 0),
                null, "MONTHLY", List.of(), dayOfMonth, null
        );
    }

    @Test
    void oneTimeRuleInHorizonGeneratesSingleOccurrence() {
        LocalDate today = LocalDate.of(2026, 3, 25);
        LocalDate horizon = today.plusDays(30);
        var rule = oneTimeRule(LocalDate.of(2026, 4, 1));

        assertThat(service.computeOccurrenceDates(rule, today, horizon))
                .containsExactly(LocalDate.of(2026, 4, 1));
    }

    @Test
    void oneTimeRuleBeforeTodayGeneratesNoOccurrence() {
        LocalDate today = LocalDate.of(2026, 3, 25);
        LocalDate horizon = today.plusDays(30);
        var rule = oneTimeRule(LocalDate.of(2026, 3, 20));

        assertThat(service.computeOccurrenceDates(rule, today, horizon)).isEmpty();
    }

    @Test
    void weeklyRuleGeneratesMatchingWeekdays() {
        // today = Tuesday 24-Mar, horizon = Tuesday 31-Mar
        // rule starts Monday 23-Mar, repeats Mon(1) and Wed(3)
        // Expected in window [24-Mar, 31-Mar]: Wed 25-Mar, Mon 30-Mar
        LocalDate today = LocalDate.of(2026, 3, 24);
        LocalDate horizon = LocalDate.of(2026, 3, 31);
        var rule = weeklyRule(LocalDate.of(2026, 3, 23), List.of(1, 3));

        assertThat(service.computeOccurrenceDates(rule, today, horizon))
                .containsExactly(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 30));
    }

    @Test
    void weeklyRuleRespectsEndDate() {
        LocalDate today = LocalDate.of(2026, 3, 23);
        LocalDate horizon = today.plusDays(30);
        var ruleWithEnd = new OccurrenceMaterializationService.TaskRuleRow(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "METROPOLE",
                List.of("WORKDAY"), today, "FIXED", LocalTime.of(8, 0),
                null, "WEEKLY", List.of(1), null, LocalDate.of(2026, 3, 30)
        );

        List<LocalDate> dates = service.computeOccurrenceDates(ruleWithEnd, today, horizon);

        // Mondays between 23-Mar and 30-Mar inclusive: 23, 30
        assertThat(dates).containsExactly(LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 30));
    }

    @Test
    void monthlyRuleGeneratesOnePerMonth() {
        LocalDate today = LocalDate.of(2026, 3, 1);
        LocalDate horizon = LocalDate.of(2026, 5, 31);
        var rule = monthlyRule(today, 15);

        assertThat(service.computeOccurrenceDates(rule, today, horizon))
                .containsExactly(
                        LocalDate.of(2026, 3, 15),
                        LocalDate.of(2026, 4, 15),
                        LocalDate.of(2026, 5, 15)
                );
    }

    @Test
    void monthlyRuleCapsToLastDayOfMonth() {
        // Day 31 in February → must be capped to 28
        LocalDate today = LocalDate.of(2026, 2, 1);
        LocalDate horizon = LocalDate.of(2026, 2, 28);
        var rule = monthlyRule(today, 31);

        assertThat(service.computeOccurrenceDates(rule, today, horizon))
                .containsExactly(LocalDate.of(2026, 2, 28));
    }
}
