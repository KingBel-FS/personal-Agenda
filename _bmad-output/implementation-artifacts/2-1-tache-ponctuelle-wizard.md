# Story 2.1: Création d'une tâche ponctuelle via wizard progressif

Status: review

## Story

As a user,
I want to create a one-time task through a guided form,
so that I can schedule a task without dealing with raw complexity.

## Acceptance Criteria

1. Le formulaire suit un wizard progressif conforme à la spec UX.
2. Il capture titre, icône, photo optionnelle, description, catégories de jours, jour sélectionné, mode horaire et date de début.
3. Les dates passées sont refusées côté frontend et backend.
4. Un aperçu de la prochaine occurrence est affiché avant confirmation.

## Tasks / Subtasks

- [x] Créer les tables minimales `task_definitions`, `task_rules`, `assets` complémentaires si besoin (AC: 2)
- [x] Implémenter les endpoints API de création de tâche ponctuelle (AC: 2, 3)
- [x] Créer le `StepForm` Angular pour la création de tâche (AC: 1, 2)
- [x] Ajouter l'aperçu de prochaine occurrence via endpoint de preview backend (AC: 4)
- [x] Tester validations, preview et accessibilité clavier/lecteur d'écran (AC: 1, 3, 4)

## Dev Notes

- Le wizard progressif est un composant UX de premier ordre. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]
- La règle métier de temps reste serveur ; le frontend affiche une preview mais ne décide pas seul. [Source: [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)]

### Project Structure Notes

- `apps/api/.../task`
- `apps/web/src/app/features/tasks/`
- composant partagé possible sous `features/tasks/components/step-form`

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [architecture.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/architecture.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

None.

### Completion Notes List

- Migration 010 (changesets 015-016) ajoute `task_definitions` et `task_rules`
- TaskService valide que startDate >= today (Europe/Paris) cote backend
- Preview endpoint calcule l'heure d'occurrence serveur-side : FIXED=heure directe, WAKE_UP_OFFSET=reveil+offset depuis le day profile
- Wizard 5 etapes : info → photo → planification → horaire → apercu+confirmation
- Route `/tasks/new` protegee par authGuard ; lien depuis la page profil
- Tests unitaires : TaskServiceTest (4 tests), TaskControllerTest (3 tests), TaskCreatePageComponent spec (5 tests)

### File List

- `apps/api/src/main/resources/db/changelog/changes/010-task-definitions.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/java/com/ia/api/task/domain/TaskDefinitionEntity.java`
- `apps/api/src/main/java/com/ia/api/task/domain/TaskRuleEntity.java`
- `apps/api/src/main/java/com/ia/api/task/repository/TaskDefinitionRepository.java`
- `apps/api/src/main/java/com/ia/api/task/repository/TaskRuleRepository.java`
- `apps/api/src/main/java/com/ia/api/task/api/CreateTaskRequest.java`
- `apps/api/src/main/java/com/ia/api/task/api/TaskPreviewRequest.java`
- `apps/api/src/main/java/com/ia/api/task/api/TaskResponse.java`
- `apps/api/src/main/java/com/ia/api/task/api/TaskPreviewResponse.java`
- `apps/api/src/main/java/com/ia/api/task/service/TaskService.java`
- `apps/api/src/main/java/com/ia/api/task/api/TaskController.java`
- `apps/api/src/test/java/com/ia/api/task/api/TaskControllerTest.java`
- `apps/api/src/test/java/com/ia/api/task/service/TaskServiceTest.java`
- `apps/web/src/app/features/tasks/task-api.service.ts`
- `apps/web/src/app/features/tasks/task-create-page.component.ts`
- `apps/web/src/app/features/tasks/task-create-page.component.html`
- `apps/web/src/app/features/tasks/task-create-page.component.scss`
- `apps/web/src/app/features/tasks/task-create-page.component.spec.ts`
- `apps/web/src/app/app.routes.ts`
- `apps/web/src/app/features/profile/profile-page.component.html`
- `apps/web/src/app/features/profile/profile-page.component.scss`
