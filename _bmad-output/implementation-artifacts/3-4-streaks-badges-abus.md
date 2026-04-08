# Story 3.4: Streaks, badges et protection contre l'abus de suspension

Status: review

## Story

As a user,  
I want my streaks and badges to reflect my real habit history,  
so that motivation remains fair and trustworthy.

## Acceptance Criteria

1. La streak courante et la meilleure streak sont calculées de façon déterministe.
2. Une suspension valide ne casse pas la streak.
3. Les badges sont débloqués sur jalons configurés.
4. Les suspensions trop fréquentes déclenchent une alerte.

## Tasks / Subtasks

- [x] Implémenter le service de calcul de streak et snapshots (AC: 1, 2)
- [x] Implémenter le déblocage de badges et leur persistance (AC: 3)
- [x] Implémenter la règle d'alerte de suspension abusive (AC: 4)
- [x] Exposer les projections streak/badges pour Today et Stats (AC: 1, 3)
- [x] Tester scénarios nominaux, suspension valide, échec passif et cooldown (AC: 1, 2, 3, 4)

## Dev Notes

- La gamification doit rester honnête et non punitive. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]
- Les animations de streak seront branchées plus tard sur des composants dédiés, mais le calcul métier doit déjà être fiable. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used
Claude Opus 4.6

### Debug Log References

### Completion Notes List
- AC1 : StreakService calcule la streak courante en parcourant les jours complétés (done/suspended = OK, missed/planned passé = break). Jours transparents (skipped/canceled uniquement) ne cassent ni ne prolongent la streak
- AC2 : Les suspensions valides comptent comme "OK" dans le calcul — seul l'échec passif (missed, planned non-agi) casse la streak
- AC3 : 7 jalons de badges configurés (3, 7, 14, 30, 60, 100, 365 jours). Persistés en table `badges` avec unicité user+type. Celebration overlay en frontend avec animation pop et auto-dismiss 5s
- AC4 : Détection d'abus par fenêtre glissante de 14 jours. Si ≥ 3 suspensions pour une même tâche → warning ⚠ affiché sur la carte d'occurrence avec pulse animation

### File List
- apps/api/src/main/resources/db/changelog/changes/021-streak-snapshots.yaml (new)
- apps/api/src/main/resources/db/changelog/changes/022-badges.yaml (new)
- apps/api/src/main/resources/db/changelog/db.changelog-master.yaml (modified)
- apps/api/src/main/java/com/ia/api/today/StreakSnapshotEntity.java (new)
- apps/api/src/main/java/com/ia/api/today/BadgeEntity.java (new)
- apps/api/src/main/java/com/ia/api/today/StreakSnapshotRepository.java (new)
- apps/api/src/main/java/com/ia/api/today/BadgeRepository.java (new)
- apps/api/src/main/java/com/ia/api/today/StreakService.java (new)
- apps/api/src/main/java/com/ia/api/today/TodayResponse.java (modified)
- apps/api/src/main/java/com/ia/api/today/TodayOccurrenceItem.java (modified)
- apps/api/src/main/java/com/ia/api/today/TodayService.java (modified)
- apps/web/src/app/features/today/today-api.service.ts (modified)
- apps/web/src/app/features/today/today-page.component.ts (modified)
- apps/web/src/app/features/today/today-page.component.html (modified)
- apps/web/src/app/features/today/today-page.component.scss (modified)
