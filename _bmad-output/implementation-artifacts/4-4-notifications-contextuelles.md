# Story 4.4: Notifications contextuelles de streak, anniversaire et objectifs

Status: review

## Story

As a user,
I want secondary reminders to reflect my real progress and context,
so that the app stays helpful rather than noisy.

## Acceptance Criteria

1. Le worker génère la notif de streak à 20h si applicable.
2. Le worker génère les rappels anniversaire, objectifs hebdo et mensuels.
3. Les subscriptions invalides sont purgées.
4. Les notifications sont visibles dans le centre in-app.

## Tasks / Subtasks

- [x] Implémenter les règles métier d'éligibilité aux rappels contextuels (AC: 1, 2)
- [x] Étendre le worker pour générer ces notifications (AC: 1, 2)
- [x] Purger les subscriptions expirées ou révoquées après échec provider (AC: 3)
- [x] Enregistrer ces notifications dans le centre in-app (AC: 4)
- [x] Tester conditions positives et négatives des différents rappels (AC: 1, 2, 3)

## Dev Notes

- La notif streak de 20h dépend d'une streak active et d'aucune tâche exécutée ce jour. [Source: [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)]
- Les rappels ne doivent pas dégrader la compréhension utilisateur ni devenir intrusifs. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]
- Les objectifs hebdo/mensuels (FR42-FR44) dépendent de la story 5-3 qui crée les tables de goals. L'infra de notification est en place — les notifications WEEKLY_GOAL_UNMET / MONTHLY_GOAL_UNMET se déclencheront quand 5-3 sera implémentée.

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [prd.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/prd.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used
Claude Opus 4.6

### Debug Log References
- notification_jobs.occurrence_id est NOT NULL (FK) — impossible de l'utiliser pour les notifs contextuelles (pas liées à une occurrence). Solution : ContextualNotificationService envoie directement via WebPushSender + insère dans notification_center
- Dedup via NOT EXISTS sur notification_center (notification_type + created_at::date) pour éviter les doublons
- Birthday query : LEFT JOIN day_profiles pour récupérer wake_up_time par day_category, avec fallback 08:00 si pas de profil
- Tests : refactoré checkAndSend(LocalDate, LocalTime) package-private pour contrôler le temps dans les tests

### Completion Notes List
- **Task 1 (Règles métier)** :
  - Streak danger : streak_active=true AND current_streak>0 AND aucune task done aujourd'hui → notif à 20h Paris
  - Anniversaire : birth_date month/day match today → notif 1h après wake_up_time du profil jour
  - Objectifs : infra prête (notification_type WEEKLY_GOAL_UNMET/MONTHLY_GOAL_UNMET) mais activation dépend de story 5-3
- **Task 2 (Worker)** : Nouveau ContextualNotificationService avec @Scheduled(fixedDelay=60s). Vérifie streak danger + birthday à chaque cycle.
- **Task 3 (Purge subscriptions)** : WebPushSender.sendWithStatus() retourne le code HTTP. NotificationSendingService et ContextualNotificationService révoquent les subscriptions sur 404/410. Supprimé le stub isSubscriptionGone().
- **Task 4 (Notification center)** : Chaque notif contextuelle insère dans notification_center avec le bon notification_type (STREAK_DANGER, BIRTHDAY). Visible dans le centre in-app existant.
- **Task 5 (Tests)** : 7 tests unitaires couvrant : streak danger avant/après 20h, candidat avec/sans subscription, subscription morte révoquée, anniversaire avant/après wake+1h, pas de candidats.

### File List
- `apps/worker/src/main/java/com/ia/worker/notification/ContextualNotificationService.java` — NEW : service @Scheduled pour streak danger + birthday
- `apps/worker/src/main/java/com/ia/worker/notification/WebPushSender.java` — Ajout sendWithStatus() retournant le code HTTP
- `apps/worker/src/main/java/com/ia/worker/notification/NotificationSendingService.java` — Migré vers sendWithStatus(), revoke sur 404/410, supprimé isSubscriptionGone()
- `apps/worker/src/main/resources/application.properties` — Ajout config contextual.notification.*
- `apps/worker/src/test/java/com/ia/worker/notification/ContextualNotificationServiceTest.java` — NEW : 7 tests
- `apps/api/src/main/resources/application.properties` — Supprimé hibernate.jdbc.time_zone (fix timezone 4.2)

## Change Log
- 2026-03-26: Story implemented — streak danger 20h, birthday reminder, subscription purge on 404/410, notification center recording. Goals deferred to 5-3.
