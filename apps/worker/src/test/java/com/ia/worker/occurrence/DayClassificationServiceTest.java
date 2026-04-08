package com.ia.worker.occurrence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DayClassificationServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    DayClassificationService service;

    @Test
    void saturdayIsWeekendHoliday() {
        LocalDate saturday = LocalDate.of(2026, 3, 28);
        assertThat(service.classifyDate(saturday, UUID.randomUUID(), "METROPOLE"))
                .isEqualTo("WEEKEND_HOLIDAY");
    }

    @Test
    void sundayIsWeekendHoliday() {
        LocalDate sunday = LocalDate.of(2026, 3, 29);
        assertThat(service.classifyDate(sunday, UUID.randomUUID(), "METROPOLE"))
                .isEqualTo("WEEKEND_HOLIDAY");
    }

    @Test
    void publicHolidayOnWeekdayIsWeekendHoliday() {
        LocalDate labourDay = LocalDate.of(2026, 5, 1); // Friday — 1er mai
        UUID userId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(
                eq("SELECT count(*) FROM holidays WHERE geographic_zone = ? AND holiday_date = ?"),
                eq(Integer.class), any(), any()
        )).thenReturn(1);

        assertThat(service.classifyDate(labourDay, userId, "METROPOLE"))
                .isEqualTo("WEEKEND_HOLIDAY");
    }

    @Test
    void weekdayInVacationPeriodIsVacation() {
        LocalDate wednesday = LocalDate.of(2026, 7, 15);
        UUID userId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(
                eq("SELECT count(*) FROM holidays WHERE geographic_zone = ? AND holiday_date = ?"),
                eq(Integer.class), any(), any()
        )).thenReturn(0);
        when(jdbcTemplate.queryForObject(
                eq("SELECT count(*) FROM vacation_periods WHERE user_id = ? AND start_date <= ? AND end_date >= ?"),
                eq(Integer.class), any(), any(), any()
        )).thenReturn(1);

        assertThat(service.classifyDate(wednesday, userId, "METROPOLE"))
                .isEqualTo("VACATION");
    }

    @Test
    void regularWeekdayIsWorkday() {
        LocalDate monday = LocalDate.of(2026, 3, 23);
        UUID userId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(
                eq("SELECT count(*) FROM holidays WHERE geographic_zone = ? AND holiday_date = ?"),
                eq(Integer.class), any(), any()
        )).thenReturn(0);
        when(jdbcTemplate.queryForObject(
                eq("SELECT count(*) FROM vacation_periods WHERE user_id = ? AND start_date <= ? AND end_date >= ?"),
                eq(Integer.class), any(), any(), any()
        )).thenReturn(0);

        assertThat(service.classifyDate(monday, userId, "METROPOLE"))
                .isEqualTo("WORKDAY");
    }
}
