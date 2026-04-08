# Story 2.1: Demander l'autorisation Family Controls

Status: ready-for-dev

## Story

As a utilisateur iPhone,
I want autoriser FocusLock a gerer mes restrictions,
so that l'app puisse appliquer les limites.

## Acceptance Criteria

1. Etant donne l'app iOS installee, quand l'utilisateur lance l'onboarding d'autorisation, alors la demande Apple est declenchee.
2. Le statut d'autorisation doit etre remonte au backend.
3. L'UX doit expliquer avant la demande pourquoi cette permission est necessaire.
4. Les cas refuses ou indisponibles doivent etre traites sans ambiguité.

## Tasks / Subtasks

- [ ] Mettre en place la demande d'autorisation iOS
- [ ] Remonter le statut au backend Spring
- [ ] Mettre a jour l'UI Angular avec l'etat retourne
- [ ] Ajouter le traitement des refus et indisponibilites
- [ ] Tester le flux d'autorisation nominal et degrade

## Dev Notes

- Story native iOS indispensable.
- Cadrer soigneusement les libelles pour l'App Store et l'UX.

### Project Structure Notes

- iOS natif: module permissions/authorization
- Backend: `device-service`
- Frontend: vues d'onboarding et statut

### References

- [prd-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\prd-focuslock-ios-2026-03-28.md)
- [architecture-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\architecture-focuslock-ios-2026-03-28.md)
- https://developer.apple.com/documentation/xcode/configuring-family-controls

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story derivee du backlog BMAD

### Completion Notes List

- Prete pour implementation

### File List

- C:\Dev\blocker\_bmad-output\implementation-artifacts\2-1-demander-lautorisation-family-controls.md

