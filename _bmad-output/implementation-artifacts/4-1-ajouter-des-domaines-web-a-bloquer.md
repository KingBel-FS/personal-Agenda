# Story 4.1: Ajouter des domaines web a bloquer

Status: ready-for-dev

## Story

As a utilisateur,
I want saisir des domaines web distractifs,
so that je puisse les rendre inaccessibles selon mes regles.

## Acceptance Criteria

1. Etant donne un utilisateur qui edite une regle, quand il ajoute un ou plusieurs domaines, alors les domaines sont valides et sauvegardes.
2. L'app iOS recupere la configuration applicable.
3. Les entrees invalides doivent etre rejetees avec un message clair.
4. L'UI doit permettre une edition simple de liste de domaines.

## Tasks / Subtasks

- [ ] Concevoir le modele `web_domain`
- [ ] Ajouter validation de domaine cote backend
- [ ] Ajouter l'editeur de domaines cote Angular
- [ ] Synchroniser les domaines vers iOS
- [ ] Tester validation, sauvegarde et restitution

## Dev Notes

- Bien distinguer domaine exact, sous-domaines et categories si le produit l'ajoute plus tard.

### Project Structure Notes

- Backend: `rule-service`
- Frontend: `rules/domain-editor`
- iOS natif: web restrictions integration

### References

- [prd-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\prd-focuslock-ios-2026-03-28.md)
- [architecture-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\architecture-focuslock-ios-2026-03-28.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story derivee du backlog BMAD

### Completion Notes List

- Prete pour implementation

### File List

- C:\Dev\blocker\_bmad-output\implementation-artifacts\4-1-ajouter-des-domaines-web-a-bloquer.md

