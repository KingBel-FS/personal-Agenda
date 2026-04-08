# Story 3.2: Marquage exécuté, non exécuté et suspension d'occurrence

Status: review

## Story

As a user,  
I want to change the status of today's occurrence quickly,  
so that the system reflects what I actually did.

## Acceptance Criteria

1. Une occurrence du jour peut être marquée `done`, `missed` ou `suspended` jusqu'à minuit.
2. La suspension affiche une confirmation expliquant la protection de streak.
3. Chaque mutation renvoie un toast et met à jour la progression.
4. La mutation backend est idempotente et durable.

## Tasks / Subtasks

- [x] Implémenter les endpoints de statut d'occurrence (AC: 1, 4)
- [x] Persister l'historique `occurrence_status_events` (AC: 4)
- [x] Implémenter `TaskCard` avec tap/swipe/long press (AC: 1, 3)
- [x] Ajouter la bottom sheet de suspension (AC: 2)
- [ ] Tester bornes temporelles jusqu'à minuit et accessibilité gestures/fallbacks (AC: 1, 2, 3)

## Dev Notes

- Les interactions principales doivent rester directes : tap, swipe, long press. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]
- Les statuts autorisés sont contraints par l'architecture ; ne pas en inventer d'autres. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used
Claude Opus 4.6

### Debug Log References

### Completion Notes List
- Migration 020 : table occurrence_status_events avec FK et index
- OccurrenceStatusService : idempotent, contrainte today-only (Paris TZ), audit event systématique
- 3 POST endpoints explicites : /complete, /miss, /suspend (architecture-compliant)
- TodayResponse enrichi : missedCount, suspendedCount, progress exclut skipped du dénominateur
- Frontend : tap → done, long press → context menu (done/missed/suspend), suspension confirmation sheet
- Toast auto-dismiss 3.5s après chaque mutation

### File List
- apps/api/src/main/resources/db/changelog/changes/020-occurrence-status-events.yaml
- apps/api/src/main/java/com/ia/api/today/OccurrenceStatusEventEntity.java
- apps/api/src/main/java/com/ia/api/today/OccurrenceStatusEventRepository.java
- apps/api/src/main/java/com/ia/api/today/OccurrenceStatusService.java
- apps/api/src/main/java/com/ia/api/today/TodayController.java (modified)
- apps/api/src/main/java/com/ia/api/today/TodayResponse.java (modified)
- apps/api/src/main/java/com/ia/api/today/TodayService.java (modified)
- apps/web/src/app/features/today/today-api.service.ts (modified)
- apps/web/src/app/features/today/today-page.component.ts (modified)
- apps/web/src/app/features/today/today-page.component.html (modified)
- apps/web/src/app/features/today/today-page.component.scss (modified)
