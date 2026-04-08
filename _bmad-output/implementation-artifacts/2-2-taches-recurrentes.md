# Story 2.2: Création et validation des tâches récurrentes

Status: review

## Story

As a user,  
I want to define recurring tasks with realistic frequencies,  
so that the app follows my real habits over time.

## Acceptance Criteria

1. Les récurrences hebdomadaires et mensuelles sont configurables.
2. La date de fin optionnelle est supportée.
3. Les règles de planification sont persistées séparément des overrides.
4. Le calcul d'heure effective respecte `Europe/Paris`.

## Tasks / Subtasks

- [x] Étendre le modèle `task_rules` pour les récurrences hebdo/mensuelles (AC: 1, 2, 3)
- [x] Implémenter validation backend des règles incompatibles (AC: 1, 2)
- [x] Étendre le wizard frontend pour les récurrences (AC: 1, 2)
- [x] Implémenter preview serveur des occurrences récurrentes (AC: 4)
- [x] Ajouter tests métier timezone et récurrence (AC: 3, 4)

## Dev Notes

- La séparation `task_definition` / `task_rule` / `task_override` est imposée par l'architecture. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]
- Les récurrences sont une zone à fort risque DST : écrire d'abord les tests. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
