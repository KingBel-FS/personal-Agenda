package com.ia.worker.holiday;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HolidaySyncWorkerService {
    private final JdbcTemplate jdbcTemplate;
    private final HolidayGovernmentClient holidayGovernmentClient;
    private final ZoneId zoneId;

    public HolidaySyncWorkerService(
            JdbcTemplate jdbcTemplate,
            HolidayGovernmentClient holidayGovernmentClient,
            @Value("${app.timezone:Europe/Paris}") String timezone
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.holidayGovernmentClient = holidayGovernmentClient;
        this.zoneId = ZoneId.of(timezone);
    }

    @Scheduled(initialDelayString = "${holiday.sync.initial-delay-ms:5000}", fixedDelayString = "${holiday.sync.fixed-delay-ms:60000}")
    public void syncPendingAndRetryingStates() {
        int currentYear = LocalDate.now(zoneId).getYear();
        List<HolidaySyncTask> tasks = jdbcTemplate.query("""
                        select id, user_id, geographic_zone, retry_count, status
                        from holiday_sync_states
                        where status = 'PENDING'
                           or (status = 'RETRY_SCHEDULED' and (next_retry_at is null or next_retry_at <= now()))
                           or (status = 'SYNCED' and (last_synced_year is null or last_synced_year < ?))
                        """,
                (rs, rowNum) -> mapTask(rs),
                currentYear
        );

        for (HolidaySyncTask task : tasks) {
            syncTask(task, currentYear);
        }
    }

    @Transactional
    protected void syncTask(HolidaySyncTask task, int currentYear) {
        try {
            Map<LocalDate, String> holidays = holidayGovernmentClient.fetchHolidays(task.geographicZone(), currentYear);
            jdbcTemplate.update("delete from holidays where geographic_zone = ? and source_year = ?", task.geographicZone(), currentYear);
            holidays.forEach((holidayDate, holidayName) -> jdbcTemplate.update(
                    "insert into holidays (id, geographic_zone, holiday_date, holiday_name, source_year, created_at) values (?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(),
                    task.geographicZone(),
                    holidayDate,
                    holidayName,
                    currentYear,
                    Timestamp.from(Instant.now())
            ));
            jdbcTemplate.update("""
                            update holiday_sync_states
                            set status = 'SYNCED',
                                last_synced_year = ?,
                                last_synced_at = now(),
                                retry_count = 0,
                                next_retry_at = null,
                                last_error = null,
                                updated_at = now()
                            where id = ?
                            """,
                    currentYear,
                    task.id()
            );
        } catch (RestClientException exception) {
            int nextRetryCount = task.retryCount() + 1;
            long backoffMinutes = Math.min(60, (long) Math.pow(2, Math.min(nextRetryCount, 6)));
            jdbcTemplate.update("""
                            update holiday_sync_states
                            set status = 'RETRY_SCHEDULED',
                                retry_count = ?,
                                next_retry_at = ?,
                                last_error = ?,
                                updated_at = now()
                            where id = ?
                            """,
                    nextRetryCount,
                    Timestamp.from(Instant.now().plusSeconds(backoffMinutes * 60)),
                    truncate(exception.getMessage()),
                    task.id()
            );
        }
    }

    private HolidaySyncTask mapTask(ResultSet resultSet) throws SQLException {
        return new HolidaySyncTask(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("geographic_zone"),
                resultSet.getInt("retry_count"),
                resultSet.getString("status")
        );
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    record HolidaySyncTask(UUID id, UUID userId, String geographicZone, int retryCount, String status) {
    }
}
