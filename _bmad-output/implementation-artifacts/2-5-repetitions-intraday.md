# Story 2.5 : Répétitions intra-journée (Time Slots)

Status: done

## Story

As a user,
I want to define multiple time slots within the same day for a single task,
so that habits like brushing teeth or hourly hydration are tracked as distinct occurrences on the same day.

## Acceptance Criteria

1. Lors de la création d'une tâche (ponctuelle ou récurrente), l'utilisateur peut ajouter un ou plusieurs créneaux horaires supplémentaires pour la même journée.
2. Chaque créneau peut avoir un mode horaire indépendant : heure fixe (`FIXED`), décalage réveil (`WAKE_UP_OFFSET`), ou décalage depuis le créneau précédent (`AFTER_PREVIOUS`).
3. Le moteur de matérialisation génère N occurrences distinctes par jour actif, une par créneau défini. L'heure de chaque occurrence reflète l'horaire calculé du créneau correspondant.
4. La vue de gestion des occurrences identifie visuellement le numéro de créneau (ex : « Créneau 2/3 ») quand une tâche en possède plusieurs.
5. L'édition et la suppression scoped (THIS_OCCURRENCE, THIS_AND_FOLLOWING) ciblent un créneau individuel, identifié par `task_time_slot_id`.
6. Une tâche sans créneau additionnel se comporte exactement comme avant (rétrocompatibilité totale — zéro régression sur les données existantes).

## Tasks / Subtasks

- [x] **DB — Migration 015** : CREATE TABLE `task_time_slots` (AC: 2, 3)
- [x] **DB — Migration 016** : ADD COLUMN `task_time_slot_id` (nullable) sur `task_occurrences` (AC: 3, 5)
- [x] **DB — Migration 017** : ADD COLUMN `task_time_slot_id` (nullable) sur `task_overrides` + swap contrainte unique (AC: 5)
- [x] **API — Entité & repo** : `TaskTimeSlotEntity`, `TaskTimeSlotRepository` (AC: 1, 2)
- [x] **API — Création** : `CreateTaskRequest` accepte `timeSlots: List<TimeSlotRequest>` optionnel ; `TaskService.createTask` crée les slots après la règle (AC: 1)
- [x] **API — Refresh** : `OccurrenceRefreshService` charge les slots ; si slots > 0, itère jours × slots (slot_order ASC) pour générer N occurrences/jour ; sinon comportement existant (AC: 3, 6)
- [x] **API — Liste** : `TaskOccurrenceResponse` inclut `slotOrder` (nullable int), `totalSlotsPerDay` (int) ; `TaskService.listOccurrences` les alimente (AC: 4)
- [x] **API — Édition/suppression** : `UpdateTaskOccurrenceRequest` et `DeleteTaskOccurrenceRequest` transmettent `taskTimeSlotId` ; `TaskService` oriente l'override ou la suppression vers le bon slot (AC: 5)
- [x] **Angular — Création** : Section optionnelle « Créneaux dans la journée » sur la page de création de tâche — ajouter/supprimer/réordonner des slots (AC: 1, 2)
- [x] **Angular — Gestion** : Carte occurrence affiche « Créneau X/N » si totalSlotsPerDay > 1 (AC: 4)
- [ ] **Tests unitaires** : `OccurrenceRefreshServiceTest` couvre les cas mono-slot (régression) et multi-slots (WAKE_UP_OFFSET + AFTER_PREVIOUS) (AC: 3, 6) 🔜 story 2.6

## Dev Notes

### Schéma `task_time_slots`

```sql
CREATE TABLE task_time_slots (
  id                     UUID PRIMARY KEY NOT NULL,
  task_rule_id           UUID NOT NULL REFERENCES task_rules(id),
  slot_order             INT  NOT NULL,
  time_mode              VARCHAR(20) NOT NULL,   -- FIXED | WAKE_UP_OFFSET | AFTER_PREVIOUS
  fixed_time             TIME,
  wake_up_offset_minutes INT,                    -- 0–720 min
  after_previous_minutes INT,                    -- 0–720 min
  created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT uq_task_time_slots_rule_order UNIQUE (task_rule_id, slot_order)
);
```

