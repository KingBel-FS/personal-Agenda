# Story 3.2: Creer un blocage horaire recurrent

Status: ready-for-dev

## Story

As a utilisateur,
I want bloquer des apps pendant mes heures de travail,
so that je protege mes plages de concentration.

## Acceptance Criteria

1. Etant donne une cible et des jours selectionnes, quand l'utilisateur configure une plage horaire recurrente, alors le systeme enregistre le planning.
2. L'enforcement iOS s'applique au bon moment.
3. Le dashboard doit rendre visible la prochaine plage de blocage.
4. Les fuseaux horaires doivent etre geres proprement.

## Tasks / Subtasks

- [ ] Definir le type de regle `schedule_block`
- [ ] Gerer recurrence et timezone cote backend
- [ ] Synchroniser le planning vers iOS
- [ ] Afficher les horaires dans Angular
- [ ] Tester les transitions temporelles principales

## Dev Notes

- Attention forte aux modeles temporels et a la timezone utilisateur.

### Project Structure Notes

- Backend: `rule-service`, `usage-service`
- Frontend: `rules`, `dashboard`
- iOS natif: scheduling/device monitoring

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

- C:\Dev\blocker\_bmad-output\implementation-artifacts\3-2-creer-un-blocage-horaire-recurrent.md

