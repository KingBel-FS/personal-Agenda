# Story 3.1: Creer une limite quotidienne par app

Status: ready-for-dev

## Story

As a utilisateur,
I want definir Instagram a 30 minutes par jour,
so that mon usage reste sous controle.

## Acceptance Criteria

1. Etant donne une app cible selectionnee, quand l'utilisateur enregistre une limite de 30 minutes, alors la regle est stockee cote backend.
2. L'app iOS synchronise et applique la regle.
3. L'UI doit afficher la limite configuree et son etat.
4. Les erreurs de synchro ou d'application doivent etre visibles.

## Tasks / Subtasks

- [ ] Definir le type de regle `daily_limit`
- [ ] Sauvegarder la regle via Spring
- [ ] Synchroniser la regle vers iOS
- [ ] Afficher la regle dans Angular
- [ ] Tester creation, edition simple et propagation

## Dev Notes

- Premiere vraie valeur utilisateur.
- Priorite a la fiabilite et la lisibilite avant les options avancees.

### Project Structure Notes

- Backend: `rule-service`
- Frontend: `features/rules`, `features/dashboard`
- iOS natif: monitoring/apply limit

### References

- [prd-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\prd-focuslock-ios-2026-03-28.md)
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

- C:\Dev\blocker\_bmad-output\implementation-artifacts\3-1-creer-une-limite-quotidienne-par-app.md

