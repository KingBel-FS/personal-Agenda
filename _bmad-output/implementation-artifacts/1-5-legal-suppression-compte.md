# Story 1.5: Mentions légales, confidentialité et suppression irréversible du compte

Status: done

## Story

As a user,
I want to access legal information and delete my account if I choose,
so that I retain trust and control over my data.

## Acceptance Criteria

1. Mentions légales et politique de confidentialité sont accessibles depuis l'app.
2. La suppression de compte demande confirmation explicite.
3. La suppression entraîne la révocation de session et la purge des données personnelles dans le délai attendu.

## Tasks / Subtasks

- [x] Ajouter les surfaces légales dans le frontend et l'API de métadonnées si nécessaire (AC: 1)
- [x] Implémenter le workflow de suppression irréversible (AC: 2, 3)
  - [x] Révoquer refresh tokens et subscriptions
  - [x] Planifier purge médias et données dépendantes
- [x] Journaliser proprement l'opération de suppression (AC: 3)
- [x] Tester l'effacement complet et l'impossibilité de réutiliser la session supprimée (AC: 3)

## Dev Notes

- Les données doivent être supprimées sous 30 minutes max. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]
- La suppression de compte doit rester explicite et irréversible. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]

### Project Structure Notes

- `apps/api/.../user`
- `apps/worker/...` si purge asynchrone
- `apps/web/src/app/features/profile/`

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

None.

### Completion Notes List

- Hard deletion (synchronous, within @Transactional) chosen over async worker — RGPD 30-min deadline met without extra complexity.
- Deletion order: vacation_periods → holiday_sync_states → day_profiles → assets (+ physical files) → refresh_tokens → reset_password_tokens → activation_tokens → consents → users.
- Physical photo files deleted via ProfilePhotoStorageService.delete() (new method added to interface + LocalProfilePhotoStorageService).
- Password confirmation required via DELETE /api/v1/profile body { confirmPassword }.
- After deletion Angular clears in-memory token and redirects to /login.
- Legal page (/legal) is publicly accessible (no authGuard).

### File List

- apps/web/src/app/features/legal/legal-page.component.ts (created)
- apps/web/src/app/features/legal/legal-page.component.html (created)
- apps/web/src/app/features/legal/legal-page.component.scss (created)
- apps/web/src/app/app.routes.ts (modified — /legal route added)
- apps/web/src/app/features/auth/login-page.component.html (modified — legal link)
- apps/web/src/app/features/profile/profile-page.component.html (modified — deletion section + legal footer)
- apps/web/src/app/features/profile/profile-page.component.ts (modified — deletionForm + deleteAccount())
- apps/web/src/app/features/profile/profile-api.service.ts (modified — deleteAccount())
- apps/web/src/app/features/profile/profile-page.component.scss (modified — .legal-nav)
- apps/api/src/main/java/com/ia/api/user/api/DeleteAccountRequest.java (created)
- apps/api/src/main/java/com/ia/api/user/service/AccountDeletionService.java (created)
- apps/api/src/main/java/com/ia/api/user/api/ProfileController.java (modified — DELETE /api/v1/profile)
- apps/api/src/main/java/com/ia/api/user/service/ProfilePhotoStorageService.java (modified — delete method)
- apps/api/src/main/java/com/ia/api/user/service/LocalProfilePhotoStorageService.java (modified — delete implementation)
- apps/api/src/main/java/com/ia/api/vacation/repository/VacationPeriodRepository.java (modified — deleteAllByUserId)
- apps/api/src/main/java/com/ia/api/holiday/repository/HolidaySyncStateRepository.java (modified — deleteByUserId)
- apps/api/src/main/java/com/ia/api/user/repository/DayProfileRepository.java (modified — deleteAllByUserId)
- apps/api/src/main/java/com/ia/api/user/repository/AssetRepository.java (modified — findAllByUserId + deleteAllByUserId)
- apps/api/src/main/java/com/ia/api/auth/repository/RefreshTokenRepository.java (modified — deleteAllByUserId)
- apps/api/src/main/java/com/ia/api/auth/repository/ResetPasswordTokenRepository.java (modified — deleteAllByUserId)
- apps/api/src/main/java/com/ia/api/auth/repository/ActivationTokenRepository.java (modified — deleteAllByUserId)
- apps/api/src/main/java/com/ia/api/auth/repository/ConsentRepository.java (modified — deleteAllByUserId)
- apps/api/src/test/java/com/ia/api/user/service/AccountDeletionServiceTest.java (created)
- apps/api/src/test/java/com/ia/api/user/api/ProfileControllerTest.java (modified — 2 new tests)
- apps/web/src/app/features/profile/profile-page.component.spec.ts (modified — 2 new tests)
