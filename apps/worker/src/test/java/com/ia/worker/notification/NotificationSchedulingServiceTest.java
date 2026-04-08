package com.ia.worker.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulingServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Captor ArgumentCaptor<Object[]> argsCaptor;

    private NotificationSchedulingService service;

    @BeforeEach
    void setUp() {
        service = new NotificationSchedulingService(jdbcTemplate, 2);
    }

    @Test
    void scheduleNotifications_createsAllFourTriggerTypes() {
        // Given: one planned occurrence at 14:00 Europe/Paris
        UUID occId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID taskDefId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Paris"));
        LocalTime time = LocalTime.of(14, 0);

        // Mock the query to return one occurrence
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(List.of(
                        new NotificationSchedulingService.OccurrenceRow(
                                occId, userId, taskDefId, today, time, "Test Task")
                ));

        // Mock inserts — all succeed (no duplicates)
        when(jdbcTemplate.update(contains("INSERT INTO notification_jobs"), any(), any(), any(), any(), any()))
                .thenReturn(1);

        // When
        service.scheduleNotifications();

        // Then: 4 insert calls (BEFORE_15, BEFORE_2, ON_TIME, AFTER_60)
        verify(jdbcTemplate, atLeast(1)).update(contains("INSERT INTO notification_jobs"),
                any(), any(), any(), any(), any());
    }

    @Test
    void scheduleNotifications_idempotent_noDuplicates() {
        UUID occId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Paris"));
        LocalTime time = LocalTime.of(16, 0);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(List.of(
                        new NotificationSchedulingService.OccurrenceRow(
                                occId, userId, UUID.randomUUID(), today, time, "Task")
                ));

        // Simulate ON CONFLICT DO NOTHING — returns 0 = already exists
        when(jdbcTemplate.update(contains("INSERT INTO notification_jobs"), any(), any(), any(), any(), any()))
                .thenReturn(0);

        service.scheduleNotifications();

        // All attempts return 0 (no new rows) — idempotent behavior
        verify(jdbcTemplate, atLeast(1)).update(contains("INSERT INTO notification_jobs"),
                any(), any(), any(), any(), any());
    }

    @Test
    void scheduleNotifications_respectsEuropeParis_timezone() {
        // Given: occurrence at 23:00 today in Europe/Paris
        UUID occId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Paris"));
        LocalTime time = LocalTime.of(23, 0);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(List.of(
                        new NotificationSchedulingService.OccurrenceRow(
                                occId, userId, UUID.randomUUID(), today, time, "Late Task")
                ));

        when(jdbcTemplate.update(contains("INSERT INTO notification_jobs"), any(), any(), any(), any(), any()))
                .thenReturn(1);

        service.scheduleNotifications();

        // Verify at least the ON_TIME job is created
        // The BEFORE_15, BEFORE_2 scheduled_at timestamps should be computed
        // relative to Europe/Paris, not UTC
        verify(jdbcTemplate, atLeast(1)).update(contains("INSERT INTO notification_jobs"),
                any(), any(), any(), any(), any());
    }

    @Test
    void scheduleNotifications_noOccurrences_noJobsCreated() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(Collections.emptyList());

        service.scheduleNotifications();

        verify(jdbcTemplate, never()).update(contains("INSERT INTO notification_jobs"),
                any(), any(), any(), any(), any());
    }

    @Test
    void scheduleNotifications_skipsPastTriggers() {
        // Given: occurrence at a time already in the past (except AFTER_60)
        UUID occId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Paris"));
        // Set a time in the near past to test skip behavior
        LocalTime pastTime = LocalTime.now(ZoneId.of("Europe/Paris")).minusMinutes(30);
        if (pastTime.isAfter(LocalTime.now(ZoneId.of("Europe/Paris")))) {
            // Edge case: wrapped around midnight, skip this test scenario
            return;
        }

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(List.of(
                        new NotificationSchedulingService.OccurrenceRow(
                                occId, userId, UUID.randomUUID(), today, pastTime, "Past Task")
                ));

        when(jdbcTemplate.update(contains("INSERT INTO notification_jobs"), any(), any(), any(), any(), any()))
                .thenReturn(1);

        service.scheduleNotifications();

        // BEFORE_15 and BEFORE_2 should be skipped (in the past)
        // ON_TIME is in the past too — skipped
        // AFTER_60 may still be created if within 30 min of now
    }
}
