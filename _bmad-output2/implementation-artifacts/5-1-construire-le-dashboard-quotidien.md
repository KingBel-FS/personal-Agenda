# Story 5.1: Construire le dashboard quotidien

Status: ready-for-dev

## Story

As a utilisateur,
I want voir mon etat du jour en un coup d'oeil,
so that je sache ou j'en suis face a mes limites.

## Acceptance Criteria

1. Etant donne des regles et evenements existants, quand l'utilisateur ouvre l'accueil, alors il voit le temps consomme, les alertes et les prochaines restrictions.
2. Les donnees proviennent de l'API Spring.
3. Le dashboard doit rester lisible avec peu ou beaucoup de regles.
4. Les etats degradés doivent etre visibles sans polluer la lecture.

## Tasks / Subtasks

- [ ] Exposer un endpoint de synthese dashboard
- [ ] Construire les widgets Angular principaux
- [ ] Afficher les prochaines restrictions et alertes
- [ ] Gerer loading, empty state et erreurs
- [ ] Tester le rendu des differents etats

## Dev Notes

- Le dashboard est la surface de pilotage centrale du MVP.

### Project Structure Notes

- Backend: `usage-service`, `rule-service`
- Frontend: `features/dashboard`

### References

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

- C:\Dev\blocker\_bmad-output\implementation-artifacts\5-1-construire-le-dashboard-quotidien.md

