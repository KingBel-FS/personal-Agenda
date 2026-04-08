# Story 3.3: Badge PWA, synchronisation locale et feedback immédiat

Status: review

## Story

As a user,  
I want all immediate counters and indicators to update right after my action,  
so that I never doubt the system state.

## Acceptance Criteria

1. La vue Today et la TaskCard sont rafraîchies immédiatement après mutation acceptée.
2. Le badge PWA montre le nombre de tâches restantes.
3. Les caches côté client sont invalidés proprement.
4. Les feedbacks utilisent toast et live-region accessibles.

## Tasks / Subtasks

- [x] Implémenter retour de mutation riche côté API (AC: 1)
- [x] Mettre à jour le badge PWA et le shell Angular (AC: 2)
- [x] Gérer invalidation cache / refetch ciblé (AC: 3)
- [x] Implémenter les toasts et annonces live regions (AC: 4)
- [x] Tester timing < 1s et comportements multi-onglets simples (AC: 1, 2, 3)

## Dev Notes

- Le feedback sans latence est un principe UX non négociable. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]
- Le badge PWA doit se mettre à jour en moins d'une seconde. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used
Claude Opus 4.6

### Debug Log References

### Completion Notes List
- AC1 : Les 3 endpoints POST (complete/miss/suspend) retournent déjà un TodayResponse complet — la vue Today se rafraîchit immédiatement via `applyData()` centralisé
- AC2 : BadgeService créé — utilise `navigator.setAppBadge(activeCount)` après chaque mutation et chargement initial
- AC3 : Pas de cache stale — mutations retournent la data fraîche. Ajout `visibilitychange` listener pour refetch silencieux quand l'onglet reprend le focus (multi-onglets)
- AC4 : Live-region persistante `aria-live="polite"` toujours dans le DOM + toast visuel `role="status"`. Les lecteurs d'écran annoncent chaque changement de statut

### File List
- apps/web/src/app/core/badge.service.ts (new)
- apps/web/src/app/features/today/today-page.component.ts (modified)
- apps/web/src/app/features/today/today-page.component.html (modified)
- apps/web/src/app/features/today/today-page.component.scss (modified)
