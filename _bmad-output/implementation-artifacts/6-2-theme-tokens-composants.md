# Story 6.2: Thème, tokens visuels et composants UX métier

Status: review

## Story

As a user,  
I want the app to feel consistent and readable in light or dark mode,  
so that my daily usage remains comfortable and recognizable.

## Acceptance Criteria

1. Le produit supporte thème clair/sombre et suivi du thème système.
2. Les couleurs sémantiques restent cohérentes pour jours, statuts et streak.
3. Les composants `TaskCard`, `DayContextHeader`, `StreakFlame`, `DayIndicator`, `NotificationBanner`, `StepForm` sont réutilisables.
4. La hiérarchie visuelle correspond à la direction UX retenue.

## Tasks / Subtasks

- [x] Définir les tokens CSS globaux de thème et sémantique (AC: 1, 2)
- [x] Mettre en place le shell thème Angular et la préférence utilisateur (AC: 1)
- [x] Implémenter les composants métier réutilisables (AC: 3)
- [x] Vérifier cohérence visuelle sur Today, Agenda, Notifications et Profil (AC: 2, 4)
- [x] Tester contrastes et fallback mode sombre (AC: 1, 2)

## Dev Notes

- La palette sémantique distingue jour travaillé, vacances, weekend/férié, exécution, suspension et streak. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `apps/web`: `npm test -- --watch=false --browsers=ChromeHeadless`
- `apps/web`: `npm run build`
- `docker compose up --build -d web`

### Completion Notes List

- Ajout du bouton `Créer une tâche` dans la vue `Aujourd'hui` via un nouveau composant `DayContextHeader`.
- Mise en place de composants métier réutilisables `TaskCard`, `DayContextHeader`, `StreakFlame` et `StepForm`.
- Centralisation de tokens sémantiques pour jours, statuts et streak dans `styles.scss`.
- Le shell suit maintenant le thème système tant qu'aucune préférence manuelle n'est enregistrée.
- `NotificationBanner` a été réaligné sur les tokens globaux sans changement de direction visuelle.

### File List

- `c:/Dev/personal-Agenda/apps/web/src/styles.scss`
- `c:/Dev/personal-Agenda/apps/web/src/app/core/app-shell.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/core/app-shell.component.html`
- `c:/Dev/personal-Agenda/apps/web/src/app/core/app-shell.component.scss`
- `c:/Dev/personal-Agenda/apps/web/src/app/core/notification-banner.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/today/today-page.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/today/today-page.component.html`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/tasks/task-create-page.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/features/tasks/task-create-page.component.html`
- `c:/Dev/personal-Agenda/apps/web/src/app/shared/components/streak-flame.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/shared/components/day-context-header.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/shared/components/task-card.component.ts`
- `c:/Dev/personal-Agenda/apps/web/src/app/shared/components/step-form.component.ts`
