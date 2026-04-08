# Story 2.4: Modification et suppression d'occurrences uniques ou futures

Status: review

## Story

As a user,  
I want to edit or delete one occurrence or all future ones,  
so that I can adapt habits without breaking history.

## Acceptance Criteria

1. L'utilisateur choisit clairement entre occurrence unique et occurrences futures.
2. Les occurrences passees restent consultables en detail, avec modification limitee a l'heure et au statut d'execution. La date, la suppression et les mutations structurelles de serie restent interdites sur le passe.
3. Les overrides et mutations de regle sont persistés selon le scope choisi.
4. Les notifications futures impactees sont recalculees. ⚠️ **PARTIELLEMENT REMPLI** — Les occurrences sources sont recalculees immediatement (✅ AC4a). La couche notifications (push/email) n'existe pas encore dans le codebase : sera implementee en story 4.2. Les occurrences regenerees serviront de source de verite pour ce scheduler. (🔜 AC4b — differe en 4.2)

## Tasks / Subtasks

- [x] Implementer les endpoints API d'edition et suppression scoped (AC: 1, 3)
- [x] Creer la persistence `task_overrides` (AC: 3)
- [x] Ajouter la bottom sheet / modale contextuelle d'arbitrage dans Angular (AC: 1)
- [x] Limiter explicitement les occurrences passees a l'edition de l'heure et du statut (AC: 2)
- [x] Declencher le recalcul des occurrences derivees (AC: 4a ✅)
- [ ] Recalcul notifications push/email apres mutation (AC: 4b 🔜 story 4.2)

## Dev Notes

- La hierarchie occurrence seule vs serie future est visible dans l'UI via une bottom sheet dediee. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]
- Les suppressions et editions restent rejouables sans double-effet: override unique pour une occurrence, split de regle pour le scope futur. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `./mvnw.cmd test` dans `apps/api`
- `./mvnw.cmd test` dans `apps/worker`
- `npm test -- --watch=false --browsers=ChromeHeadless`
- `npm run build`
- `docker compose up --build -d`
- `docker exec personal-agenda-postgres-1 psql -U ia -d ia -c "..."`

### Completion Notes List

- Nouveau changelog Liquibase `013-task-overrides.yaml` pour persister les overrides par occurrence.
- API `GET /api/v1/tasks/occurrences`, `PUT /api/v1/tasks/occurrences/{id}` et `DELETE /api/v1/tasks/occurrences/{id}` ajoutes avec `scope = THIS_OCCURRENCE | THIS_AND_FOLLOWING`.
- Les occurrences passees ne sont plus totalement bloquees: backend et UI autorisent uniquement la mise a jour de l'heure et du statut d'execution, sans toucher a la date, a la suppression ou a la structure de recurrence.
- La page `Gerer les occurrences` bascule automatiquement sur une modale reduite pour le passe, avec consultation complete et edition strictement restreinte.
- Scope `THIS_OCCURRENCE`: creation ou mise a jour d'un `task_override`, conservation de l'historique, et mise a jour immediate de l'occurrence materialisee.
- Scope `THIS_AND_FOLLOWING`: split de serie via cloture de l'ancienne regle et creation d'une nouvelle definition + nouvelle regle a partir de l'occurrence ciblee, pour ne pas retroacter l'historique.
- Recalcul derive: les occurrences futures impactees sont purgees puis regenerees immediatement via `OccurrenceRefreshService`.
- Correction complementaire appliquee apres validation reelle: les nouvelles regles sont aussi materialisees immediatement a la creation cote API, pour que `tasks/manage` ne depenne plus d'un batch differe.
- La couche notifications n'existe pas encore dans le codebase a ce stade (`4.2` non implementee). `2.4` recalcule donc la source de verite des occurrences futures que le scheduler de notifications consommera ensuite.
- Front Angular ajoute une page de gestion dediee avec bottom sheet d'arbitrage, edition scoped et suppression scoped.
- Affinage UX/metier applique ensuite: si le profil de reveil change, les occurrences futures en `WAKE_UP_OFFSET` sont regenerees immediatement; la portee `cette occurrence et les suivantes` est desactivee s'il ne reste plus d'autre occurrence dans la serie.
- L'edition scoped de serie future couvre maintenant aussi la date de fin, les jours hebdomadaires et les types de jours applicables, avec libelles francais cote UI au lieu des codes bruts.
- Le formulaire d'edition preserve bien le mode `decalage reveil` et l'offset existant, y compris pour les offsets negatifs, et les violations metier remontent via toaster explicite en francais.
- Les ecrans de creation et d'edition ont ete harmonises en francais naturel avec accents, sans exposition de codes metier bruts dans les libelles visibles.
- L'edition de serie future permet maintenant de changer explicitement `hebdomadaire` / `mensuelle` dans la modale, en plus des jours de semaine, jours concernes et date de fin.
- La session web survive a `F5` sur les routes protegees grace a la restauration du token d'acces cote client.
- Correctif metier complementaire: une serie recurrente reste editable meme lorsqu'il ne reste plus qu'une occurrence materialisee; l'utilisateur peut ainsi reouvrir la serie en modifiant la date de fin, les jours et les categories applicables a partir de cette derniere occurrence.
- La liste `Gerer les occurrences` est maintenant paginee cote backend avec taille par defaut `10`, selecteur `5/10/25/50/100`, recherche par nom, filtre type de tache, filtre date de fin de serie, filtre horaire ou decalage reveil, et fenetre d'occurrences futures par defaut extensible via filtres de dates.
- Validation Docker executee: migration `task_overrides` appliquee, tables `task_occurrences` et `task_overrides` presentes, `actuator/health = UP`.

