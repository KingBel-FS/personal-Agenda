# Story 1.4: Vacances utilisateur et synchronisation des jours feries

Status: review

## Story

As a user,  
I want my holidays and vacations to be known by the system,  
so that daily classification is automatic from the start.

## Acceptance Criteria

1. A la fin de l'onboarding, les jours feries de la zone configuree sont recuperes sans bloquer l'usage.
2. L'utilisateur peut creer, modifier et supprimer ses periodes de vacances.
3. Un rafraichissement annuel est planifie.
4. Les erreurs API gouv declenchent retry et degradation gracieuse.

## Tasks / Subtasks

- [x] Creer le modele `holidays`, `vacation_periods` et `holiday_sync_states` via Liquibase (AC: 1, 2)
- [x] Implementer le client d'integration `calendrier.api.gouv.fr` avec retry exponentiel (AC: 1, 4)
- [x] Implementer le job worker de sync initiale et annuelle (AC: 1, 3)
- [x] Creer les endpoints CRUD vacances utilisateur (AC: 2)
- [x] Exposer l'etat de synchronisation et l'alerte profil (AC: 4)
- [x] Ajouter tests backend/frontend/worker et valider build + Docker (AC: 4)

## Dev Notes

- Les indisponibilites API ne doivent pas bloquer la creation de compte. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]
- Le worker est responsable des syncs planifiees et du retry. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]
- L'edition utilisateur des vacances se fait depuis le profil. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]

### Project Structure Notes

- `apps/api/.../holiday`, `apps/api/.../vacation`, `apps/api/.../user`
- `apps/worker/.../holiday`
- `apps/web/src/app/features/profile/`

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `./mvnw.cmd test` dans `apps/api`
- `./mvnw.cmd test` dans `apps/worker`
- `npm test -- --watch=false --browsers=ChromeHeadless`
- `npm run build`
- `docker compose up --build -d`
- `docker compose logs worker --tail=80`
- `docker exec personal-agenda-postgres-1 psql -U ia -d ia -c "..."`

### Completion Notes List

- Changelog Liquibase `008` ajoute pour `holidays`, `vacation_periods` et `holiday_sync_states`, avec seeding `PENDING` pour les comptes existants.
- La creation de compte et le changement de zone marquent l'etat de sync en attente sans bloquer l'usage courant.
- Nouveau `apps/worker` Spring Boot avec client `calendrier.api.gouv.fr`, retry via `RetryTemplate`, sync initiale et refresh annuel.
- Le worker gere `PENDING`, `RETRY_SCHEDULED` et `SYNCED(last_synced_year < currentYear)` puis materialise les jours feries par zone et annee.
- Un bug runtime Docker a ete corrige pendant la validation reelle: l'image worker build maintenant avec Maven directement, sans wrapper relatif casse.
- Un second bug runtime a ete corrige: les timestamps SQL du worker sont desormais persistes via `Timestamp.from(...)`, ce qui debloque l'insertion PostgreSQL des jours feries.
- Endpoints CRUD vacances exposes sous `/api/v1/profile/vacations` et statut de sync inclus dans `GET /api/v1/profile`.
- Le frontend profil affiche l'alerte de sync, le detail de retry si necessaire, et permet l'ajout, la modification et la suppression de periodes de vacances.
- Validation Docker reelle: `api`, `web`, `worker`, `postgres` et `mailpit` demarrent; `actuator/health` est `UP`; `holidays_count = 11`; tous les `holiday_sync_states` sont passes a `SYNCED`.

### File List

- apps/api/src/main/java/com/ia/api/auth/service/AuthService.java
- apps/api/src/main/java/com/ia/api/holiday/domain/HolidayEntity.java
- apps/api/src/main/java/com/ia/api/holiday/domain/HolidaySyncStateEntity.java
- apps/api/src/main/java/com/ia/api/holiday/domain/HolidaySyncStatus.java
- apps/api/src/main/java/com/ia/api/holiday/repository/HolidayRepository.java
- apps/api/src/main/java/com/ia/api/holiday/repository/HolidaySyncStateRepository.java
- apps/api/src/main/java/com/ia/api/holiday/service/HolidaySyncStateService.java
- apps/api/src/main/java/com/ia/api/user/api/HolidaySyncStatusResponse.java
- apps/api/src/main/java/com/ia/api/user/api/ProfileController.java
- apps/api/src/main/java/com/ia/api/user/api/ProfileResponse.java
- apps/api/src/main/java/com/ia/api/user/api/VacationPeriodRequest.java
- apps/api/src/main/java/com/ia/api/user/api/VacationPeriodResponse.java
- apps/api/src/main/java/com/ia/api/user/service/ProfileService.java
- apps/api/src/main/java/com/ia/api/vacation/domain/VacationPeriodEntity.java
- apps/api/src/main/java/com/ia/api/vacation/repository/VacationPeriodRepository.java
- apps/api/src/main/java/com/ia/api/vacation/service/VacationService.java
- apps/api/src/main/resources/db/changelog/changes/008-holidays-vacations-sync-state.yaml
- apps/api/src/main/resources/db/changelog/db.changelog-master.yaml
- apps/api/src/test/java/com/ia/api/auth/service/AuthServiceTest.java
- apps/api/src/test/java/com/ia/api/user/api/ProfileControllerTest.java
- apps/api/src/test/java/com/ia/api/user/service/ProfileServiceTest.java
- apps/web/src/app/features/profile/profile-api.service.ts
- apps/web/src/app/features/profile/profile-page.component.html
- apps/web/src/app/features/profile/profile-page.component.scss
- apps/web/src/app/features/profile/profile-page.component.spec.ts
- apps/web/src/app/features/profile/profile-page.component.ts
- apps/worker/Dockerfile
- apps/worker/pom.xml
- apps/worker/src/main/java/com/ia/worker/WorkerApplication.java
- apps/worker/src/main/java/com/ia/worker/config/WorkerBeansConfig.java
- apps/worker/src/main/java/com/ia/worker/holiday/HolidayGovernmentClient.java
- apps/worker/src/main/java/com/ia/worker/holiday/HolidaySyncWorkerService.java
- apps/worker/src/main/resources/application.properties
- apps/worker/src/test/java/com/ia/worker/holiday/HolidayGovernmentClientTest.java
- docker-compose.yml

### Change Log

- 2026-03-24: Implementation complete de la story 1.4 sous workflow `bmad-dev`, avec validation locale et Docker.
