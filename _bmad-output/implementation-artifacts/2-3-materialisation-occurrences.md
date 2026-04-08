# Story 2.3: Matérialisation des occurrences et classification des jours

Status: review

## Story

As a user,  
I want the system to prepare the right occurrences for each day automatically,  
so that my day view is always contextualized correctly.

## Acceptance Criteria

1. Le worker matérialise les occurrences sur un horizon glissant.
2. Chaque jour est classifié selon profil, vacances et jours fériés.
3. Les tâches non applicables sont exclues ou marquées `skipped` selon la règle métier.
4. Le calcul est déterministe malgré DST et redémarrages.

## Tasks / Subtasks

- [x] Créer les tables `task_occurrences` et états associés via Liquibase (AC: 1, 3)
- [x] Implémenter le service de classification de jour (AC: 2)
- [x] Implémenter le job worker de matérialisation (AC: 1, 3, 4)
- [x] Ajouter verrous SQL / idempotence pour reprise après redémarrage (AC: 4)
- [x] Tester scénarios vacances, fériés, weekend et DST (AC: 2, 3, 4)

## Dev Notes

- Tous les calculs temps passent par un service unique côté backend. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]
- `skipped` n'est pas `suspended`; ne jamais fusionner les statuts. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `./mvnw.cmd test` dans `apps/api`
- `./mvnw.cmd test` dans `apps/worker`
- `docker compose up --build -d api worker`
- `docker compose logs worker --tail=120`
- `docker exec personal-agenda-postgres-1 psql -U ia -d ia -c "select ... from task_occurrences ..."`

### Completion Notes List

- Tables `task_occurrences` et metadata associees creees via Liquibase pour stocker les occurrences materialisees et leur statut.
- La classification de jour repose sur le profil utilisateur, les vacances et les jours feries synchronises.
- Le worker ne depend plus d'un batch nocturne unique: il reconcilie en continu les occurrences futures sur un delai fixe pour rattraper les ecritures manquees et les redemarrages.
- Le backend API declenche aussi une materialisation immediate apres creation de tache afin qu'une nouvelle regle apparaisse sans attente dans les vues basees sur `task_occurrences`.
- Verification runtime reelle effectuee sur la regle `e50a37d8-d764-4f0b-bc07-4ef718f60037`: une occurrence `2026-03-29 11:00:00 / WEEKEND_HOLIDAY` a bien ete materialisee en base apres redeploiement.

### File List

- apps/api/src/main/java/com/ia/api/task/service/OccurrenceRefreshService.java
- apps/api/src/main/java/com/ia/api/task/service/TaskService.java
- apps/api/src/test/java/com/ia/api/task/service/TaskServiceTest.java
- apps/worker/src/main/java/com/ia/worker/occurrence/OccurrenceMaterializationService.java
- apps/worker/src/main/resources/application.properties
- apps/worker/src/test/java/com/ia/worker/occurrence/OccurrenceMaterializationServiceTest.java

### Change Log

- 2026-03-24: Materialisation renforcee en quasi temps reel avec write-through API et reconciliation continue worker.
