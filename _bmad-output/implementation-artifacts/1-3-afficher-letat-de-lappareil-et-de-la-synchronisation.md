# Story 1.3: Afficher l'etat de l'appareil et de la synchronisation

Status: ready-for-dev

## Story

As a utilisateur,
I want voir si mon iPhone est bien relie et autorise,
so that je sache si mes blocages sont reellement actifs.

## Acceptance Criteria

1. Etant donne un utilisateur connecte, quand il ouvre le dashboard, alors il voit un indicateur de connexion appareil et de permissions.
2. Un etat degrade explicite doit etre affiche si l'enforcement n'est pas disponible.
3. Le backend doit exposer un statut appareil/synchronisation exploitable par Angular.
4. Les etats minimums doivent couvrir: non lie, permission manquante, actif, synchro en erreur.

## Tasks / Subtasks

- [ ] Definir le modele `Device`
- [ ] Exposer un endpoint de statut systeme/appareil
- [ ] Afficher le badge d'etat dans le shell Angular
- [ ] Afficher les cas degradés et les actions de resolution
- [ ] Tester les variations d'etat principales

## Dev Notes

- Story critique pour la transparence produit.
- L'etat appareil doit etre visible tot dans l'UX, pas cache en settings.

### Project Structure Notes

- Backend: `device-service`
- Frontend: `core/device`, `shared/status-badge`

### References

- [ux-design-specification.md](C:\Dev\blocker\_bmad-output\planning-artifacts\ux-design-specification.md)
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

- C:\Dev\blocker\_bmad-output\implementation-artifacts\1-3-afficher-letat-de-lappareil-et-de-la-synchronisation.md

