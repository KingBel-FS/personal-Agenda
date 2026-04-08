# Story 4.2: Afficher un shield personnalise

Status: ready-for-dev

## Story

As a utilisateur,
I want voir un ecran de blocage clair,
so that je comprenne pourquoi l'acces est refuse.

## Acceptance Criteria

1. Etant donne une cible restreinte ouverte apres depassement ou pendant un blocage, quand l'acces est refuse, alors un shield FocusLock est affiche.
2. Le nom de la regle et le contexte du blocage sont visibles.
3. Le design du shield doit respecter la direction UX sobre et adulte.
4. Si un override est autorise, le shield doit exposer l'action secondaire prevue.

## Tasks / Subtasks

- [ ] Definir le contenu et la structure du shield
- [ ] Implementer l'ecran de blocage cote iOS
- [ ] Harmoniser le design avec Angular
- [ ] Afficher les metadonnees de la regle
- [ ] Tester blocage nominal et variantes de contexte

## Dev Notes

- Story tres visible produit.
- Prioriser clarte, fermete et apaisement.

### Project Structure Notes

- iOS natif: shield UI
- Backend: lecture des regles/evenements utiles
- Frontend: design references, eventual preview state

### References

- [ux-design-specification.md](C:\Dev\blocker\_bmad-output\planning-artifacts\ux-design-specification.md)
- [epics-and-stories-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\epics-and-stories-focuslock-ios-2026-03-28.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story derivee du backlog BMAD

### Completion Notes List

- Prete pour implementation

### File List

- C:\Dev\blocker\_bmad-output\implementation-artifacts\4-2-afficher-un-shield-personnalise.md

