# Story 3.1: Vue Aujourd'hui et contexte de journée

Status: ready-for-dev

## Story

As a user,  
I want to open the app and instantly understand today's context,  
so that I know what matters now without searching.

## Acceptance Criteria

1. Les occurrences du jour sont ordonnées par heure effective.
2. Le header de contexte affiche type de jour, tâches actives, tâches skippées et progression.
3. La navigation principale 4 onglets est présente.
4. La vue respecte le design mobile-first cockpit contextuel.

## Tasks / Subtasks

- [ ] Exposer l'endpoint agrégé Today côté API (AC: 1, 2)
- [ ] Implémenter `DayContextHeader` et la page Today Angular (AC: 2, 4)
- [ ] Implémenter la bottom navigation 4 onglets (AC: 3)
- [ ] Ajouter skeleton/loading et accessibilité de base (AC: 4)
- [ ] Tester responsive mobile/tablette et ordre de focus (AC: 4)

## Dev Notes

- La vue Aujourd'hui est l'écran héros du produit. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]
- Ne pas recalculer la journée côté Angular ; consommer une projection API. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
