# Story 1.2: Connexion, refresh token et reinitialisation de mot de passe

Status: review

## Story

As an activated user,  
I want to log in, stay signed in securely and recover access if needed,  
so that my account remains usable without weakening security.

## Acceptance Criteria

1. La connexion avec email/mot de passe renvoie un access token et active un flux refresh token securise.
2. Les identifiants invalides renvoient le contrat d'erreur API standardise.
3. La reinitialisation de mot de passe emet un token a usage unique expirant en 1 heure.
4. Le reset invalide les refresh tokens precedents.

## Tasks / Subtasks

- [x] Configurer Spring Security stateless et le filtre JWT (AC: 1, 2)
  - [x] Implementer generation/validation access token
  - [x] Stocker le refresh token hache
- [x] Creer les endpoints de login et refresh (AC: 1)
  - [x] Gerer les erreurs d'authentification avec le contrat d'erreur standard
- [x] Creer le flux reset password (AC: 3)
  - [x] Generer et persister les tokens de reset
  - [x] Emettre l'email securise
- [x] Invalider les refresh tokens apres reset (AC: 4)
  - [x] Revoquer la session active et les sessions derivees
- [x] Ajouter tests unitaires et integration auth/reset (AC: 1, 2, 3, 4)

## Dev Notes

- Access token 15 minutes, refresh token rotatif 30 jours. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]
- Les tokens d'activation et reset sont a usage unique et expirent apres 1 heure. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]
- Utiliser Spring Security comme point d'entree unique, sans logique auth dispersee. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]

### Project Structure Notes

- `apps/api/.../auth` pour controleurs, services, DTO et filtres.
- UI login/reset dans `apps/web/src/app/features/auth/`.

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `./mvnw.cmd test`
- `docker compose up --build -d api`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/password-reset/request`
- `POST /api/v1/auth/password-reset/confirm`
- `http://localhost:8025/api/v1/messages`
- `npm test -- --watch=false --browsers=ChromeHeadless`
- `npm run build`
- `docker compose up --build -d web`

### Completion Notes List

- JWT access token 15 minutes ajoute avec signature HMAC et filtre `Bearer` stateless.
- Flux `login` implemente avec cookie `HttpOnly` de refresh token rotatif 30 jours et stockage hache en base.
- Flux `password-reset/request` et `password-reset/confirm` implementes avec token a usage unique 1 heure et email SMTP securise via Mailpit en Docker.
- La confirmation de reset revoque les refresh tokens actifs precedents; verification faite en base et par appel `refresh` avec ancien cookie.
- Contrat d'erreur auth standardise en `401 AUTHENTICATION_FAILED` pour credentials invalides.
- UI Angular ajoutee pour `/login`, `/forgot-password` et `/reset-password`; le lien "J'ai un lien d'activation" a ete retire de la connexion et remplace par "Mot de passe oublie".
- Deconnexion complete ajoutee avec `POST /api/v1/auth/logout`, suppression du refresh cookie, revocation du refresh token courant et bouton visible depuis `/profile`.
- Le reset est declenche depuis l'email recu et aboutit sur la page `/reset-password?token=...`.
- Suites backend vertes: `20` tests. Suites frontend vertes: `10` tests. Build Angular et conteneur web rebuildes.

### File List

- apps/api/pom.xml
- apps/api/src/main/java/com/ia/api/auth/api/AuthController.java
- apps/api/src/main/java/com/ia/api/auth/api/LoginRequest.java
- apps/api/src/main/java/com/ia/api/auth/api/LoginResponse.java
- apps/api/src/main/java/com/ia/api/auth/api/MessageResponse.java
- apps/api/src/main/java/com/ia/api/auth/api/PasswordResetConfirmRequest.java
- apps/api/src/main/java/com/ia/api/auth/api/PasswordResetRequest.java
- apps/api/src/main/java/com/ia/api/auth/api/RefreshResponse.java
- apps/api/src/main/java/com/ia/api/auth/domain/RefreshTokenEntity.java
- apps/api/src/main/java/com/ia/api/auth/domain/ResetPasswordTokenEntity.java
- apps/api/src/main/java/com/ia/api/auth/repository/RefreshTokenRepository.java
- apps/api/src/main/java/com/ia/api/auth/repository/ResetPasswordTokenRepository.java
- apps/api/src/main/java/com/ia/api/auth/service/AuthService.java
- apps/api/src/main/java/com/ia/api/auth/service/JwtAuthenticationFilter.java
- apps/api/src/main/java/com/ia/api/auth/service/JwtService.java
- apps/api/src/main/java/com/ia/api/auth/service/PasswordResetEmailSender.java
- apps/api/src/main/java/com/ia/api/auth/service/SmtpPasswordResetEmailSender.java
- apps/api/src/main/java/com/ia/api/common/api/GlobalExceptionHandler.java
- apps/api/src/main/java/com/ia/api/common/config/AppBeansConfig.java
- apps/api/src/main/java/com/ia/api/common/config/SecurityConfig.java
- apps/api/src/main/resources/application.properties
- apps/api/src/main/resources/db/changelog/db.changelog-master.yaml
- apps/api/src/main/resources/db/changelog/changes/006-reset-password-tokens.yaml
- apps/api/src/test/java/com/ia/api/auth/api/AuthControllerTest.java
- apps/api/src/test/java/com/ia/api/auth/service/AuthServiceTest.java
- apps/api/src/test/java/com/ia/api/auth/service/JwtServiceTest.java
- apps/api/src/test/java/com/ia/api/auth/service/SmtpPasswordResetEmailSenderTest.java
- apps/web/src/app/app.routes.ts
- apps/web/src/app/features/auth/login-page.component.ts
- apps/web/src/app/features/auth/login-page.component.html
- apps/web/src/app/features/auth/login-page.component.scss
- apps/web/src/app/features/auth/login-page.component.spec.ts
- apps/web/src/app/features/auth/forgot-password-page.component.ts
- apps/web/src/app/features/auth/forgot-password-page.component.html
- apps/web/src/app/features/auth/forgot-password-page.component.scss
- apps/web/src/app/features/auth/forgot-password-page.component.spec.ts
- apps/web/src/app/features/auth/reset-password-page.component.ts
- apps/web/src/app/features/auth/reset-password-page.component.html
- apps/web/src/app/features/auth/reset-password-page.component.scss
- apps/web/src/app/features/auth/reset-password-page.component.spec.ts
- apps/web/src/app/features/profile/profile-api.service.ts
- apps/web/src/app/features/profile/profile-page.component.ts
- apps/web/src/app/features/profile/profile-page.component.html
- apps/web/src/app/features/profile/profile-page.component.scss
- apps/web/src/app/features/profile/profile-page.component.spec.ts

### Change Log

- 2026-03-24: Implementation complete de la story 1.2 sous workflow `bmad-dev` avec JWT, refresh rotatif, reset password, UI login/recovery/reset, logout complet et validations Docker.
