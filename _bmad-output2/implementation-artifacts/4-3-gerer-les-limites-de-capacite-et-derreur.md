# Story 4.3: Gerer les limites de capacite et d'erreur

Status: ready-for-dev

## Story

As a utilisateur,
I want etre informe quand une restriction ne peut pas etre appliquee,
so that je sache qu'une action est necessaire.

## Acceptance Criteria

1. Etant donne une contrainte technique empechant une application correcte, quand l'etat de la regle est degrade, alors le systeme remonte une erreur comprehensible.
2. Une action de resolution est proposee.
3. Les erreurs sont journalisees pour support et diagnostic.
4. Le dashboard doit distinguer blocage actif et blocage indisponible.

## Tasks / Subtasks

- [ ] Definir les etats degradés possibles
- [ ] Logger les erreurs cote iOS et backend
- [ ] Exposer les erreurs dans l'API de statut
- [ ] Afficher les actions de resolution cote Angular
- [ ] Tester les principaux cas degradés

## Dev Notes

- Story essentielle pour eviter les fausses promesses produit.

### Project Structure Notes

- Backend: `audit-service`, `device-service`
- Frontend: system status / dashboard warnings
- iOS natif: error reporting

### References

- [product-brief-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\product-brief-focuslock-ios-2026-03-28.md)
- [architecture-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\architecture-focuslock-ios-2026-03-28.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story derivee du backlog BMAD

### Completion Notes List

- Prete pour implementation

### File List

- C:\Dev\blocker\_bmad-output\implementation-artifacts\4-3-gerer-les-limites-de-capacite-et-derreur.md

