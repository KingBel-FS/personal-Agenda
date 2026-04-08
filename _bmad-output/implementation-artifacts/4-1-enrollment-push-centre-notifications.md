# Story 4.1: Enrollment Web Push, permission banner et centre de notifications

Status: review

## Story

As a user,  
I want the app to guide me when notifications are unavailable,  
so that I can restore reminders without guessing what is wrong.

## Acceptance Criteria

1. Un banner persistant informe quand la permission push manque ou est révoquée.
2. Une subscription VAPID valide est enregistrée pour l'utilisateur.
3. Toutes les notifications générées sont traçables dans un centre in-app.

## Tasks / Subtasks

- [x] Implémenter le flux d'enrôlement Web Push dans Angular PWA (AC: 2)
- [x] Créer l'API d'enregistrement/révocation de subscription (AC: 2)
- [x] Implémenter `NotificationBanner` et l'accès système de permission (AC: 1)
- [x] Créer la table et les endpoints du centre de notifications (AC: 3)
- [x] Tester permissions refusées/révoquées et purge des endpoints invalides (AC: 1, 2, 3)

## Dev Notes

- Le centre de notifications in-app est un fallback important face aux limitations iOS. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]
- Les subscriptions invalides doivent être purgées automatiquement. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used
Claude Opus 4.6

### Debug Log References
- TypeScript Uint8Array type mismatch with PushManager.subscribe() applicationServerKey — fixed by using `.buffer as ArrayBuffer`

### Completion Notes List
- Generated VAPID ECDSA P-256 key pair for Web Push (stored in docker-compose env vars, configured in application.properties)
- Created `push_subscriptions` table (Liquibase 023) with user_id, endpoint, auth_key, p256dh_key, revoked_at — unique constraint on (user_id, endpoint)
- Created `notification_center` table (Liquibase 024) with title, body, notification_type, status (RECEIVED/VIEWED/DISMISSED), timestamps
- Backend: PushSubscriptionEntity/Repository + PushSubscriptionService (subscribe with upsert, unsubscribe via revoke, purge revoked)
- Backend: NotificationCenterEntity/Repository + NotificationCenterService (paged list, unviewed count, mark viewed, dismiss)
- Backend: NotificationController with endpoints — GET /vapid-key, POST /subscribe, POST /unsubscribe, GET /center, GET /center/unviewed-count, POST /center/{id}/mark-viewed, POST /center/{id}/dismiss
- Frontend: PushNotificationService — full enrollment flow (permission request → VAPID key fetch → PushManager.subscribe → server registration), unenroll, status check
- Frontend: Service worker (sw.js) expanded with push event handler (showNotification) and notificationclick handler (action dispatch + window focus)
- Frontend: NotificationBannerComponent — persistent alert with 3 states (info/default, warning/denied, error), dismiss per session, integrated into AppShellComponent
- Frontend: NotificationCenterPageComponent — full page with notification list, relative timestamps, mark-viewed on click, dismiss button, pagination
- Frontend: Route /notifications added, navigation links in sidebar + bottom nav
- 14 unit tests (7 PushSubscriptionService + 7 NotificationCenterService): subscribe, reactivate revoked, unknown user, unsubscribe, purge, list, count, markViewed, dismiss, wrong user checks
- Full regression suite: 71 tests, 0 failures

### Change Log
- 2026-03-26: Story 4.1 implemented — Web Push enrollment, notification banner, notification center (AC 1, 2, 3 satisfied)

### File List
- apps/api/src/main/resources/db/changelog/changes/023-push-subscriptions.yaml (new)
- apps/api/src/main/resources/db/changelog/changes/024-notification-center.yaml (new)
- apps/api/src/main/resources/db/changelog/db.changelog-master.yaml (modified)
- apps/api/src/main/resources/application.properties (modified — VAPID config)
- apps/api/src/main/java/com/ia/api/notification/domain/PushSubscriptionEntity.java (new)
- apps/api/src/main/java/com/ia/api/notification/domain/NotificationCenterEntity.java (new)
- apps/api/src/main/java/com/ia/api/notification/repository/PushSubscriptionRepository.java (new)
- apps/api/src/main/java/com/ia/api/notification/repository/NotificationCenterRepository.java (new)
- apps/api/src/main/java/com/ia/api/notification/service/PushSubscriptionService.java (new)
- apps/api/src/main/java/com/ia/api/notification/service/NotificationCenterService.java (new)
- apps/api/src/main/java/com/ia/api/notification/api/NotificationController.java (new)
- apps/api/src/test/java/com/ia/api/notification/service/PushSubscriptionServiceTest.java (new)
- apps/api/src/test/java/com/ia/api/notification/service/NotificationCenterServiceTest.java (new)
- apps/web/public/sw.js (modified — push + notificationclick handlers)
- apps/web/src/app/core/push-notification.service.ts (new)
- apps/web/src/app/core/notification-banner.component.ts (new)
- apps/web/src/app/core/app-shell.component.ts (modified — import banner)
- apps/web/src/app/core/app-shell.component.html (modified — banner + notification nav links)
- apps/web/src/app/features/notifications/notification-api.service.ts (new)
- apps/web/src/app/features/notifications/notification-center-page.component.ts (new)
- apps/web/src/app/features/notifications/notification-center-page.component.html (new)
- apps/web/src/app/features/notifications/notification-center-page.component.scss (new)
- apps/web/src/app/app.routes.ts (modified — /notifications route)
- docker-compose.yml (modified — VAPID env vars)
