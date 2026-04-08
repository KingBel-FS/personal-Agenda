# Story 5.1: Agenda semaine/mois avec indicateurs de jour et consultation historique

Status: review

## Story

As a user,  
I want to navigate my agenda visually,  
so that I can inspect past and future activity at different granularities.

## Acceptance Criteria

1. Les vues semaine et mois sont disponibles en lundi-premier.
2. Chaque jour affiche categorie et statut via un indicateur visuel.
3. Les occurrences passees restent consultables integralement. Sur le passe, seules l'heure et le statut d'execution restent modifiables.
4. L'agenda reste accessible clavier et lecteur d'ecran.

## Tasks / Subtasks

- [x] Creer les endpoints agenda semaine/mois cote API (AC: 1, 2, 3)
- [x] Implementer les vues agenda Angular et `DayIndicator` (AC: 1, 2)
- [x] Encadrer l'edition sur les occurrences passees dans l'UI et les actions contextuelles (AC: 3)
- [x] Assurer navigation clavier et labels accessibles du calendrier (AC: 4)
- [x] Tester rendu cross-breakpoints et consultation historique (AC: 1, 3, 4)

## Dev Notes

- L'agenda doit se comporter comme une heatmap de vie, pas juste une grille neutre. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `apps/api`: `./mvnw.cmd test`
- `apps/web`: `npm test -- --watch=false --browsers=ChromeHeadless`
- `apps/web`: `npm run build`
- `repo`: `docker compose up --build -d api web`

### Completion Notes List

- Ajout de `AgendaController` et `AgendaService` pour exposer des plages semaine/mois lundi-premier avec synthese quotidienne, categorie de jour et tonalite de statut.
- Creation de la page Angular `/agenda` avec bascule semaine/mois, `DayIndicator`, panneau de detail par jour et consultation historique.
- Navigation clavier ajoutee directement sur les cellules visibles du calendrier via fleches, labels `aria-label` detailles et focus stable.
- Liaison agenda vers `/daily?date=...` pour ouvrir une journee depuis l'agenda.
- Ajustement metier applique apres validation terrain: les jours passes ouverts depuis l'agenda autorisent maintenant l'appui long, mais uniquement pour changer l'heure et le statut d'execution d'une occurrence.
- Validation reelle effectuee: API `104` tests OK, front `39` tests OK, build Angular OK, redeploiement Docker `api` + `web` OK.

### File List

- apps/api/src/main/java/com/ia/api/agenda/AgendaController.java
- apps/api/src/main/java/com/ia/api/agenda/AgendaDaySummary.java
- apps/api/src/main/java/com/ia/api/agenda/AgendaRangeResponse.java
- apps/api/src/main/java/com/ia/api/agenda/AgendaService.java
- apps/api/src/main/java/com/ia/api/today/OccurrenceStatusService.java
- apps/api/src/test/java/com/ia/api/agenda/AgendaControllerTest.java
- apps/api/src/test/java/com/ia/api/agenda/AgendaServiceTest.java
- apps/web/src/app/app.routes.ts
- apps/web/src/app/core/app-shell.component.html
- apps/web/src/app/features/agenda/agenda-api.service.ts
- apps/web/src/app/features/agenda/agenda-page.component.html
- apps/web/src/app/features/agenda/agenda-page.component.scss
- apps/web/src/app/features/agenda/agenda-page.component.spec.ts
- apps/web/src/app/features/agenda/agenda-page.component.ts
- apps/web/src/app/features/agenda/day-indicator.component.html
- apps/web/src/app/features/agenda/day-indicator.component.scss
- apps/web/src/app/features/agenda/day-indicator.component.ts
- apps/web/src/app/features/today/daily-view-page.component.html
- apps/web/src/app/features/today/daily-view-page.component.ts

### Change Log

- 2026-03-27: consultation historique agenda ajustee avec edition limitee de l'heure et du statut sur les occurrences passees.
