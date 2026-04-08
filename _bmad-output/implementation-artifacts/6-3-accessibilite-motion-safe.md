# Story 6.3: Accessibilité et motion-safe sur les flux critiques

Status: review

## Story

As a user with accessibility needs,  
I want all critical flows to remain operable and understandable,  
so that I can use the product without barriers.

## Acceptance Criteria

1. Les flux onboarding, Today, Agenda, notifications et formulaires sont navigables au clavier.
2. Les contrôles ont des noms accessibles et alternatives textuelles.
3. Les composants critiques respectent contrastes et cibles 44x44.
4. Les animations honorent `prefers-reduced-motion`.

## Tasks / Subtasks

- [x] Auditer les composants et pages critiques contre RGAA/WCAG AA (AC: 1, 2, 3)
- [x] Corriger focus order, aria-labels, alt text et pièges clavier (AC: 1, 2)
- [x] Vérifier contrastes et tailles de cibles (AC: 3)
- [x] Désactiver/réduire animations selon préférence système (AC: 4)
- [x] Ajouter tests automatisés et check manuel lecteur d'écran sur flux critiques (AC: 1, 2, 3, 4)

## Dev Notes

- L'accessibilité est native, non corrective. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]
- L'application vise RGAA 4.1 / WCAG AA complet. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `apps/web`: `npm test -- --watch=false --browsers=ChromeHeadless`
- `apps/web`: `npm run build`
- `docker compose up --build -d web`

### Completion Notes List

- Ajout d'un lien d'évitement global vers le contenu principal.
- Fermeture clavier `Échap` sur les overlays critiques Today, Daily et Agenda.
- Renforcement des noms accessibles et de la navigation clavier sur le centre de notifications et la liste d'occurrences Agenda.
- Uniformisation des tailles minimales de cible à `44x44` pour les contrôles de base.
- Renforcement du mode `prefers-reduced-motion` avec désactivation plus stricte des animations/transitions.
- Ajout d'un test front dédié aux notifications accessibles.

### File List

- `c:/Dev/personal-Agenda/apps/web/src/app/app.html`
- `c:/Dev/personal-Agenda/apps/web/src/styles.scss`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/notifications/notification-center-page.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/notifications/notification-center-page.component.html`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/notifications/notification-center-page.component.scss`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/notifications/notification-center-page.component.spec.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/today/today-page.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/today/daily-view-page.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/today/daily-view-page.component.html`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/agenda/agenda-page.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/agenda/agenda-page.component.html`
