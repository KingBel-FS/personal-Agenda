package com.ia.worker.occurrence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class OccurrenceMaterializationService {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final JdbcTemplate jdbcTemplate;
    private final DayClassificationService dayClassificationService;
    private final int horizonDays;

    public OccurrenceMaterializationService(
            JdbcTemplate jdbcTemplate,
            DayClassificationService dayClassificationService,
            @Value("${occurrence.materialization.horizon-days:30}") int horizonDays
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dayClassificationService = dayClassificationService;
        this.horizonDays = horizonDays;
    }

    @Scheduled(
            initialDelayString = "${occurrence.materialization.initial-delay-ms:5000}",
            fixedDelayString = "${occurrence.materialization.fixed-delay-ms:60000}"
    )
    public void runScheduled() {
        materializeOccurrences(LocalDate.now(PARIS));
    }

    public void materializeOccurrences(LocalDate today) {
        LocalDate horizon = today.plusDays(horizonDays);
        List<TaskRuleRow> rules = loadActiveRules(today, horizon);
        for (TaskRuleRow rule : rules) {
            List<LocalDate> dates = computeOccurrenceDates(rule, today, horizon);
            for (LocalDate date : dates) {
                String dayCategory = dayClassificationService.classifyDate(date, rule.userId(), rule.geographicZone());
                String status = rule.dayCategories().contains(dayCategory) ? "planned" : "skipped";
                LocalTime time = computeOccurrenceTime(rule, dayCategory);
                insertOccurrence(rule, date, dayCategory, status, time);
            }
        }
    }

    private List<TaskRuleRow> loadActiveRules(LocalDate today, LocalDate horizon) {
        return jdbcTemplate.query("""
                SELECT tr.id, tr.task_definition_id, td.user_id, u.geographic_zone,
                       tr.day_categories, tr.start_date, tr.time_mode, tr.fixed_time,
                       tr.wake_up_offset_minutes, tr.recurrence_type, tr.days_of_week,
                       tr.day_of_month, tr.end_date
                FROM task_rules tr
                JOIN task_definitions td ON td.id = tr.task_definition_id
                JOIN users u ON u.id = td.user_id
                WHERE tr.start_date <= ?
                  AND (tr.end_date IS NULL OR tr.end_date >= ?)
                """,
                (rs, rowNum) -> mapRule(rs),
                horizon, today
        );
    }

    private TaskRuleRow mapRule(ResultSet rs) throws SQLException {
        String daysOfWeekRaw = rs.getString("days_of_week");
        List<Integer> daysOfWeek = (daysOfWeekRaw != null && !daysOfWeekRaw.isBlank())
                ? Arrays.stream(daysOfWeekRaw.split(",")).map(Integer::parseInt).toList()
                : List.of();

        List<String> dayCategories = Arrays.asList(rs.getString("day_categories").split(","));

        java.sql.Time fixedTimeSql = rs.getTime("fixed_time");
        LocalTime fixedTime = fixedTimeSql != null ? fixedTimeSql.toLocalTime() : null;

        java.sql.Date endDateSql = rs.getDate("end_date");
        LocalDate endDate = endDateSql != null ? endDateSql.toLocalDate() : null;

        return new TaskRuleRow(
                rs.getObject("id", UUID.class),
                rs.getObject("task_definition_id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("geographic_zone"),
                dayCategories,
                rs.getDate("start_date").toLocalDate(),
                rs.getString("time_mode"),
                fixedTime,
                (Integer) rs.getObject("wake_up_offset_minutes"),
                rs.getString("recurrence_type"),
                daysOfWeek,
                (Integer) rs.getObject("day_of_month"),
                endDate
        );
    }

    List<LocalDate> computeOccurrenceDates(TaskRuleRow rule, LocalDate today, LocalDate horizon) {
        LocalDate effectiveStart = rule.startDate().isBefore(today) ? today : rule.startDate();
        LocalDate effectiveEnd = (rule.endDate() != null && rule.endDate().isBefore(horizon))
                ? rule.endDate() : horizon;

        if ("WEEKLY".equals(rule.recurrenceType())) {
            List<LocalDate> dates = new ArrayList<>();
            LocalDate current = effectiveStart;
            while (!current.isAfter(effectiveEnd)) {
                if (rule.daysOfWeek().contains(current.getDayOfWeek().getValue())) {
                    dates.add(current);
                }
                current = current.plusDays(1);
            }
            return dates;
        }

        if ("MONTHLY".equals(rule.recurrenceType()) && rule.dayOfMonth() != null) {
            List<LocalDate> dates = new ArrayList<>();
            YearMonth startMonth = YearMonth.from(effectiveStart);
            YearMonth endMonth = YearMonth.from(effectiveEnd);
            YearMonth current = startMonth;
            while (!current.isAfter(endMonth)) {
                int day = Math.min(rule.dayOfMonth(), current.lengthOfMonth());
                LocalDate candidate = current.atDay(day);
                if (!candidate.isBefore(effectiveStart) && !candidate.isAfter(effectiveEnd)) {
                    dates.add(candidate);
                }
                current = current.plusMonths(1);
            }
            return dates;
        }

        // ONE_TIME: single occurrence at startDate if within horizon
        if (!rule.startDate().isBefore(today) && !rule.startDate().isAfter(horizon)) {
            return List.of(rule.startDate());
        }
        return List.of();
    }

    private LocalTime computeOccurrenceTime(TaskRuleRow rule, String dayCategory) {
        if ("FIXED".equals(rule.timeMode())) {
            return rule.fixedTime() != null ? rule.fixedTime() : LocalTime.of(8, 0);
        }
        LocalTime wakeUp = jdbcTemplate.query(
                "SELECT wake_up_time FROM day_profiles WHERE user_id = ? AND day_category = ?",
                rs -> {
                    if (rs.next()) {
                        return rs.getTime("wake_up_time").toLocalTime();
                    }
                    return LocalTime.of(7, 0);
                },
                rule.userId(), dayCategory
        );
        int offset = rule.wakeUpOffsetMinutes() != null ? rule.wakeUpOffsetMinutes() : 0;
        return wakeUp.plusMinutes(offset);
    }

    private void insertOccurrence(TaskRuleRow rule, LocalDate date, String dayCategory, String status, LocalTime time) {
        jdbcTemplate.update("""
                INSERT INTO task_occurrences (id, user_id, task_definition_id, task_rule_id,
                    occurrence_date, occurrence_time, status, day_category, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                ON CONFLICT (task_rule_id, occurrence_date) DO NOTHING
                """,
                UUID.randomUUID(),
                rule.userId(),
                rule.taskDefinitionId(),
                rule.id(),
                date,
                time,
                status,
                dayCategory
        );
    }

    record TaskRuleRow(
            UUID id,
            UUID taskDefinitionId,
            UUID userId,
            String geographicZone,
            List<String> dayCategories,
            LocalDate startDate,
            String timeMode,
            LocalTime fixedTime,
            Integer wakeUpOffsetMinutes,
            String recurrenceType,
            List<Integer> daysOfWeek,
            Integer dayOfMonth,
            LocalDate endDate
    ) {}
}
