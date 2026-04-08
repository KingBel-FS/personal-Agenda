# Story 5.5b: Redesign exports — Excel stylé + PDF avec graphiques

Status: review

## Story

En tant qu'utilisateur,
je veux exporter mes données en Excel (tableaux stylés multi-feuilles) ou en PDF (avec graphiques et diagrammes),
afin d'analyser et partager mon suivi d'habitudes de façon professionnelle.

## Acceptance Criteria

1. Le format CSV est remplacé par EXCEL (XLSX) avec plusieurs feuilles stylées et des en-têtes colorées.
2. Le PDF contient des tableaux structurés ET des graphiques (camembert statuts, histogramme journalier).
3. La navigation vers /exports est restaurée (sidebar desktop + menu mobile).
4. Les tests @Disabled sont réactivés et mis à jour pour les nouveaux formats.

## Tasks / Subtasks

- [x] Ajouter Apache POI (poi-ooxml 5.3.0) + JFreeChart (1.5.5) dans pom.xml (AC: 1, 2)
- [x] Réécrire ExportService : buildExcel (multi-feuilles stylées) + buildPdf enrichi (tables + graphiques) (AC: 1, 2)
- [x] Mettre à jour ExportRequest (EXCEL|PDF), ExportService content-type et extension (AC: 1)
- [x] Mettre à jour les tests exports : supprimer @Disabled, adapter au format EXCEL (AC: 4)
- [x] Mettre à jour le frontend : format EXCEL, labels, navigation restaurée (AC: 3)

## Dev Notes

- Bibliothèque PDF existante : `com.github.librepdf:openpdf` (classes `com.lowagie.text.*`)
- Apache POI : `XSSFWorkbook` pour XLSX multi-feuilles
- JFreeChart : `ChartFactory`, `ChartUtils` pour générer PNG embarqué dans le PDF
- Le PDF utilise `PdfPTable` (openpdf) pour les tableaux structurés
- Content-type EXCEL : `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Headless mode requis pour JFreeChart en Docker : `System.setProperty("java.awt.headless", "true")`
- Le frontend utilise déjà `responseType: 'blob'` donc compatible avec XLSX et PDF

## Dev Agent Record

### Agent Model Used
Claude Sonnet 4.6

### Debug Log References

### Completion Notes List
- AC1 : Export EXCEL (XLSX) via Apache POI XSSFWorkbook. 4 feuilles : Résumé (stats clés), Tâches, Historique, Objectifs. En-têtes bold/colorées (teal), lignes alternées, auto-filter, freeze row, colonnes ajustées.
- AC2 : PDF via openpdf (PdfPTable pour les tableaux) + JFreeChart (camembert statuts + histogramme taux complétion 14 derniers jours). PDF en format A4 paysage.
- AC3 : Navigation export restaurée dans sidebar desktop et menu mobile (app-shell.component.html).
- AC4 : @Disabled supprimé sur ExportServiceTest et ExportControllerTest. 6 tests ExportServiceTest + 4 tests ExportControllerTest. CSV rejeté par validation (test dédié).

### File List
- apps/api/pom.xml (modified — ajout poi-ooxml 5.3.0 + jfreechart 1.5.5)
- apps/api/src/main/java/com/ia/api/export/api/ExportRequest.java (modified — EXCEL|PDF)
- apps/api/src/main/java/com/ia/api/export/service/ExportService.java (modified — refonte complète)
- apps/api/src/test/java/com/ia/api/export/service/ExportServiceTest.java (modified — @Disabled retiré, 6 tests)
- apps/api/src/test/java/com/ia/api/export/api/ExportControllerTest.java (modified — @Disabled retiré, 4 tests)
- apps/web/src/app/features/exports/exports-api.service.ts (modified — types EXCEL)
- apps/web/src/app/features/exports/exports-page.component.ts (modified — format EXCEL par défaut)
- apps/web/src/app/features/exports/exports-page.component.html (modified — option Excel .xlsx)
- apps/web/src/app/core/app-shell.component.html (modified — nav exports restaurée desktop + mobile)

### Change Log
- 2026-04-06 : Remplacement CSV→EXCEL (Apache POI multi-feuilles), PDF enrichi (tables + 2 graphiques JFreeChart), navigation restaurée, tests réactivés. 125/125 tests verts.
