# Story 1.3: Profil utilisateur, heures de reveil et zone geographique

Status: review

## Story

As a signed-in user,  
I want to manage my profile, wake-up times and geographic zone,  
so that the app can calculate my schedule correctly.

## Acceptance Criteria

1. L'utilisateur peut modifier pseudo, photo, zone et heures de reveil par categorie de jour.
2. Le changement de zone geographique demande une confirmation explicite sur l'impact des jours feries.
3. Les valeurs sauvegardees sont utilisees par les projections metier de planification.

## Tasks / Subtasks

- [x] Etendre le modele de donnees profil et day profiles (AC: 1, 3)
  - [x] Ajouter changelogs Liquibase pour `day_profiles`
- [x] Implementer endpoints lecture/mise a jour profil (AC: 1)
  - [x] Gerer upload photo via asset prive et URL signee
- [x] Implementer confirmation de changement de zone (AC: 2)
  - [x] Retourner l'impact attendu cote API
- [x] Brancher le frontend profil sur Angular Reactive Forms (AC: 1, 2)
  - [x] Assurer validations accessibles
- [x] Couvrir par tests backend/frontend principaux (AC: 1, 2, 3)

## Dev Notes

- Les heures de reveil existent par categories : jours travailles, vacances, weekend/férie. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]
- La photo utilisateur est privee et servie par URL signee. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]
- L'UX attend une edition simple sans exposer la complexite metier. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]

### Project Structure Notes

- `apps/api/.../user`, `apps/api/.../asset`
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

- `./mvnw.cmd test`
- `npm test -- --watch=false --browsers=ChromeHeadless`
- `npm run build`
- `docker compose up --build -d`
- `POST /api/v1/auth/login`
- `GET /api/v1/profile`
- `POST /api/v1/profile/zone-impact`
- `PUT /api/v1/profile`
- `GET signed profile photo URL`
- `docker compose exec -T postgres psql -U ia -d ia -At -c "..."`

### Completion Notes List

- Modele profil etendu avec `timezone_name`, table `day_profiles` et creation de profils par defaut `WORKDAY/VACATION/WEEKEND_HOLIDAY`.
- Endpoints authentifies `GET/PUT /api/v1/profile` et `POST /api/v1/profile/zone-impact` ajoutes; la reponse expose une projection de planification reutilisable par la suite metier.
- La photo de profil reste privee: stockage local prive, tracabilite `assets`, endpoint public signe `GET /api/v1/profile/photo`.
- Le changement de zone impose une confirmation explicite si la zone change, avec message d'impact jours feries renvoye cote API.
- Frontend Angular `profile` ajoute avec Reactive Forms, validations accessibles, preview d'impact de zone et upload photo.
- Bug runtime Docker corrige apres validation reelle: `server.tomcat.max-part-count=20` pour accepter le multipart complet de la story.
- Verification Docker executee sur `charlie@example.com`: login OK, lecture profil OK, preview zone OK, update multipart OK, URL signee photo `200`, base mise a jour pour zone/pseudo/heures de reveil.

### File List

- apps/api/src/main/java/com/ia/api/auth/domain/UserEntity.java
- apps/api/src/main/java/com/ia/api/auth/service/AuthService.java
- apps/api/src/main/java/com/ia/api/common/config/SecurityConfig.java
- apps/api/src/main/java/com/ia/api/user/api/DayProfileRequest.java
- apps/api/src/main/java/com/ia/api/user/api/DayProfileResponse.java
- apps/api/src/main/java/com/ia/api/user/api/ProfileController.java
- apps/api/src/main/java/com/ia/api/user/api/ProfileResponse.java
- apps/api/src/main/java/com/ia/api/user/api/SchedulingProfileResponse.java
- apps/api/src/main/java/com/ia/api/user/api/UpdateProfileRequest.java
- apps/api/src/main/java/com/ia/api/user/api/ZoneImpactRequest.java
- apps/api/src/main/java/com/ia/api/user/api/ZoneImpactResponse.java
- apps/api/src/main/java/com/ia/api/user/domain/DayCategory.java
- apps/api/src/main/java/com/ia/api/user/domain/DayProfileEntity.java
- apps/api/src/main/java/com/ia/api/user/repository/AssetRepository.java
- apps/api/src/main/java/com/ia/api/user/repository/DayProfileRepository.java
- apps/api/src/main/java/com/ia/api/user/service/LocalProfilePhotoStorageService.java
- apps/api/src/main/java/com/ia/api/user/service/ProfilePhotoStorageService.java
- apps/api/src/main/java/com/ia/api/user/service/ProfileService.java
- apps/api/src/main/java/com/ia/api/user/service/SignedAssetUrlService.java
- apps/api/src/main/resources/application.properties
- apps/api/src/main/resources/db/changelog/db.changelog-master.yaml
- apps/api/src/main/resources/db/changelog/changes/007-user-profile-day-profiles.yaml
- apps/api/src/test/java/com/ia/api/auth/service/AuthServiceTest.java
- apps/api/src/test/java/com/ia/api/user/api/ProfileControllerTest.java
- apps/api/src/test/java/com/ia/api/user/service/ProfileServiceTest.java
- apps/web/src/app/app.routes.ts
- apps/web/src/app/features/profile/profile-api.service.ts
- apps/web/src/app/features/profile/profile-page.component.html
- apps/web/src/app/features/profile/profile-page.component.scss
- apps/web/src/app/features/profile/profile-page.component.spec.ts
- apps/web/src/app/features/profile/profile-page.component.ts

### Change Log

- 2026-03-24: Implementation complete de la story 1.3 sous workflow `bmad-dev`, avec validation locale et Docker.