### File List

- apps/api/src/main/java/com/ia/api/task/api/DeleteTaskOccurrenceRequest.java
- apps/api/src/main/java/com/ia/api/task/api/TaskController.java
- apps/api/src/main/java/com/ia/api/task/api/TaskOccurrenceListRequest.java
- apps/api/src/main/java/com/ia/api/task/api/TaskOccurrencePageResponse.java
- apps/api/src/main/java/com/ia/api/task/api/TaskMutationScope.java
- apps/api/src/main/java/com/ia/api/task/api/TaskOccurrenceResponse.java
- apps/api/src/main/java/com/ia/api/task/api/UpdateTaskOccurrenceRequest.java
- apps/api/src/main/java/com/ia/api/task/domain/TaskOccurrenceEntity.java
- apps/api/src/main/java/com/ia/api/task/domain/TaskOverrideEntity.java
- apps/api/src/main/java/com/ia/api/task/repository/TaskOccurrenceRepository.java
- apps/api/src/main/java/com/ia/api/task/repository/TaskOverrideRepository.java
- apps/api/src/main/java/com/ia/api/task/service/DayClassificationReadService.java
- apps/api/src/main/java/com/ia/api/task/service/OccurrenceRefreshService.java
- apps/api/src/main/java/com/ia/api/task/service/TaskService.java
- apps/api/src/main/resources/db/changelog/changes/013-task-overrides.yaml
- apps/api/src/main/resources/db/changelog/db.changelog-master.yaml
- apps/api/src/test/java/com/ia/api/task/api/TaskControllerTest.java
- apps/api/src/test/java/com/ia/api/task/service/TaskServiceTest.java
- apps/worker/src/main/java/com/ia/worker/occurrence/OccurrenceMaterializationService.java
- apps/worker/src/main/resources/application.properties
- apps/web/src/app/app.routes.ts
- apps/web/src/app/app.spec.ts
- apps/web/src/app/features/profile/profile-page.component.html
- apps/web/src/app/features/tasks/task-api.service.ts
- apps/web/src/app/features/tasks/task-create-page.component.spec.ts
- apps/web/src/app/features/tasks/task-manage-page.component.html
- apps/web/src/app/features/tasks/task-manage-page.component.scss
- apps/web/src/app/features/tasks/task-manage-page.component.spec.ts
- apps/web/src/app/features/tasks/task-manage-page.component.ts

### Change Log

- 2026-03-24: Implementation complete de la story 2.4 sous workflow `bmad-dev`, avec validation locale et Docker.
- 2026-03-24: Correctif post-validation pour supprimer la latence de visibilite des nouvelles occurrences dans `tasks/manage`.
- 2026-03-24: Correctifs complementaires sur l'edition de series futures, le recalcul apres changement de reveil et la UX responsive/francisee de la modale.
- 2026-03-24: Harmonisation finale des libelles francais, correction des accents, persistance de session au refresh et mise a jour des artefacts BMAD.
- 2026-03-25: Pagination backend et filtres metier ajoutes a `tasks/manage`, avec retour futur-seulement par defaut et extension de fenetre via filtres de dates.
