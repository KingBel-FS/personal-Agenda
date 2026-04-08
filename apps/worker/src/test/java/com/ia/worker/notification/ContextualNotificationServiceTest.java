package com.ia.worker.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContextualNotificationServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private WebPushSender webPushSender;

    private ContextualNotificationService service;

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 26);

    @BeforeEach
    void setUp() {
        service = new ContextualNotificationService(jdbcTemplate, webPushSender);
    }

    // ── Streak danger tests ──────────────────────────────

    @Test
    void streakDanger_before20h_doesNotQuery() {
        stubNoBirthdays();

        service.checkAndSend(TODAY, LocalTime.of(19, 59));

        // No streak query should happen before 20h — no push sent
        verify(webPushSender, never()).sendWithStatus(any(), any(), any(), any());
    }

    @Test
    void streakDanger_after20h_withCandidate_sendsNotification() {
        UUID userId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();

        stubStreakCandidates(List.of(
                new ContextualNotificationService.StreakDangerCandidate(userId, 5)
        ));
        stubNoBirthdays();
        stubSubscriptions(userId, List.of(
                new ContextualNotificationService.SubscriptionRow(subId, "https://fcm.test/push", "auth", "p256dh")
        ));

        when(webPushSender.sendWithStatus(eq("https://fcm.test/push"), eq("auth"), eq("p256dh"), any()))
                .thenReturn(201);

        service.checkAndSend(TODAY, LocalTime.of(20, 5));

        // Verify push was sent
        verify(webPushSender).sendWithStatus(eq("https://fcm.test/push"), eq("auth"), eq("p256dh"), any());

        // Verify notification_center insert with STREAK_DANGER type
        verify(jdbcTemplate).update(contains("notification_center"), any(UUID.class), eq(userId),
                contains("série de 5 jours"), anyString(), eq("STREAK_DANGER"));
    }

    @Test
    void streakDanger_deadSubscription_revokesIt() {
        UUID userId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();

        stubStreakCandidates(List.of(
                new ContextualNotificationService.StreakDangerCandidate(userId, 3)
        ));
        stubNoBirthdays();
        stubSubscriptions(userId, List.of(
                new ContextualNotificationService.SubscriptionRow(subId, "https://fcm.test/dead", "auth", "p256dh")
        ));

        when(webPushSender.sendWithStatus(any(), any(), any(), any())).thenReturn(410);

        service.checkAndSend(TODAY, LocalTime.of(20, 5));

        // Verify subscription revoked
        verify(jdbcTemplate).update(contains("revoked_at"), eq(subId));
    }

    @Test
    void streakDanger_noCandidates_noNotificationsSent() {
        stubStreakCandidates(List.of());
        stubNoBirthdays();

        service.checkAndSend(TODAY, LocalTime.of(20, 30));

        verify(webPushSender, never()).sendWithStatus(any(), any(), any(), any());
    }

    // ── Birthday tests ───────────────────────────────────

    @Test
    void birthday_afterWakeUpPlusOneHour_sendsNotification() {
        UUID userId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();

        stubNoStreakCandidates();
        stubBirthdayCandidates(List.of(
                new ContextualNotificationService.BirthdayCandidate(userId, "Gino", LocalTime.of(7, 0))
        ));
        stubSubscriptions(userId, List.of(
                new ContextualNotificationService.SubscriptionRow(subId, "https://fcm.test/push", "auth", "p256dh")
        ));

        when(webPushSender.sendWithStatus(any(), any(), any(), any())).thenReturn(201);

        // 08:30 is after 07:00 + 1h = 08:00
        service.checkAndSend(TODAY, LocalTime.of(8, 30));

        ArgumentCaptor<WebPushSender.PushPayload> payloadCaptor = ArgumentCaptor.forClass(WebPushSender.PushPayload.class);
        verify(webPushSender).sendWithStatus(any(), any(), any(), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue().title()).contains("Gino");
        assertThat(payloadCaptor.getValue().title()).contains("anniversaire");

        verify(jdbcTemplate).update(contains("notification_center"), any(UUID.class), eq(userId),
                contains("anniversaire"), anyString(), eq("BIRTHDAY"));
    }

    @Test
    void birthday_beforeWakeUpPlusOneHour_doesNotSend() {
        UUID userId = UUID.randomUUID();

        stubNoStreakCandidates();
        stubBirthdayCandidates(List.of(
                new ContextualNotificationService.BirthdayCandidate(userId, "Gino", LocalTime.of(9, 0))
        ));

        // 09:30 is before 09:00 + 1h = 10:00
        service.checkAndSend(TODAY, LocalTime.of(9, 30));

        verify(webPushSender, never()).sendWithStatus(any(), any(), any(), any());
    }

    @Test
    void birthday_noSubscriptions_doesNotInsertCenter() {
        UUID userId = UUID.randomUUID();

        stubNoStreakCandidates();
        stubBirthdayCandidates(List.of(
                new ContextualNotificationService.BirthdayCandidate(userId, "Gino", LocalTime.of(7, 0))
        ));
        stubSubscriptions(userId, List.of());

        service.checkAndSend(TODAY, LocalTime.of(10, 0));

        verify(webPushSender, never()).sendWithStatus(any(), any(), any(), any());
        verify(jdbcTemplate, never()).update(contains("notification_center"), any(UUID.class), any(UUID.class),
                anyString(), anyString(), anyString());
    }

    @Test
    void weeklyGoal_onSundayAfter20h_sendsNotification() {
        UUID userId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();

        stubNoBirthdays();
        stubStreakCandidates(List.of());
        stubGoalUsers("WEEKLY", List.of(userId));
        stubNotificationCheck(userId, "WEEKLY_GOAL", 0);
        stubUnmetGoals(userId, "WEEKLY", List.of(
                new ContextualNotificationService.GoalReminderCandidate(UUID.randomUUID(), "TASK", 4, UUID.randomUUID(), "Lecture", 1)
        ));
        stubSubscriptions(userId, List.of(
                new ContextualNotificationService.SubscriptionRow(subId, "https://fcm.test/push", "auth", "p256dh")
        ));
        when(webPushSender.sendWithStatus(any(), any(), any(), any())).thenReturn(201);

        service.checkAndSend(LocalDate.of(2026, 3, 29), LocalTime.of(20, 5));

        verify(webPushSender).sendWithStatus(any(), any(), any(), any());
        verify(jdbcTemplate).update(contains("notification_center"), any(UUID.class), eq(userId),
                contains("Objectif hebdomadaire"), contains("Lecture 1/4"), eq("WEEKLY_GOAL"));
    }

    @Test
    void monthlyGoal_onLastDayAfter20h_sendsNotification() {
        UUID userId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();

        stubNoBirthdays();
        stubStreakCandidates(List.of());
        stubGoalUsers("MONTHLY", List.of(userId));
        stubNotificationCheck(userId, "MONTHLY_GOAL", 0);
        stubUnmetGoals(userId, "MONTHLY", List.of(
                new ContextualNotificationService.GoalReminderCandidate(UUID.randomUUID(), "GLOBAL", 10, null, null, 7)
        ));
        stubSubscriptions(userId, List.of(
                new ContextualNotificationService.SubscriptionRow(subId, "https://fcm.test/push", "auth", "p256dh")
        ));
        when(webPushSender.sendWithStatus(any(), any(), any(), any())).thenReturn(201);

        service.checkAndSend(LocalDate.of(2026, 3, 31), LocalTime.of(20, 1));

        verify(webPushSender).sendWithStatus(any(), any(), any(), any());
        verify(jdbcTemplate).update(contains("notification_center"), any(UUID.class), eq(userId),
                contains("Objectif mensuel"), contains("Objectif global 7/10"), eq("MONTHLY_GOAL"));
    }

    // ── Helpers ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void stubStreakCandidates(List<ContextualNotificationService.StreakDangerCandidate> candidates) {
        when(jdbcTemplate.query(contains("streak_snapshots"), any(RowMapper.class), any(), any(), any()))
                .thenReturn(candidates);
    }

    @SuppressWarnings("unchecked")
    private void stubNoStreakCandidates() {
        // Before 20h, streak check returns early — no query happens
    }

    @SuppressWarnings("unchecked")
    private void stubBirthdayCandidates(List<ContextualNotificationService.BirthdayCandidate> candidates) {
        when(jdbcTemplate.query(contains("birth_date"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(candidates);
    }

    @SuppressWarnings("unchecked")
    private void stubNoBirthdays() {
        when(jdbcTemplate.query(contains("birth_date"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private void stubGoalUsers(String periodType, List<UUID> userIds) {
        when(jdbcTemplate.query(contains("SELECT DISTINCT user_id"), any(RowMapper.class), eq(periodType)))
                .thenReturn(userIds);
    }

    @SuppressWarnings("unchecked")
    private void stubUnmetGoals(UUID userId, String periodType, List<ContextualNotificationService.GoalReminderCandidate> goals) {
        when(jdbcTemplate.query(contains("FROM goals g"), any(RowMapper.class), any(), any(), eq(userId), eq(periodType)))
                .thenReturn(goals);
    }

    private void stubNotificationCheck(UUID userId, String notificationType, int count) {
        when(jdbcTemplate.queryForObject(contains("FROM notification_center"), eq(Integer.class), eq(userId), eq(notificationType), any()))
                .thenReturn(count);
    }

    @SuppressWarnings("unchecked")
    private void stubSubscriptions(UUID userId, List<ContextualNotificationService.SubscriptionRow> subs) {
        when(jdbcTemplate.query(contains("push_subscriptions"), any(RowMapper.class), eq(userId)))
                .thenReturn(subs);
    }
}
