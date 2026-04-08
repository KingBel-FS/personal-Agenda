package com.ia.api.notification.api;

import com.ia.api.common.api.ApiResponse;
import com.ia.api.notification.domain.NotificationCenterEntity;
import com.ia.api.notification.domain.NotificationJobEntity;
import com.ia.api.notification.repository.NotificationJobRepository;
import com.ia.api.notification.service.NotificationCenterService;
import com.ia.api.notification.service.NotificationJobService;
import com.ia.api.notification.service.PushSubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final PushSubscriptionService pushService;
    private final NotificationCenterService centerService;
    private final NotificationJobService jobService;

    public NotificationController(
            PushSubscriptionService pushService,
            NotificationCenterService centerService,
            NotificationJobService jobService
    ) {
        this.pushService = pushService;
        this.centerService = centerService;
        this.jobService = jobService;
    }

    // ── VAPID key ────────────────────────────────────────

    @GetMapping("/vapid-key")
    public ApiResponse<Map<String, String>> vapidKey() {
        return ApiResponse.of(Map.of("publicKey", pushService.getVapidPublicKey()));
    }

    // ── Push subscription ────────────────────────────────

    @PostMapping("/subscribe")
    public ApiResponse<Map<String, Object>> subscribe(
            @RequestBody SubscribeRequest body,
            Authentication auth
    ) {
        var sub = pushService.subscribe(
                auth.getName(),
                body.subscription().endpoint(),
                body.subscription().keys().auth(),
                body.subscription().keys().p256dh()
        );
        return ApiResponse.of(Map.of(
                "subscriptionId", sub.getId(),
                "status", "active",
                "createdAt", sub.getCreatedAt().toString()
        ));
    }

    @PostMapping("/unsubscribe")
    public ApiResponse<Map<String, String>> unsubscribe(
            @RequestBody Map<String, String> body,
            Authentication auth
    ) {
        pushService.unsubscribe(auth.getName(), body.get("endpoint"));
        return ApiResponse.of(Map.of("status", "unsubscribed"));
    }

    // ── Notification center ──────────────────────────────

    @GetMapping("/center")
    public ApiResponse<Map<String, Object>> center(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        Page<NotificationCenterEntity> result = centerService.list(auth.getName(), page, size);
        List<NotificationCenterItem> items = result.getContent().stream()
                .map(n -> new NotificationCenterItem(
                        n.getId(), n.getTitle(), n.getBody(), n.getIconUrl(),
                        n.getNotificationType(), n.getRelatedTaskId(),
                        n.getStatus(), n.getCreatedAt().toString()
                ))
                .toList();
        return ApiResponse.of(Map.of(
                "notifications", items,
                "totalCount", result.getTotalElements(),
                "hasMore", result.hasNext()
        ));
    }

    @GetMapping("/center/unviewed-count")
    public ApiResponse<Map<String, Long>> unviewedCount(Authentication auth) {
        return ApiResponse.of(Map.of("count", centerService.unviewedCount(auth.getName())));
    }

    @PostMapping("/center/{id}/mark-viewed")
    public ApiResponse<Map<String, String>> markViewed(
            @PathVariable UUID id,
            Authentication auth
    ) {
        centerService.markViewed(auth.getName(), id);
        return ApiResponse.of(Map.of("status", "viewed"));
    }

    @PostMapping("/center/{id}/dismiss")
    public ApiResponse<Map<String, String>> dismiss(
            @PathVariable UUID id,
            Authentication auth
    ) {
        centerService.dismiss(auth.getName(), id);
        return ApiResponse.of(Map.of("status", "dismissed"));
    }

    // ── Push action (public — SW has no JWT) ─────────────

    @PostMapping("/actions")
    public ApiResponse<Map<String, String>> pushAction(@RequestBody PushActionRequest body) {
        String result = jobService.executeAction(body.notificationJobId(), body.action(), body.taskOccurrenceId());
        if (result == null) {
            return ApiResponse.of(Map.of("status", "ignored"));
        }
        return ApiResponse.of(Map.of("status", result));
    }

    // ── Notification jobs (observability) ────────────────

    @GetMapping("/jobs/{occurrenceId}")
    public ApiResponse<List<NotificationJobItem>> jobsForOccurrence(
            @PathVariable UUID occurrenceId,
            Authentication auth
    ) {
        return ApiResponse.of(jobService.getJobsForOccurrence(auth.getName(), occurrenceId));
    }

    // ── DTOs ─────────────────────────────────────────────

    public record SubscribeRequest(Subscription subscription) {
        public record Subscription(String endpoint, Keys keys) {
            public record Keys(String auth, String p256dh) {}
        }
    }

    public record NotificationCenterItem(
            UUID id, String title, String body, String iconUrl,
            String notificationType, UUID relatedTaskId,
            String status, String createdAt
    ) {}

    public record NotificationJobItem(
            UUID id, String triggerType, String scheduledAt,
            String status, String sentAt, String canceledAt, String errorMessage
    ) {}

    public record PushActionRequest(UUID notificationJobId, String action, UUID taskOccurrenceId) {}
}
