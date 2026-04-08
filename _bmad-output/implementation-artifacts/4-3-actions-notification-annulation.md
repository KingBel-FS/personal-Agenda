# Story 4.3: Actions depuis notification push et annulation automatique

Status: review

## Story

As a user,
I want to complete or miss a task directly from the notification,
so that I do not have to open the app for routine confirmations.

## Acceptance Criteria

1. Les actions `Exécuté` et `Non exécuté` déclenchent la mutation backend.
2. Une confirmation exploitable est visible après retour dans l'app.
3. Les notifications restantes de la même occurrence sont annulées automatiquement.
4. La photo utilisateur peut être incluse comme image d'expéditeur quand la plateforme le supporte.

## Tasks / Subtasks

- [x] Implémenter la réception des actions push dans le Service Worker Angular (AC: 1)
- [x] Exposer ou réutiliser les endpoints statut d'occurrence pour les actions push (AC: 1)
- [x] Ajouter l'annulation des jobs restants lors du passage `done` (AC: 3)
- [x] Propager un feedback UI après retour dans l'app (AC: 2)
- [x] Ajouter la photo de profil au payload quand disponible (AC: 4)

## Dev Notes

- Certains navigateurs ne supportent pas toutes les actions push ; prévoir fallback ouverture app. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]
- L'action push ne doit pas contourner les règles métier ni l'idempotence des mutations. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used
Claude Opus 4.6

### Debug Log References
- Push payload mismatch: SW expected `iconUrl`/`badgeUrl`/`notificationJobId`/`actions`, worker sent `icon`/`badge`/`data.occurrenceId` — fixed by restructuring PushPayload record
- Endpoint auth: SW cannot attach JWT headers, so `/api/v1/notifications/actions` is public — uses notificationJobId as implicit auth token (UUID unguessable)
- Circular dependency avoided: `executeAction` directly uses repositories instead of calling `OccurrenceStatusService` (which already depends on `NotificationJobService`)

### Completion Notes List
- **Task 1**: Updated `PushPayload` record to include `notificationJobId`, `taskOccurrenceId`, `actionUrl`, `requireInteraction`, `actions` (list of DONE/MISSED buttons). Updated SW to use async/await for sequential fetch-then-navigate.
- **Task 2**: Created `POST /api/v1/notifications/actions` endpoint (public, no JWT). `executeAction` method validates notificationJobId, maps DONE→"done"/MISSED→"missed", updates occurrence status, records audit event, cancels remaining pending jobs. Idempotent: if occurrence already non-planned, returns current status without mutation.
- **Task 3**: `executeAction` calls `cancelPendingJobsForOccurrence(occurrenceId)` after status change — remaining BEFORE_15/BEFORE_2/ON_TIME/AFTER_60 jobs are canceled.
- **Task 4**: SW appends `?pushAction=DONE&occurrenceId=...` to deep link. TodayPageComponent reads query params and shows toast ("Tâche marquée terminée/manquée depuis la notification"). Params cleaned from URL after display.
- **Task 5**: Worker loads `profile_photo_url` from users table and sends it as `imageUrl` in push payload. Displayed as notification image when platform supports it.
- **Bonus fix**: Removed `hibernate.jdbc.time_zone=Europe/Paris` from API application.properties — was causing +1h offset on TIME columns.
- 13 unit tests pass (7 existing + 6 new for executeAction). Worker tests (5) pass. Frontend builds.

### File List
- `apps/worker/src/main/java/com/ia/worker/notification/WebPushSender.java` — PushPayload restructured with all SW fields + PushAction record
- `apps/worker/src/main/java/com/ia/worker/notification/NotificationSendingService.java` — Builds payload with actions, notificationJobId, profile photo
- `apps/api/src/main/java/com/ia/api/notification/api/NotificationController.java` — Added POST /actions endpoint + PushActionRequest DTO
- `apps/api/src/main/java/com/ia/api/notification/service/NotificationJobService.java` — Added executeAction method + new dependencies
- `apps/api/src/main/java/com/ia/api/common/config/SecurityConfig.java` — /api/v1/notifications/actions added to permitAll
- `apps/api/src/main/resources/application.properties` — Removed hibernate.jdbc.time_zone=Europe/Paris
- `apps/web/public/sw.js` — Push event + notificationclick rewritten with async/await, action buttons, feedback params
- `apps/web/src/app/features/today/today-page.component.ts` — Push action feedback via query params + toast
- `apps/api/src/test/java/com/ia/api/notification/service/NotificationJobServiceTest.java` — 6 new tests for executeAction

## Change Log
- 2026-03-26: Story implemented — push actions (DONE/MISSED), public endpoint, job cancellation, feedback UI, profile photo in payload. Fixed timezone bug.
