# Story 2.2: Lier l'iPhone au compte utilisateur

Status: ready-for-dev

## Story

As a utilisateur,
I want relier mon iPhone a mon compte,
so that mes regles web et mobile soient synchronisees.

## Acceptance Criteria

1. Etant donne un utilisateur authentifie, quand il termine la liaison appareil, alors un device iOS est cree cote backend.
2. L'appareil apparait comme actif dans le dashboard.
3. La liaison doit etre idempotente et recuperable apres reconnexion.
4. Les erreurs de liaison doivent etre visibles et actionnables.

## Tasks / Subtasks

- [ ] Definir le flux de liaison entre iOS et backend
- [ ] Creer l'enregistrement `Device`
- [ ] Afficher le resultat de liaison cote Angular
- [ ] Gerer les cas de reliaison et conflit simple
- [ ] Tester la creation et la reprise de liaison

## Dev Notes

- Cette story pose la base de synchronisation entre les couches.

### Project Structure Notes

- Backend: `device-service`
- iOS natif: linking/bootstrap
- Frontend: onboarding/device-state

### References

- [architecture-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\architecture-focuslock-ios-2026-03-28.md)
- [epics-and-stories-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\epics-and-stories-focuslock-ios-2026-03-28.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story derivee du backlog BMAD

### Completion Notes List

- Prete pour implementation

### File List

- C:\Dev\blocker\_bmad-output\implementation-artifacts\2-2-lier-liphone-au-compte-utilisateur.md

