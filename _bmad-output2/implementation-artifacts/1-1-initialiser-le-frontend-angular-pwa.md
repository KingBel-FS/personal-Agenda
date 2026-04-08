# Story 1.1: Initialiser le frontend Angular/PWA

Status: ready-for-dev

## Story

As a utilisateur,
I want ouvrir une interface moderne installable,
so that je puisse gerer mes regles rapidement.

## Acceptance Criteria

1. Etant donne un nouveau visiteur, quand il ouvre le produit, alors l'application charge une landing Angular responsive et un shell d'application coherent.
2. La promesse produit doit indiquer clairement que l'enforcement des blocages sur iPhone depend d'une couche native Apple, et non d'une PWA seule.
3. Le frontend doit etre prepare comme PWA installable avec manifeste, icones, configuration HTTPS et service worker Angular.
4. L'architecture initiale doit prevoir les zones fonctionnelles minimales: landing, onboarding, dashboard placeholder, regles placeholder, parametres placeholder.
5. Le design initial doit respecter la direction UX: ton adulte, visuel editorial-tech sobre, etat iPhone/permissions visible dans la structure de navigation.

## Tasks / Subtasks

- [ ] Initialiser le projet Angular avec le support PWA
- [ ] Construire le shell de navigation principal
- [ ] Creer la landing initiale
- [ ] Creer les placeholders des zones coeur produit
- [ ] Poser les tokens UI et le responsive de base
- [ ] Ajouter les tests de rendu et de routage initiaux

## Dev Notes

- Story de fondation frontend uniquement.
- Ne jamais presenter la PWA comme mecanisme de blocage iPhone autonome.

### Project Structure Notes

- `src/app/core/`
- `src/app/features/landing/`
- `src/app/features/onboarding/`
- `src/app/features/dashboard/`
- `src/app/features/rules/`
- `src/app/features/settings/`
- `src/app/shared/`

### References

- [product-brief-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\product-brief-focuslock-ios-2026-03-28.md)
- [prd-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\prd-focuslock-ios-2026-03-28.md)
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

- C:\Dev\blocker\_bmad-output\implementation-artifacts\1-1-initialiser-le-frontend-angular-pwa.md

