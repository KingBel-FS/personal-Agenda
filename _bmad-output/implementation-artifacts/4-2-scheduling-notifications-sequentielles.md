# Story 4.2: Scheduling des notifications séquentielles de tâche

Status: review

## Story

As a user,
I want reminders to be scheduled automatically around each task,
so that I am reminded before and after the planned time if needed.

## Acceptance Criteria

1. Le worker crée les rappels -15 min, -2 min, heure exacte et +1 h.
2. Le scheduling respecte `Europe/Paris`.
3. Les jobs sont idempotents et reprennent après redémarrage.
4. Les timestamps de livraison sont observables.

## Tasks / Subtasks

- [x] Créer la table `notification_jobs` et ses index via Liquibase (AC: 1, 3)
- [x] Implémenter le job worker de planification séquentielle (AC: 1, 2, 3)
- [x] Ajouter verrous SQL et reprise après redémarrage (AC: 3)
- [x] Journaliser scheduling et statuts de livraison (AC: 4)
- [x] Tester scénarios fuseau, rattrapage et déduplication (AC: 2, 3)

## Dev Notes

- La précision push dépend du planificateur worker, pas du frontend. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]
- Le NFR de livraison push est de 30 s après déclenchement du planificateur. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used
Claude Opus 4.6

### Debug Log References
- Worker startup error: `column td.name does not exist` → fixed to `td.title`
- Worker startup error: `ObjectMapper bean not found` → added to WorkerBeansConfig
- Both fixed and verified in deployment

### Completion Notes List
- ✅ Task 1: Created `notification_jobs` table (025-notification-jobs.yaml) with columns: id, user_id, occurrence_id, trigger_type, scheduled_at, status, sent_at, canceled_at, error_message, created_at, updated_at. Indexes on (user_id), (status, scheduled_at). Unique constraint on (occurrence_id, trigger_type) for idempotency.
- ✅ Task 2: Implemented NotificationSchedulingService — scans planned occurrences within configurable horizon (default 2h), creates 4 jobs per occurrence (BEFORE_15, BEFORE_2, ON_TIME, AFTER_60). Uses ON CONFLICT DO NOTHING for deduplication. Implemented NotificationSendingService — picks ready jobs, sends web push via VAPID, updates status. Implemented WebPushSender — VAPID JWT + raw HTTP push.
- ✅ Task 3: NotificationSendingService uses `FOR UPDATE SKIP LOCKED` for concurrent-safe processing. NotificationRecoveryService cancels orphaned PENDING jobs (occurrence no longer planned) every 5 min. Purges old SENT/CANCELED/FAILED jobs daily. PENDING jobs survive restart naturally (polled from DB).
- ✅ Task 4: All scheduling/sending events logged via SLF4J. NotificationJobEntity + NotificationJobRepository added to API. GET `/api/v1/notifications/jobs/{occurrenceId}` endpoint exposes job timestamps (scheduledAt, sentAt, canceledAt, status). NotificationJobService cancels PENDING jobs when occurrence status changes (integrated into OccurrenceStatusService).
- ✅ Task 5: NotificationSchedulingServiceTest (5 tests): timezone respect, idempotent deduplication, empty occurrences, past trigger skipping. NotificationJobServiceTest (7 tests): user-scoped queries, cancel pending jobs, delivery timestamps, access control.
- ✅ Bonus: Fixed task creation redirect from /profile to /tasks/manage (bug fix requested by user)

### Change Log
- 2026-03-26: Implemented story 4.2 — notification job scheduling, sending, recovery, observability. All 5 tasks complete. 96 tests pass (78 API + 18 worker). Deployed successfully.

### File List
- apps/api/src/main/resources/db/changelog/changes/025-notification-jobs.yaml (new)
- apps/api/src/main/resources/db/changelog/db.changelog-master.yaml (modified)
- apps/api/src/main/java/com/ia/api/notification/domain/NotificationJobEntity.java (new)
- apps/api/src/main/java/com/ia/api/notification/repository/NotificationJobRepository.java (new)
- apps/api/src/main/java/com/ia/api/notification/service/NotificationJobService.java (new)
- apps/api/src/main/java/com/ia/api/notification/api/NotificationController.java (modified)
- apps/api/src/main/java/com/ia/api/today/OccurrenceStatusService.java (modified)
- apps/api/src/test/java/com/ia/api/notification/service/NotificationJobServiceTest.java (new)
- apps/worker/pom.xml (modified — added bouncy castle, jjwt, jackson)
- apps/worker/src/main/resources/application.properties (modified — added notification + VAPID config)
- apps/worker/src/main/java/com/ia/worker/config/WorkerBeansConfig.java (modified — added ObjectMapper bean)
- apps/worker/src/main/java/com/ia/worker/notification/NotificationSchedulingService.java (new)
- apps/worker/src/main/java/com/ia/worker/notification/NotificationSendingService.java (new)
- apps/worker/src/main/java/com/ia/worker/notification/WebPushSender.java (new)
- apps/worker/src/main/java/com/ia/worker/notification/NotificationRecoveryService.java (new)
- apps/worker/src/test/java/com/ia/worker/notification/NotificationSchedulingServiceTest.java (new)
- apps/web/src/app/features/tasks/task-create-page.component.ts (modified — redirect fix)
- docker-compose.yml (modified — VAPID env vars for worker)
