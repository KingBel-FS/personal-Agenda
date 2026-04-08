# Story 5.2: Autoriser un override avec friction

Status: ready-for-dev

## Story

As a utilisateur,
I want pouvoir contourner exceptionnellement un blocage,
so that le systeme reste utilisable sans devenir trop permissif.

## Acceptance Criteria

1. Etant donne une regle autorisant un override, quand l'utilisateur demande une exception, alors une friction configurable est imposee.
2. L'action est journalisee avec horodatage.
3. Le shield et l'UI doivent clarifier le caractere exceptionnel de l'override.
4. Le systeme doit pouvoir desactiver l'override selon la regle.

## Tasks / Subtasks

- [ ] Definir le modele de politique d'override
- [ ] Implementer la friction choisie
- [ ] Journaliser demande et resolution
- [ ] Afficher la capacite ou non d'override dans l'UI
- [ ] Tester les chemins autorise, refuse et desactive

## Dev Notes

- Cette story touche au coeur du positionnement produit.
- Eviter toute friction gadget; privilegier une friction simple et efficace.

### Project Structure Notes

- Backend: `rule-service`, `usage-service`
- Frontend: `rules`, `dashboard`
- iOS natif: shield actions / override flow

### References

- [prd-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\prd-focuslock-ios-2026-03-28.md)
- [ux-design-specification.md](C:\Dev\blocker\_bmad-output\planning-artifacts\ux-design-specification.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story derivee du backlog BMAD

### Completion Notes List

- Prete pour implementation

### File List

- C:\Dev\blocker\_bmad-output\implementation-artifacts\5-2-autoriser-un-override-avec-friction.md

