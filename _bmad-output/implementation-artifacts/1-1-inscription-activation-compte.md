# Story 1.1: Inscription, consentement et activation de compte

Status: review

## Story

As a visitor,  
I want to create my account and activate it securely,  
so that I can start using IA with my own protected data.

## Acceptance Criteria

1. L'API crée un compte en état non activé avec pseudo, email, mot de passe haché, date de naissance, zone géographique et photo optionnelle.
2. Le consentement explicite est stocké avec horodatage et version légale.
3. Un email d'activation contenant un token à usage unique expirant en 1 heure est envoyé.
4. Tant que le compte n'est pas activé, l'authentification est refusée.
5. Le changelog Liquibase de cette story ne crée que les tables nécessaires : `users`, `consents`, `activation_tokens`, `assets`, `refresh_tokens`.

## Tasks / Subtasks

- [x] Créer le squelette Spring Boot API pour le domaine `auth` et `user` (AC: 1, 4)
  - [x] Ajouter les entités, repositories et services requis pour `users`, `consents`, `activation_tokens`
  - [x] Configurer le hash mot de passe avec Argon2id
- [x] Écrire les migrations Liquibase initiales de compte (AC: 1, 2, 5)
  - [x] Créer les tables minimales et index uniques sur email
  - [x] Ajouter contraintes de nullabilité et colonnes d'audit minimales
- [x] Implémenter l'inscription et la persistance du consentement (AC: 1, 2)
  - [x] Valider les payloads d'entrée
  - [x] Retourner le contrat d'erreur standardisé en cas d'échec
- [x] Implémenter l'émission d'email d'activation (AC: 3)
  - [x] Générer un token à usage unique expirant en 1 heure
  - [x] Publier l'email via le provider mail retenu
- [x] Implémenter le endpoint d'activation et le blocage pré-activation (AC: 4)
  - [x] Marquer le compte activé
  - [x] Invalider le token après usage
- [x] Couvrir par tests unitaires et d'intégration (AC: 1, 2, 3, 4, 5)

## Dev Notes

- Stack cible : Spring Boot 4.0.3, PostgreSQL 17.x, Liquibase, Angular 20 PWA. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]
- Tous les changements de schéma passent par Liquibase. Ne pas générer de schéma automatique Hibernate. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]
- Les payloads API sont en `camelCase`, la base en `snake_case`. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]
- Les données utilisateurs doivent rester hébergées en UE et le consentement doit être conservé horodaté. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]
- Les écrans d'onboarding doivent rester mobiles, sobres et accessibles. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]

### Project Structure Notes

- Backend attendu dans `apps/api/src/main/java/com/ia/api/auth` et `.../user`.
- Les ressources Liquibase doivent vivre sous `apps/api/src/main/resources/db/changelog/`.
- Le frontend d'inscription doit vivre sous `apps/web/src/app/features/auth/`.

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
- `curl.exe -F ... http://localhost:4200/api/v1/auth/register`
- `http://localhost:8025/api/v1/messages`
- `docker compose exec postgres psql -U ia -d ia -c "..."`

### Completion Notes List

- Inscription convertie en `multipart/form-data` avec photo optionnelle uploadée en fichier, stockée en stockage privé local et tracée dans `assets`.
- Envoi d'email d'activation réimplémenté via `JavaMailSender` SMTP configurable; validation Docker faite avec Mailpit.
- Lien d'activation routé vers `/activate` côté Angular; page d'activation ajoutée et backend d'activation conservé via `/api/v1/auth/activate`.
- Blocage pré-activation implémenté via `AppUserDetailsService` qui refuse les comptes `PENDING_ACTIVATION`.
- Vérifications de bout en bout exécutées sous Docker: compte créé, email reçu, token activé, compte passé en `ACTIVE`, fichier image présent sur le volume privé.
- Suites vertes: backend `10` tests, frontend `3` tests, build Angular OK.

### File List

- apps/api/pom.xml
- apps/api/src/main/java/com/ia/api/auth/api/AuthController.java
- apps/api/src/main/java/com/ia/api/auth/api/RegisterRequest.java
- apps/api/src/main/java/com/ia/api/auth/service/ActivationEmailSender.java
- apps/api/src/main/java/com/ia/api/auth/service/AppUserDetailsService.java
- apps/api/src/main/java/com/ia/api/auth/service/AuthService.java
- apps/api/src/main/java/com/ia/api/auth/service/SmtpActivationEmailSender.java
- apps/api/src/main/java/com/ia/api/common/api/GlobalExceptionHandler.java
- apps/api/src/main/java/com/ia/api/common/config/CorsConfig.java
- apps/api/src/main/resources/application.properties
- apps/api/src/test/java/com/ia/api/auth/api/AuthControllerTest.java
- apps/api/src/test/java/com/ia/api/auth/service/AppUserDetailsServiceTest.java
- apps/api/src/test/java/com/ia/api/auth/service/AuthServiceTest.java
- apps/api/src/test/java/com/ia/api/auth/service/SmtpActivationEmailSenderTest.java
- apps/api/src/main/java/com/ia/api/user/domain/AssetEntity.java
- apps/api/src/main/java/com/ia/api/user/repository/AssetRepository.java
- apps/api/src/main/java/com/ia/api/user/service/LocalProfilePhotoStorageService.java
- apps/api/src/main/java/com/ia/api/user/service/ProfilePhotoStorageService.java
- apps/web/src/app/app.routes.ts
- apps/web/src/app/features/auth/activate-page.component.html
- apps/web/src/app/features/auth/activate-page.component.scss
- apps/web/src/app/features/auth/activate-page.component.ts
- apps/web/src/app/features/auth/register-page.component.html
- apps/web/src/app/features/auth/register-page.component.scss
- apps/web/src/app/features/auth/register-page.component.spec.ts
- apps/web/src/app/features/auth/register-page.component.ts
- docker-compose.yml

### Change Log

- 2026-03-24: Reprise complète de la story 1.1 sous workflow `bmad-dev` avec upload photo fichier, SMTP réel, activation front/back et couverture de tests.
