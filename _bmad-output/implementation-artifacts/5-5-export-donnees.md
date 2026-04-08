# Story 5.5: Export des données utilisateur

Status: review

## Story

As a user,  
I want to export my history and task data,  
so that I can keep or analyze my data outside the app.

## Acceptance Criteria

1. L'utilisateur peut demander un export CSV ou PDF.
2. L'export contient les données autorisées selon le scope demandé.
3. Le délai de génération respecte le NFR cible.
4. Le téléchargement est sécurisé et traçable.

## Tasks / Subtasks

- [x] Définir le périmètre exact des exports et les projections associées (AC: 1, 2)
- [x] Implémenter génération CSV côté backend (AC: 1, 2)
- [x] Implémenter génération PDF côté backend ou service dédié léger (AC: 1, 2)
- [x] Créer l'UI de demande et récupération d'export (AC: 1, 4)
- [x] Mesurer performances et journaliser les exports (AC: 3, 4)

## Dev Notes

- Aucun export ne doit exposer des données d'un autre utilisateur ni des URLs privées non signées. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
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

- Export synchrone disponible en `CSV` et `PDF`.
- Scopes exposés : `TASKS`, `HISTORY`, `FULL`.
- Les exports sont bornés par l'utilisateur courant et par la période demandée, sans URL privée signée exposée.
- Chaque export est journalisé dans `export_audits` avec format, scope, dates, statut, volume et durée.
- Une page front `/exports` permet de lancer un export et de consulter l'historique récent.

### File List

- `apps/api/pom.xml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/resources/db/changelog/changes/028-export-audits.yaml`
- `apps/api/src/main/java/com/ia/api/export/api/ExportController.java`
- `apps/api/src/main/java/com/ia/api/export/api/ExportRequest.java`
- `apps/api/src/main/java/com/ia/api/export/api/ExportAuditItem.java`
- `apps/api/src/main/java/com/ia/api/export/api/ExportHistoryResponse.java`
- `apps/api/src/main/java/com/ia/api/export/domain/ExportAuditEntity.java`
- `apps/api/src/main/java/com/ia/api/export/repository/ExportAuditRepository.java`
- `apps/api/src/main/java/com/ia/api/export/service/ExportService.java`
- `apps/api/src/test/java/com/ia/api/export/api/ExportControllerTest.java`
- `apps/api/src/test/java/com/ia/api/export/service/ExportServiceTest.java`
- `apps/web/src/app/app.routes.ts`
- `apps/web/src/app/core/app-shell.component.html`
- `apps/web/src/app/features/exports/exports-api.service.ts`
- `apps/web/src/app/features/exports/exports-page.component.ts`
- `apps/web/src/app/features/exports/exports-page.component.html`
- `apps/web/src/app/features/exports/exports-page.component.scss`
- `apps/web/src/app/features/exports/exports-page.component.spec.ts`

## Suspension Note

- Le 27/03/2026, la navigation export a été retirée et les tests exports ont été désactivés en attente d'un redesign aligné sur les tableaux de statistiques.
