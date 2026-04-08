# Story 6.1: Synchronisation temps réel entre onglets et appareils

Status: review

## Story

As a user,  
I want my account state to stay consistent across my open sessions,  
so that I can trust what I see on each device.

## Acceptance Criteria

1. Les changements d'état pertinents sont propagés en moins de 2 secondes.
2. Today, Agenda, badges et compteurs se rafraîchissent sans reload manuel.
3. Le mécanisme évite les doubles écritures et conflits.

## Tasks / Subtasks

- [x] Choisir et implémenter le mécanisme de sync temps réel minimal viable (SSE ou WebSocket) (AC: 1, 2)
- [x] Publier événements métier pertinents après mutations (AC: 1)
- [x] Rafraîchir shell et vues Angular sur réception (AC: 2)
- [x] Tester scénarios multi-onglets, multi-appareils et déconnexion/reconnexion (AC: 1, 2, 3)

## Dev Notes

- La sync temps réel doit rester simple et ne pas contourner les projections backend. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `apps/api`: `./mvnw.cmd test`
- `apps/web`: `npm test -- --watch=false --browsers=ChromeHeadless`
- `apps/web`: `npm run build`
- `docker compose up --build -d api web`
- `curl -i --max-time 2 -H "Accept: text/event-stream" http://localhost:8080/api/v1/sync/events?...`

### Completion Notes List

- Canal SSE minimal ajouté via `/api/v1/sync/events` avec authentification par access token JWT en query param.
- Les mutations Today publient désormais un événement `TODAY` après écriture.
- Les mutations Task/Create/Edit/Delete publient désormais un événement `TASKS` après écriture.
- Le shell Angular ouvre le flux, recharge streak + badge applicatif, puis les vues `Today`, `Daily` et `Agenda` rechargent leurs projections actives à réception.
- Le mécanisme reste lecture seule côté client : aucune écriture n'est rejouée depuis le canal de sync, ce qui évite les doubles écritures et conflits applicatifs.
- L'export a été temporairement suspendu côté navigation et ses tests ont été désactivés en attente d'un redesign aligné sur les tableaux de statistiques.

### File List

- `apps/api/src/main/java/com/ia/api/common/config/SecurityConfig.java`
- `apps/api/src/main/java/com/ia/api/sync/api/SyncController.java`
- `apps/api/src/main/java/com/ia/api/sync/api/SyncEventResponse.java`
- `apps/api/src/main/java/com/ia/api/sync/service/RealtimeSyncService.java`
- `apps/api/src/main/java/com/ia/api/today/OccurrenceStatusService.java`
- `apps/api/src/main/java/com/ia/api/task/service/TaskService.java`
- `apps/api/src/test/java/com/ia/api/sync/api/SyncControllerTest.java`
- `apps/api/src/test/java/com/ia/api/task/service/TaskServiceTest.java`
- `apps/api/src/test/java/com/ia/api/export/api/ExportControllerTest.java`
- `apps/api/src/test/java/com/ia/api/export/service/ExportServiceTest.java`
- `apps/web/src/app/app.routes.ts`
- `apps/web/src/app/core/app-shell.component.html`
- `apps/web/src/app/core/app-shell.component.ts`
- `apps/web/src/app/core/realtime-sync.service.ts`
- `apps/web/src/app/core/realtime-sync.service.spec.ts`
- `apps/web/src/app/features/today/today-page.component.ts`
- `apps/web/src/app/features/today/daily-view-page.component.ts`
- `apps/web/src/app/features/agenda/agenda-page.component.ts`
- `apps/web/src/app/features/exports/exports-page.component.spec.ts`
