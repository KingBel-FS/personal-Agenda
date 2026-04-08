# Story 3.3: Prevenir avant depassement de limite

Status: ready-for-dev

## Story

As a utilisateur,
I want etre averti avant d'etre bloque,
so that je puisse sortir proprement de l'app.

## Acceptance Criteria

1. Etant donne une limite quotidienne active, quand l'utilisateur approche du seuil defini, alors une notification ou un feedback visible est emis.
2. L'evenement est journalise.
3. Le seuil d'alerte doit etre deterministe et configurable par produit si besoin.
4. Le message doit rester adulte et non culpabilisant.

## Tasks / Subtasks

- [ ] Definir la logique de pre-alerte
- [ ] Emettre la notification ou l'alerte locale
- [ ] Journaliser l'evenement cote backend
- [ ] Afficher l'etat presque atteint dans Angular
- [ ] Tester le declenchement et la journalisation

## Dev Notes

- Story utile pour reduire la frustration au moment du blocage.

### Project Structure Notes

- Backend: `usage-service`, `notification-service`
- Frontend: `dashboard`
- iOS natif: monitoring and alerts

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

- C:\Dev\blocker\_bmad-output\implementation-artifacts\3-3-prevenir-avant-depassement-de-limite.md

