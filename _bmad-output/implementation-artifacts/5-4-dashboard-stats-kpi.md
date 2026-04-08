# Story 5.4: Dashboard statistiques et KPI comparatifs

Status: review

## Story

As a user,  
I want to analyze my execution history at multiple levels,  
so that I can understand my trends and improve my routine.

## Acceptance Criteria

1. Le dashboard expose stats hebdo, mensuelles, annuelles et globales.
2. Un drill-down par tache est disponible.
3. Les KPI comparent N et N-1 avec visualisation adaptee.
4. Les requetes restent performantes sur les historiques cibles.

## Tasks / Subtasks

- [x] Concevoir les projections stats et les indexes DB requis (AC: 1, 4)
- [x] Implementer les endpoints stats globaux et par tache (AC: 1, 2)
- [x] Construire le dashboard Angular et ses cartes KPI (AC: 1, 3)
- [x] Ajouter les visualisations comparatives et drill-down (AC: 2, 3)
- [x] Mesurer et tester les temps de requete sur jeux de donnees representatifs (AC: 4)

## Dev Notes

- Les stats sont neutres et factuelles, jamais moralisantes. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]
- Les performances DB doivent rester < 50 ms sur operations courantes avec index adaptes. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `apps/api`: `./mvnw.cmd test`
- `apps/web`: `npm test -- --watch=false --browsers=ChromeHeadless`
- `apps/web`: `npm run build`
- `docker compose up --build -d api web`

### Completion Notes List

- Module stats backend ajoute avec agregats hebdo, mensuels, annuels et globaux, plus comparatifs N/N-1.
- Endpoints livres pour le dashboard et le drill-down par tache.
- Dashboard Angular `/stats` livre avec cartes KPI, listes par tache et modale de detail.
- Indexes DB ajoutes sur `task_occurrences` pour stabiliser les lectures analytiques.
- Correctif metier applique apres validation: les comparaisons hebdo, mensuelles et annuelles opposent maintenant des fenetres de meme avancement entre N et N-1, au lieu de comparer une periode partielle a une periode complete.
- Reprise UX ciblee sur `/stats` en respectant la charte web/mobile clair/sombre: cartes sans debordement, modal detail plus stable, contrastes renforces en theme clair et grilles adaptatives.
- Navigation par ancrage ajoutee sur les cartes hebdo, mensuelles et annuelles: l'utilisateur peut remonter periode par periode, voir explicitement la plage affichee et la plage precedente, avec toutes les valeurs a `0` quand la plage est anterieure a la creation du compte.
- Les badges `+N pts` ont ete retires de l'UI au profit d'une comparaison directe `actuelle / precedente`, plus lisible et moins bruitee.
- Reprise majeure ensuite sous `bmad-dev`: la section `Global` a ete remplacee par un axe `Quotidien`, les periodes hebdomadaires sont maintenant de lundi a dimanche, les mois et annees suivent les bornes calendaires pleines, et chaque carte expose des graphiques pertinents (courbe, histogramme, camembert) a partir d'un historique dedie.

### File List

- `apps/api/src/main/java/com/ia/api/stats/api/StatsController.java`
- `apps/api/src/main/java/com/ia/api/stats/api/StatsDashboardResponse.java`
- `apps/api/src/main/java/com/ia/api/stats/api/StatsDeltaResponse.java`
- `apps/api/src/main/java/com/ia/api/stats/api/StatsPeriodResponse.java`
- `apps/api/src/main/java/com/ia/api/stats/api/StatsSnapshotResponse.java`
- `apps/api/src/main/java/com/ia/api/stats/api/StatsTaskDetailResponse.java`
- `apps/api/src/main/java/com/ia/api/stats/api/StatsTaskRecentOccurrenceResponse.java`
- `apps/api/src/main/java/com/ia/api/stats/api/StatsTaskSummaryResponse.java`
- `apps/api/src/main/java/com/ia/api/stats/service/StatsService.java`
- `apps/api/src/main/java/com/ia/api/task/repository/OccurrenceAggregateProjection.java`
- `apps/api/src/main/java/com/ia/api/task/repository/TaskOccurrenceRepository.java`
- `apps/api/src/main/java/com/ia/api/task/repository/TaskStatsProjection.java`
- `apps/api/src/main/resources/db/changelog/changes/027-stats-indexes.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/test/java/com/ia/api/stats/api/StatsControllerTest.java`
- `apps/api/src/test/java/com/ia/api/stats/service/StatsServiceTest.java`
- `apps/web/src/app/app.routes.ts`
- `apps/web/src/app/core/app-shell.component.html`
- `apps/web/src/app/features/stats/stats-api.service.ts`
- `apps/web/src/app/features/stats/stats-page.component.html`
- `apps/web/src/app/features/stats/stats-page.component.scss`
- `apps/web/src/app/features/stats/stats-page.component.spec.ts`
- `apps/web/src/app/features/stats/stats-page.component.ts`
