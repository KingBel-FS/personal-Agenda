# Story 5.3: Afficher les insights hebdomadaires

Status: ready-for-dev

## Story

As a utilisateur,
I want comprendre mes tendances d'usage,
so that j'ajuste mes regles intelligemment.

## Acceptance Criteria

1. Etant donne des evenements d'usage synchronises, quand l'utilisateur consulte Insights, alors il voit les principales distractions, l'evolution hebdomadaire et les jours reussis.
2. Aucun detail sensible inutile n'est expose.
3. Les visualisations doivent etre lisibles sur mobile.
4. Les donnees doivent etre coherentes avec le journal d'evenements collecte.

## Tasks / Subtasks

- [ ] Definir l'aggregation hebdomadaire cote backend
- [ ] Exposer l'API d'insights
- [ ] Construire la page Angular Insights
- [ ] Gerer les cas sans historique
- [ ] Tester la coherence des aggregations et du rendu

## Dev Notes

- Story a valeur forte pour retention et ajustement des regles.
- Garder un niveau de detail sobre et respectueux de la vie privee.

### Project Structure Notes

- Backend: `usage-service`
- Frontend: `features/insights` ou extension de `dashboard`

### References

- [product-brief-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\product-brief-focuslock-ios-2026-03-28.md)
- [ux-design-specification.md](C:\Dev\blocker\_bmad-output\planning-artifacts\ux-design-specification.md)
- [architecture-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\architecture-focuslock-ios-2026-03-28.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story derivee du backlog BMAD

### Completion Notes List

- Prete pour implementation

### File List

- C:\Dev\blocker\_bmad-output\implementation-artifacts\5-3-afficher-les-insights-hebdomadaires.md

