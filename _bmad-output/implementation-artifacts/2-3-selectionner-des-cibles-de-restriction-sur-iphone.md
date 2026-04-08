# Story 2.3: Selectionner des cibles de restriction sur iPhone

Status: ready-for-dev

## Story

As a utilisateur,
I want choisir les apps, categories et domaines web a limiter,
so that je configure mes distractions reelles.

## Acceptance Criteria

1. Etant donne l'autorisation Apple accordee, quand l'utilisateur ouvre le selecteur de cibles, alors il peut choisir des apps, categories ou domaines supportes.
2. La selection doit etre sauvegardee dans une regle.
3. Les cibles non supportees doivent etre exclues ou clairement marquees.
4. Le parcours doit rester simple depuis l'onboarding ou l'ecran regles.

## Tasks / Subtasks

- [ ] Integrer les selecteurs iOS pertinents
- [ ] Definir la representation backend des cibles
- [ ] Ajouter le parcours UI de selection
- [ ] Sauvegarder la selection dans le modele de regle
- [ ] Tester la persistence et la restitution de la selection

## Dev Notes

- Story charniere entre permission et moteur de regles.

### Project Structure Notes

- iOS natif: target selection
- Backend: `rule-service`
- Frontend: `features/rules`

### References

- [prd-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\prd-focuslock-ios-2026-03-28.md)
- [architecture-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\architecture-focuslock-ios-2026-03-28.md)
- https://developer.apple.com/documentation/familycontrols/familyactivitypicker

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story derivee du backlog BMAD

### Completion Notes List

- Prete pour implementation

### File List

- C:\Dev\blocker\_bmad-output\implementation-artifacts\2-3-selectionner-des-cibles-de-restriction-sur-iphone.md