### Logique de calcul multi-slot dans `OccurrenceRefreshService`

```
Pour chaque jour actif D :
  slots = findAllByTaskRuleIdOrderBySlotOrderAsc(rule.id)
  if slots.isEmpty() :
    → comportement existant (1 occurrence, heure = rule.fixedTime ou réveil+offset)
  else :
    previousTime = null
    for each slot in slots :
      time = switch(slot.timeMode)
        FIXED          → slot.fixedTime
        WAKE_UP_OFFSET → wakeUpTime + slot.wakeUpOffsetMinutes
        AFTER_PREVIOUS → previousTime + slot.afterPreviousMinutes
      crée/met à jour occurrence(D, time, slot.id)
      previousTime = time
```

### Clé d'unicité des occurrences multi-slot

La colonne `task_time_slot_id` (nullable) permet de distinguer les occurrences du même jour pour la même règle :
- `NULL` → occurrence legacy — une seule par (task_rule_id, occurrence_date)
- `UUID` → occurrence multi-slot — une par (task_rule_id, occurrence_date, task_time_slot_id)

**Note** : ne pas ajouter de contrainte `UNIQUE (task_rule_id, occurrence_date, task_time_slot_id)` en base au niveau du changeset 016 (la valeur NULL n'est pas gérée uniformément par NULLS DISTINCT selon les versions PostgreSQL). Contrôler l'unicité au niveau applicatif dans `OccurrenceRefreshService`.

### Override keying pour slots

```java
// Clé d'override : (ruleId, occurrenceDate, slotId)
// slotId = null pour les occurrences legacy
Optional<TaskOverrideEntity> override = taskOverrideRepository
    .findByTaskRuleIdAndOccurrenceDateAndTaskTimeSlotId(
        rule.getId(), occurrence.getOccurrenceDate(), occurrence.getTaskTimeSlotId());
```

### `TaskOccurrenceResponse` — champs additionnels

```java
record TaskOccurrenceResponse(
    ...                  // champs existants inchangés
    Integer slotOrder,   // null si legacy, 1..N sinon
    int totalSlotsPerDay // 1 si legacy ou mono-slot, N sinon
) {}
```

### Rétrocompatibilité

- Toutes les occurrences existantes ont `task_time_slot_id = NULL` → traitées comme avant.
- `OccurrenceRefreshService` : branche `if (slots.isEmpty())` reproduit l'algorithme actuel à l'identique.
- Les endpoints existants n'ont pas de champ `taskTimeSlotId` obligatoire : `null` = comportement legacy.

### Ordre de migration Liquibase

```
015-task-time-slots.yaml      — CREATE TABLE task_time_slots
016-occurrence-slot-fk.yaml   — ADD COLUMN task_time_slot_id (nullable) → task_occurrences
017-override-slot-fk.yaml     — ADD COLUMN task_time_slot_id (nullable) → task_overrides
                                 + DROP CONSTRAINT uq_task_overrides_rule_date
                                 + ADD CONSTRAINT uq_task_overrides_rule_date_slot
                                   UNIQUE (task_rule_id, occurrence_date, task_time_slot_id)
```

### Points différés (post-MVP)

- Mutation d'un seul slot indépendamment des autres via THIS_AND_FOLLOWING
- Helper UX « Répéter toutes les X min, N fois » (génération automatique des slots côté frontend)
- Statistiques de complétion par slot (streak par créneau)

### Sources architecturales

- [ADR-001 — Répétitions Intra-Journée](../planning-artifacts/architecture.md#adr-001--répétitions-intra-journée-intraday-time-slots)
- [architecture.md — Data modeling approach](../planning-artifacts/architecture.md#data-modeling-approach)
