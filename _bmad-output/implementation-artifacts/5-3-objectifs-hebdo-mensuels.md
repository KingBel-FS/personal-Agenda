# Story 5.3: Objectifs hebdomadaires et mensuels

Status: review

## Story

As a user,  
I want to define overall or task-specific goals,  
so that I can measure whether my habits meet my targets.

## Acceptance Criteria

1. L'utilisateur peut créer des objectifs globaux ou par tâche éligible.
2. La progression est recalculée en temps réel depuis les occurrences.
3. L'UI affiche statut courant et historique récent.
4. Le moteur de reminders réutilise la même projection objectif.

## Tasks / Subtasks

- [x] Créer tables et modèle `goals` / snapshots si requis (AC: 1, 2)
- [x] Implémenter CRUD objectifs et logique d'éligibilité (AC: 1)
- [x] Implémenter projection de progression temps réel (AC: 2, 4)
- [x] Construire l'écran objectifs Angular (AC: 3)
- [x] Tester cohérence avec tâches hebdo/mensuelles et objectifs non atteints (AC: 1, 2, 4)

## Dev Notes

- Les objectifs sont réutilisés par les notifications hebdo/mensuelles ; éviter toute logique dupliquée. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `apps/api`: `./mvnw.cmd test`
- `apps/worker`: `./mvnw.cmd test`
- `apps/web`: `npm test -- --watch=false --browsers=ChromeHeadless`
- `apps/web`: `npm run build`
- root: `docker compose up --build -d api worker web`

### Completion Notes List

- Ajout du domaine `goals` avec migration Liquibase `026-goals.yaml`, entité JPA, repository et API CRUD `GET/POST/PUT/DELETE /api/v1/goals`.
- Implémentation d'une projection temps réel des objectifs hebdomadaires et mensuels, globale ou par tâche récurrente éligible, avec historique récent sur 4 périodes.
- Intégration du moteur de reminders hebdo/mensuels dans le worker via `ContextualNotificationService`, aligné sur la même projection métier d'objectifs non atteints.
- Ajout de l'écran Angular `Objectifs`, de la route `/goals` et de l'entrée de navigation desktop/mobile.
- Validation complète : `99` tests API OK, `27` tests worker OK, `35` tests web OK, build Angular OK, redéploiement Docker OK.

### File List

- apps/api/src/main/resources/db/changelog/db.changelog-master.yaml
- apps/api/src/main/resources/db/changelog/changes/026-goals.yaml
- apps/api/src/main/java/com/ia/api/goal/domain/GoalEntity.java
- apps/api/src/main/java/com/ia/api/goal/repository/GoalRepository.java
- apps/api/src/main/java/com/ia/api/goal/api/CreateGoalRequest.java
- apps/api/src/main/java/com/ia/api/goal/api/UpdateGoalRequest.java
- apps/api/src/main/java/com/ia/api/goal/api/GoalProgressHistoryItem.java
- apps/api/src/main/java/com/ia/api/goal/api/GoalProgressSnapshot.java
- apps/api/src/main/java/com/ia/api/goal/api/GoalEligibleTaskItem.java
- apps/api/src/main/java/com/ia/api/goal/api/GoalResponse.java
- apps/api/src/main/java/com/ia/api/goal/api/GoalListResponse.java
- apps/api/src/main/java/com/ia/api/goal/api/GoalController.java
- apps/api/src/main/java/com/ia/api/goal/service/GoalService.java
- apps/api/src/main/java/com/ia/api/task/repository/TaskOccurrenceRepository.java
- apps/api/src/test/java/com/ia/api/goal/api/GoalControllerTest.java
- apps/api/src/test/java/com/ia/api/goal/service/GoalServiceTest.java
- apps/worker/src/main/java/com/ia/worker/notification/ContextualNotificationService.java
- apps/worker/src/test/java/com/ia/worker/notification/ContextualNotificationServiceTest.java
- apps/web/src/app/app.routes.ts
- apps/web/src/app/core/app-shell.component.html
- apps/web/src/app/features/goals/goals-api.service.ts
- apps/web/src/app/features/goals/goals-page.component.ts
- apps/web/src/app/features/goals/goals-page.component.html
- apps/web/src/app/features/goals/goals-page.component.scss
- apps/web/src/app/features/goals/goals-page.component.spec.ts

## Change Log

- 2026-03-26: implémentation complète de la story 5.3 (backend objectifs, projection temps réel, reminders worker, écran Angular, tests et déploiement Docker).
