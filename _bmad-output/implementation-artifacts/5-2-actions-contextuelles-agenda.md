# Story 5.2: Actions contextuelles depuis l'agenda

Status: ready-for-dev

## Story

As a user,  
I want to manage tasks and occurrences from the agenda,  
so that I can adjust my schedule in the context of the calendar.

## Acceptance Criteria

1. Depuis l'agenda, l'utilisateur peut créer, éditer et supprimer selon le contexte.
2. Les actions destructives séparent occurrence seule et futures occurrences.
3. Les vues Today et Agenda sont rafraîchies après mutation.
4. Les occurrences passées restent non éditables.

## Tasks / Subtasks

- [ ] Réutiliser/étendre les endpoints tâche et occurrence existants (AC: 1, 2, 4)
- [ ] Implémenter la bottom sheet contextuelle depuis l'agenda (AC: 1, 2)
- [ ] Gérer le refetch et la cohérence des projections Today/Agenda (AC: 3)
- [ ] Tester parcours modaux, annulations, erreurs et cas passés (AC: 2, 4)

## Dev Notes

- Ne pas créer une nouvelle logique CRUD spécifique agenda si les endpoints du domaine existent déjà. Réutiliser. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
