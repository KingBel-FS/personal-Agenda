# Story 5.6 : Heure de réveil custom par jour

Status: done

## Story

En tant qu'utilisateur,
je veux définir une heure de réveil spécifique pour un jour futur (ou le jour même avant 4h du matin),
afin que toutes mes tâches en mode WAKE_UP_OFFSET soient recalculées automatiquement pour ce jour.

## Acceptance Criteria

1. Un bouton "Réveil du jour" est disponible dans le panneau détail de l'agenda (desktop) et dans la vue journalière (mobile).
2. Un bottom-sheet permet de saisir une heure de réveil custom pour la date sélectionnée.
3. Si un override existe déjà, il est affiché pré-rempli et supprimable.
4. Seuls les jours futurs sont modifiables ; le jour même est modifiable uniquement avant 4h00 heure locale.
5. À la sauvegarde, toutes les occurrences WAKE_UP_OFFSET du jour sont recalculées et mises à jour en base.
6. À la suppression de l'override, les occurrences WAKE_UP_OFFSET reviennent au réveil du profil (dayProfile).
7. Un événement de sync SSE est publié après recalcul pour notifier le frontend.

## Tasks / Subtasks

- [x] Migration Liquibase : table `user_day_wake_up_override` (AC: 5, 6)
- [x] Entité + Repository + Service backend (GET/PUT/DELETE /api/v1/wake-up-override) (AC: 2, 3, 4, 5, 6, 7)
- [x] Recalcul des occurrences WAKE_UP_OFFSET lors du PUT/DELETE (AC: 5, 6, 7)
- [x] Tests backend (WakeUpOverrideServiceTest) (AC: 4, 5, 6)
- [x] Frontend : WakeUpOverrideApiService (AC: 1)
- [x] Frontend : bottom-sheet réveil dans daily-view-page (AC: 2, 3, 4)
- [x] Frontend : bouton réveil dans agenda-page detail panel (AC: 1)

## Dev Notes

- Table : `user_day_wake_up_override` (id UUID PK, user_id UUID FK, override_date DATE, wake_up_time TIME, created_at TIMESTAMP)
  UNIQUE constraint on (user_id, override_date)
- Recalcul : findAllByUserIdAndOccurrenceDateAndStatusNot(userId, date, "canceled") → filtrer timeMode=WAKE_UP_OFFSET → recalc = overrideWakeUp + wakeUpOffsetMinutes
- wakeUpOffsetMinutes vient de TaskRuleEntity (via taskRuleId de l'occurrence)
- Si plus d'override : fallback = DayProfileEntity.wakeUpTime par dayCategory
- Le frontend utilise le même pattern bottom-sheet que daily-view-page.component.html
- Contrainte date : LocalDate.now(ZoneId.of(userTimezoneName)); current day allowed if LocalTime.now(zone).isBefore(LocalTime.of(4,0))

## Dev Agent Record

### Agent Model Used
Claude Sonnet 4.6

### Completion Notes List
- Migration 029 créée : table user_day_wake_up_override avec contrainte unique (user_id, override_date)
- WakeUpOverridEntity, WakeUpOverrideRepository, WakeUpOverrideService, WakeUpOverrideController créés
- Recalcul : fetch toutes les occurrences WAKE_UP_OFFSET du jour, update occurrenceTime, save, publish sync
- 5 tests unitaires WakeUpOverrideServiceTest : PUT set, PUT update, DELETE, jour passé rejeté, jour même avant 4h OK
- Frontend : WakeUpOverrideApiService + bottom-sheet dans daily-view-page + bouton dans agenda-page

### File List
- apps/api/src/main/resources/db/changelog/changes/029-wake-up-override.yaml (new)
- apps/api/src/main/resources/db/changelog/db.changelog-master.yaml (modified)
- apps/api/src/main/java/com/ia/api/wakeup/domain/WakeUpOverrideEntity.java (new)
- apps/api/src/main/java/com/ia/api/wakeup/repository/WakeUpOverrideRepository.java (new)
- apps/api/src/main/java/com/ia/api/wakeup/service/WakeUpOverrideService.java (new)
- apps/api/src/main/java/com/ia/api/wakeup/api/WakeUpOverrideController.java (new)
- apps/api/src/main/java/com/ia/api/wakeup/api/WakeUpOverrideRequest.java (new)
- apps/api/src/main/java/com/ia/api/wakeup/api/WakeUpOverrideResponse.java (new)
- apps/api/src/test/java/com/ia/api/wakeup/service/WakeUpOverrideServiceTest.java (new)
- apps/web/src/app/features/wake-up-override/wake-up-override-api.service.ts (new)
- apps/web/src/app/features/today/daily-view-page.component.ts (modified)
- apps/web/src/app/features/today/daily-view-page.component.html (modified)
- apps/web/src/app/features/today/daily-view-page.component.scss (modified)
- apps/web/src/app/features/agenda/agenda-page.component.ts (modified)
- apps/web/src/app/features/agenda/agenda-page.component.html (modified)

### Change Log
- 2026-04-07 : Implémentation heure réveil custom par jour (story 5.6). Migration 029, backend GET/PUT/DELETE, recalcul WAKE_UP_OFFSET, frontend bottom-sheet + bouton agenda.
