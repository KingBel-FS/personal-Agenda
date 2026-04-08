package com.ia.worker.occurrence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class DayClassificationService {

    private final JdbcTemplate jdbcTemplate;

    public DayClassificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns WEEKEND_HOLIDAY, VACATION, or WORKDAY for the given date and user.
     * Priority: weekend/holiday > vacation > workday.
     */
    public String classifyDate(LocalDate date, UUID userId, String geographicZone) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return "WEEKEND_HOLIDAY";
        }

        Integer holidayCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM holidays WHERE geographic_zone = ? AND holiday_date = ?",
                Integer.class, geographicZone, date
        );
        if (holidayCount != null && holidayCount > 0) {
            return "WEEKEND_HOLIDAY";
        }

        Integer vacationCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vacation_periods WHERE user_id = ? AND start_date <= ? AND end_date >= ?",
                Integer.class, userId, date, date
        );
        if (vacationCount != null && vacationCount > 0) {
            return "VACATION";
        }

        return "WORKDAY";
    }
}
