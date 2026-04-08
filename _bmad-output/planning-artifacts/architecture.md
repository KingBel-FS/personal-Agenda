---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
inputDocuments:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/ux-design-specification.md'
workflowType: 'architecture'
project_name: 'IA'
user_name: 'Gino Cachondeo'
date: '2026-03-24'
lastStep: 8
status: 'complete'
completedAt: '2026-03-24'
---

# Architecture Decision Document

## Project Context Analysis

### Requirements Overview

**Functional requirements**

IA est une PWA mobile-first de gestion d'habitudes avec :

- authentification email/mot de passe, activation email, suppression de compte
- moteur de tâches ponctuelles et récurrentes
- calcul d'occurrences selon type de jour et heure de réveil
- vue Aujourd'hui, agenda semaine/mois, stats, objectifs, profil
- système de streaks, badges et suspension d'occurrence
- notifications push séquentielles avec actions directes
- synchronisation des jours fériés français et gestion manuelle des vacances
- uploads d'images profil et tâche

**Non-functional requirements**

Les contraintes qui pilotent l'architecture sont :

- timezone unique `Europe/Paris`
- exactitude métier du moteur de récurrence et des streaks
- API < 200 ms au p95 sur les opérations courantes
- LCP/TTI < 1 s sur les vues principales
- conformité RGAA / WCAG AA
- sécurité forte des sessions et isolation stricte des données
- dégradation gracieuse si l'API gouv ou les push subscriptions échouent

**Scale & Complexity**

Le projet est une application **full-stack web PWA** avec une complexité surtout métier, pas volumétrique. Le point difficile est la cohérence temporelle : récurrences, DST, jobs de notification, statuts dérivés, streaks et recalculs.

- Primary domain: full-stack web with background jobs
- Complexity level: medium-high business logic
- Estimated architectural components: 10 to 14 modules

### Technical Constraints & Dependencies

- PWA installable avec Service Worker
- Web Push avec VAPID
- API jours fériés `calendrier.api.gouv.fr`
- stockage privé des médias avec URLs signées
- calcul métier exclusivement côté serveur

### Cross-Cutting Concerns Identified

- gestion unifiée du temps
- sécurité et sessions
- scheduling et idempotence des jobs
- accessibilité native
- observabilité des flux critiques
- cohérence des statuts sur tous les écrans

## Starter Template Evaluation

### Primary Technology Domain

Le domaine principal est **full-stack Spring + Angular + worker backend**, pas un simple frontend SPA. Le produit a besoin d'un backend applicatif robuste pour :

- calculer les occurrences
- planifier et annuler les notifications
- recalculer les streaks
- centraliser toutes les règles liées à `Europe/Paris`

### Chosen Foundation

La fondation retenue est un **monorepo polyglotte simple** :

- `apps/web` : Angular PWA
- `apps/api` : Spring Boot API
- `apps/worker` : Spring Boot worker
- `infra/db` : scripts et changelogs Liquibase

### Verified Current Versions

Versions vérifiées le **24 mars 2026** sur sources officielles ou docs officielles :

- **Spring Boot 4.0.3** - page officielle Spring Boot et référence courante
- **Angular 20 LTS** - Angular 20 est la branche LTS officielle ; Angular 21 est active mais non LTS
- **Java 21 LTS** - version de base retenue pour la plateforme
- **PostgreSQL 17.x** pour le MVP, avec compatibilité d'upgrade vers 18 current
- **Liquibase 5.0.x** - dernière branche majeure officielle documentée

Choix d'architecture dérivés :

- **Angular 20 LTS** plutôt que branche active plus récente, pour la stabilité
- **PostgreSQL 17.x** plutôt que 18 current au MVP, pour un compromis stabilité / modernité

### Final Starter Decision

Pas de starter full-stack imposé. On retient :

- Angular CLI pour le frontend
- Spring Initializr pour `api` et `worker`
- Liquibase comme mécanisme unique de migration
- Maven comme build tool Java pour homogénéité d'équipe et simplicité CI

## Core Architectural Decisions

### Decision Priority Analysis

**Critical decisions**

- séparation `web / api / worker`
- PostgreSQL comme source de vérité
- Liquibase comme unique outil de migration
- Spring Boot pour l'API et les jobs
- Angular pour la PWA
- calcul des récurrences et notifications exclusivement côté serveur

**Important decisions**

- Maven multi-module ou mono-repo avec deux apps Spring séparées
- REST JSON versionné
- auth JWT courte durée + refresh rotatif
- stockage objet S3-compatible pour les médias

**Deferred decisions**

- analytics produit avancées
- websocket temps réel
- multi-région
- mode offline métier

### Data Architecture

**Database**

Choix : **PostgreSQL 17.x** au MVP.

Pourquoi :

- officiellement supporté
- suffisamment moderne
- excellent fit pour transactions, indexation, verrous SQL et jobs pilotés en base

**Migrations**

Choix : **Liquibase** uniquement.

Règles :

- tout changement de schéma passe par un changelog versionné
- pas de génération automatique silencieuse au runtime
- les seeds métier stables sont séparés des migrations structurelles

**Data modeling approach**

Le modèle distingue :

- `task_definition` : définition canonique de la tâche
- `task_rule` : règle de planification
- `task_override` : exception locale ou future
- `task_occurrence` : occurrence concrète matérialisée
- `notification_job` : notification planifiée
- `occurrence_status_event` : historique des changements d'état

Tables principales :

- `users`
- `refresh_tokens`
- `push_subscriptions`
- `day_profiles`
- `holidays`
- `vacation_periods`
- `task_definitions`
- `task_rules`
- `task_overrides`
- `task_occurrences`
- `occurrence_status_events`
- `notification_jobs`
- `streak_snapshots`
- `badges`
- `goals`
- `goal_progress_snapshots`
- `assets`

**Key architectural rule**

La vue Aujourd'hui ne calcule jamais elle-même les occurrences métier. Elle consomme des projections renvoyées par l'API.

### Authentication & Security

**Auth model**

- email + mot de passe
- hash `Argon2id`
- access token JWT 15 minutes
- refresh token rotatif 30 jours
- refresh token stocké haché en base
- cookie `httpOnly` et `secure` pour le frontend web

**Authorization**

- vérification systématique de propriété sur chaque ressource
- aucune lecture croisée inter-utilisateur
- pas de rôles complexes au MVP en dehors de `USER`

**Spring security**

- Spring Security comme point d'entrée unique
- filtres JWT dédiés
- configuration stateless sur l'API
- validation centralisée des permissions métier

### API & Communication

**API style**

Choix : **REST JSON** sous `/api/v1`.

Raisons :

- domaine transactionnel et explicite
- testabilité simple
- excellent fit avec Angular `HttpClient`
- documentation OpenAPI facile

**Success response format**

```json
{
  "data": {},
  "meta": {}
}
```

**Error response format**

```json
{
  "error": {
    "code": "TASK_NOT_FOUND",
    "message": "Task occurrence not found",
    "details": {}
  }
}
```

**Internal communication**

- `web -> api` via HTTPS REST
- `api -> db` via JPA / JDBC
- `api -> worker` via tables PostgreSQL et événements persistés en base
- `worker -> push` via Web Push VAPID

### Backend Application Architecture

**API framework**

Choix : **Spring Boot 4.0.3**.

Modules techniques principaux :

- Spring Web
- Spring Security
- Spring Validation
- Spring Data JPA
- Actuator
- Mail
- scheduling / async selon besoin local

**Persistence approach**

Approche hybride maîtrisée :

- **Spring Data JPA** pour entités transactionnelles classiques
- **requêtes SQL ciblées** pour jobs, projections de calendrier et traitements sensibles aux performances

Pourquoi :

- JPA accélère le développement du cœur CRUD
- SQL explicite garde le contrôle sur les zones complexes : occurrences, jobs, agrégats stats

### Frontend Architecture

**Framework**

Choix : **Angular 20 LTS**.

Raisons :

- structure forte et prévisible
- bon fit pour un produit à formulaires riches et écrans métier nombreux
- DI, routing, guards et séparation feature-first très naturels
- bonne discipline de code pour plusieurs agents

**State management**

- signaux Angular + services par feature pour le local
- cache HTTP côté feature
- store global léger seulement pour auth, préférences et shell applicatif

Pas de sur-ingénierie NgRx au démarrage tant que la complexité frontend ne le justifie pas.

**PWA strategy**

- Angular PWA support
- Service Worker dédié au shell, manifest, icônes et push
- pas d'offline métier complet au MVP

**UI system**

- Angular Material CDK ou stack UI maison légère selon niveau de personnalisation
- les composants métier du UX doc restent la référence
- l'accessibilité et les tokens sémantiques restent obligatoires

### Background Processing & Scheduling

Le produit nécessite un moteur de planification fiable, pas juste un cron simpliste.

**Decision**

Le `worker` Spring est une application séparée responsable de :

- matérialiser les occurrences sur horizon glissant
- créer les notifications séquentielles
- annuler les jobs restants après exécution
- envoyer l'alerte streak de 20h
- purger les subscriptions invalides
- synchroniser les jours fériés
- recalculer streaks, badges et objectifs

Le modèle retenu n'est plus un batch nocturne unique. La matérialisation fonctionne en **deux couches complémentaires** :

- **write-through côté API** : après création de tâche ou mutation qui change le futur, l'API recalcule immédiatement les occurrences futures concernées
- **worker always-on de réconciliation** : le worker tourne en continu avec un cycle court pour rattraper les écarts, compléter les insertions manquantes et rejouer les traitements idempotents

**Scheduling strategy**

- scheduler Spring côté worker
- coordination et verrouillage via PostgreSQL
- jobs idempotents
- sélection concurrent-safe avec `FOR UPDATE SKIP LOCKED` ou mécanisme équivalent
- matérialisation des occurrences déclenchée immédiatement sur écriture critique et réconciliée ensuite par le worker

**Why not external queue first**

Redis/BullMQ n'existe plus dans cette stack cible. Au MVP, PostgreSQL suffit comme colonne vertébrale du scheduling. Cela réduit l'infra et garde la vérité métier dans une seule base.

**Rule for agents**

Une vue fondée sur `task_occurrences` ne doit jamais dépendre exclusivement d'un passage planifié quotidien. Toute mutation qui change le futur visible utilisateur doit déclencher un recalcul synchrone ou quasi synchrone des occurrences impactées.

### Infrastructure & Deployment

**Deployment shape**

- `web` : application Angular servie derrière Nginx ou CDN + reverse proxy
- `api` : Spring Boot stateless
- `worker` : Spring Boot always-on
- `postgres` : base managée
- `object storage` : bucket privé

**Container strategy**

- image Docker pour `api`
- image Docker pour `worker`
- image ou build static pour `web`
- `docker compose` local pour l'environnement de dev

**Why not pure serverless**

- les jobs doivent être fiables et continus
- les notifications nécessitent une planification précise
- le moteur métier ne doit pas dépendre de réveils opportunistes

## Implementation Patterns & Consistency Rules

### Pattern Categories Defined

Zones à risque de divergence entre agents :

- naming DB / Java / TypeScript
- formats API
- statuts métier
- logique date/heure
- structure de modules Spring
- structure feature Angular

### Naming Patterns

**Database naming**

- tables : `snake_case` pluriel
- colonnes : `snake_case`
- foreign keys : `<target>_id`
- index : `idx_<table>_<field>`
- unique constraints : `uq_<table>_<field>`

**API naming**

- endpoints REST pluriels
- actions métier explicites :
  - `POST /api/v1/occurrences/{id}/complete`
  - `POST /api/v1/occurrences/{id}/miss`
  - `POST /api/v1/occurrences/{id}/suspend`
- payloads JSON en `camelCase`

**Java naming**

- classes : `PascalCase`
- méthodes / variables : `camelCase`
- packages : minuscules pointées par domaine
- enums : `UPPER_SNAKE_CASE`

**Angular naming**

- fichiers : `kebab-case`
- composants : `PascalCase`
- selectors préfixés projet
- services par feature, pas de fourre-tout `shared` métier

### Status Semantics

Statuts métier autorisés :

- `planned`
- `done`
- `missed`
- `suspended`
- `skipped`
- `canceled`

`skipped` = effet du contexte du jour.  
`suspended` = décision utilisateur.  
Ces deux états ne doivent jamais être fusionnés.

### Structure Patterns

**Spring organization**

- organisation par domaine métier, pas par couche technique pure
- chaque module contient `controller`, `service`, `repository`, `dto`, `mapper`, `domain`
- les jobs worker suivent la même logique métier

**Angular organization**

- organisation `feature-first`
- chaque feature contient pages, components, services, models, api, tests
- composants UI génériques séparés des composants métier

**Validation**

- validation d'entrée backend via Bean Validation
- validation frontend par Reactive Forms
- règles métier canoniques uniquement au backend

### Format Patterns

**Dates**

- timestamps API en ISO 8601 UTC
- dates calendaires métier stockées explicitement en `date`
- aucun calcul métier dépendant du fuseau navigateur

**Null handling**

- pas de `null` arbitraire dans les payloads publics quand l'absence peut être explicite
- utiliser champs optionnels cohérents

### Communication Patterns

**Domain events**

Convention `domain.entity.verb` :

- `task.occurrence.completed`
- `task.occurrence.missed`
- `task.occurrence.suspended`
- `task.definition.updated`
- `notification.subscription.revoked`

**Event payload**

- `eventId`
- `occurredAt`
- `userId`
- `entityId`
- `payload`

### Process Patterns

**Error handling**

- `@ControllerAdvice` central côté API
- mapping unique erreur technique -> erreur API
- jamais de stacktrace exposée au client

**Idempotency**

- mutations critiques rejouables sans double-effet
- jobs worker idempotents
- notifications annulées ou déjà envoyées non recréées

**Time rules**

- service temps unique côté backend
- timezone métier fixée à `Europe/Paris`
- tests obligatoires sur DST et minuit

## Project Structure & Boundaries

### Project Tree

```text
personal-agenda/
├── README.md
├── .gitignore
├── docs/
│   ├── adr/
│   ├── api/
│   └── runbooks/
├── apps/
│   ├── web/
│   │   ├── package.json
│   │   ├── angular.json
│   │   ├── tsconfig.json
│   │   └── src/
│   │       ├── app/
│   │       │   ├── core/
│   │       │   │   ├── auth/
│   │       │   │   ├── guards/
│   │       │   │   ├── interceptors/
│   │       │   │   ├── layout/
│   │       │   │   └── pwa/
│   │       │   ├── shared/
│   │       │   │   ├── ui/
│   │       │   │   ├── pipes/
│   │       │   │   └── utils/
│   │       │   ├── features/
│   │       │   │   ├── auth/
│   │       │   │   ├── today/
│   │       │   │   ├── agenda/
│   │       │   │   ├── tasks/
│   │       │   │   ├── streaks/
│   │       │   │   ├── goals/
│   │       │   │   ├── stats/
│   │       │   │   ├── notifications/
│   │       │   │   └── profile/
│   │       │   ├── app.routes.ts
│   │       │   └── app.config.ts
│   │       ├── assets/
│   │       ├── manifest.webmanifest
│   │       └── ngsw-config.json
│   ├── api/
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/ia/api/
│   │       │   │   ├── config/
│   │       │   │   ├── common/
│   │       │   │   ├── auth/
│   │       │   │   ├── user/
│   │       │   │   ├── holiday/
│   │       │   │   ├── vacation/
│   │       │   │   ├── task/
│   │       │   │   ├── occurrence/
│   │       │   │   ├── notification/
│   │       │   │   ├── streak/
│   │       │   │   ├── badge/
│   │       │   │   ├── goal/
│   │       │   │   ├── stats/
│   │       │   │   └── asset/
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       └── db/
│   │       │           └── changelog/
│   │       └── test/
│   └── worker/
│       ├── pom.xml
│       └── src/
│           ├── main/
│           │   ├── java/com/ia/worker/
│           │   │   ├── config/
│           │   │   ├── common/
│           │   │   ├── holiday/
│           │   │   ├── occurrence/
│           │   │   ├── notification/
│           │   │   ├── streak/
│           │   │   └── job/
│           │   └── resources/
│           │       └── application.yml
│           └── test/
├── infra/
│   ├── db/
│   │   └── liquibase/
│   ├── docker/
│   └── nginx/
└── tests/
    ├── contract/
    ├── integration/
    └── e2e/
```

### Boundary Rules

**web**

- rendu UI, interactions, navigation, PWA et push côté client
- aucune règle métier canonique de récurrence ou streak

**api**

- seule entrée métier synchrone
- transactions, validation, sécurité, projections de lecture

**worker**

- traitements différés et planifiés uniquement
- pas d'API utilisateur

**database / liquibase**

- schéma, versionnement et gouvernance structurelle
- aucune logique HTTP ou UI

### Requirement Mapping

**Auth & compte**

- `apps/api/.../auth`
- `apps/api/.../user`
- `apps/web/.../features/auth`

**Jours fériés / vacances / profils de jour**

- `apps/api/.../holiday`
- `apps/api/.../vacation`
- `apps/worker/.../holiday`

**Tâches / occurrences / récurrence**

- `apps/api/.../task`
- `apps/api/.../occurrence`
- `apps/worker/.../occurrence`

**Notifications**

- `apps/api/.../notification`
- `apps/worker/.../notification`
- `apps/web/.../features/notifications`

**Streaks / badges / objectifs / stats**

- `apps/api/.../streak`
- `apps/api/.../badge`
- `apps/api/.../goal`
- `apps/api/.../stats`
- `apps/worker/.../streak`

## Architecture Validation Results

### Coherence Validation

**Decision compatibility**

Les choix sont cohérents :

- Angular 20 LTS pour la PWA
- Spring Boot 4.0.3 pour les services backend
- Java 21 LTS comme base runtime
- PostgreSQL comme point de vérité
- Liquibase comme gouvernance unique du schéma

**Pattern consistency**

- `camelCase` côté API JSON et TypeScript
- `snake_case` côté base
- statuts métier unifiés
- service temps centralisé

**Structure alignment**

La structure reflète correctement les frontières techniques et métier. Chaque domaine critique a un emplacement unique.

### Requirements Coverage Validation

Toutes les familles de requirements majeures du PRD sont couvertes :

- compte et sécurité
- profils de jour
- tâches ponctuelles et récurrentes
- modifications locales et futures
- notifications séquentielles
- agenda et vue Aujourd'hui
- streaks, badges, objectifs et stats
- uploads et médias

Les NFR critiques sont également couverts :

- performance
- sécurité
- accessibilité
- fiabilité temporelle
- dégradation gracieuse des intégrations

### Implementation Readiness Validation

Le document est prêt pour guider l'implémentation, à condition de respecter :

- aucune logique temps dupliquée côté Angular
- toutes les évolutions de schéma via Liquibase
- tous les jobs worker idempotents
- aucun nouveau statut métier introduit localement sans mise à jour centrale

### Gap Analysis Results

Pas de bloqueur critique.

Points ouverts non bloquants :

- choix exact du fournisseur email transactionnel
- choix exact du stockage objet
- décision finale Angular Material vs bibliothèque UI plus custom
- stratégie PDF détaillée lors de l'implémentation

## Completion Summary

L'architecture est maintenant réalignée sur ta stack cible : **Spring Boot + Angular + PostgreSQL + Liquibase**. La forme retenue est une PWA Angular, une API Spring Boot, un worker Spring Boot séparé et PostgreSQL comme source unique de vérité, avec Liquibase pour toute évolution de schéma.

### Recommended Next Steps

1. Générer les epics et stories techniques alignées sur cette nouvelle stack
2. Initialiser la structure physique `web / api / worker`
3. Créer le modèle de données initial Liquibase
4. Implémenter d'abord auth, profils de jour, tâches, occurrences, puis notifications

---

## ADR-001 — Répétitions Intra-Journée (Intraday Time Slots)

**Date** : 2026-03-25
**Statut** : Accepté
**Contexte** : Feature demandée par Gino — permettre qu'une tâche se produise plusieurs fois le même jour (ex : brossage des dents matin + soir, hydratation toutes les heures).

### Problème

Le modèle actuel `task_rule → task_occurrence` génère exactement **1 occurrence par jour actif par règle**. Il n'existe aucun mécanisme pour exprimer « cette tâche a lieu N fois dans la même journée ».

### Options évaluées

| Option | Description | Verdict |
|---|---|---|
| Multiple `task_rules` liées | 2 règles = 2 créneaux dans la journée | ❌ Mutations THIS_AND_FOLLOWING brisées, risque d'orphelins |
| Nouvelle entité `task_time_slots` | 1 règle → N créneaux → N occurrences/jour | ✅ Retenu |
| JSON column `additional_times` sur `task_rules` | CSV ou JSON des horaires additionnels | ❌ Anti-pattern SQL, impossible à indexer ou à overrider par créneau |
| Champs intervalle sur `task_rules` | `interval_minutes` + `repeat_count` | ⚠️ Insuffisant pour les schedules mixtes (réveil+30min puis fixe 21h) |

### Décision : Nouvelle entité `task_time_slots`

```
task_time_slots
  id                    UUID PK NOT NULL
  task_rule_id          UUID FK → task_rules NOT NULL
  slot_order            INT NOT NULL  (1-based, ordonnancement dans la journée)
  time_mode             VARCHAR(20) NOT NULL
                        Valeurs : FIXED | WAKE_UP_OFFSET | AFTER_PREVIOUS
  fixed_time            TIME (nullable)
  wake_up_offset_minutes INT (nullable, 0–720)
  after_previous_minutes INT (nullable, 0–720)
                        Décalage depuis le slot précédent calculé (slot_order - 1)
  created_at            TIMESTAMP WITH TIME ZONE NOT NULL

  UNIQUE (task_rule_id, slot_order)
```

**Sémantique de `time_mode`** :
- `FIXED` — heure absolue dans la journée (ex : 21h00)
- `WAKE_UP_OFFSET` — X minutes après l'heure de réveil calculée (ex : +30 min)
- `AFTER_PREVIOUS` — X minutes après le créneau précédent calculé (ex : +720 min = 12h après slot 1)

**Exemples de représentation** :

```
Brossage dents (2 fois/jour) :
  slot 1 — WAKE_UP_OFFSET, 30 min
  slot 2 — AFTER_PREVIOUS, 720 min (12h après slot 1)

Hydratation (8 fois/jour) :
  slot 1 — WAKE_UP_OFFSET, 15 min
  slot 2 — AFTER_PREVIOUS, 60 min
  slot 3 — AFTER_PREVIOUS, 60 min
  ... (slot N — AFTER_PREVIOUS, 60 min)
```

### Modifications du schéma existant

**`task_occurrences`** — ajout d'un FK nullable vers le slot source :
```sql
ALTER TABLE task_occurrences
  ADD COLUMN task_time_slot_id UUID REFERENCES task_time_slots(id);
-- NULL = occurrence legacy (comportement existant, un seul slot implicite)
```

**`task_overrides`** — ajout du FK slot + remplacement de la contrainte unique :
```sql
ALTER TABLE task_overrides
  ADD COLUMN task_time_slot_id UUID REFERENCES task_time_slots(id);
DROP CONSTRAINT uq_task_overrides_rule_date;
ADD CONSTRAINT uq_task_overrides_rule_date_slot
  UNIQUE (task_rule_id, occurrence_date, task_time_slot_id);
-- NULLS DISTINCT : deux overrides null sont distincts → comporte legacy inchangé
```

### Invariants obligatoires

1. **Rétrocompatibilité totale** — `task_rule` sans `task_time_slots` → comportement identique à aujourd'hui. Aucune migration de données existantes.
2. **Calcul exclusivement côté serveur** — `OccurrenceRefreshService` résout `AFTER_PREVIOUS` en itérant les slots dans l'ordre `slot_order ASC`. Le frontend reçoit uniquement des heures calculées.
3. **Ordonnancement déterministe** — `slot_order` garantit un ordre stable. La contrainte `UNIQUE (task_rule_id, slot_order)` l'impose en base.
4. **THIS_AND_FOLLOWING MVP** — la mutation future modifie tous les slots de la règle à partir de la date ciblée. La mutation d'un slot individuel en isolation est différée post-MVP.
5. **Idempotence du refresh** — un refresh sur une règle multi-slot purge les occurrences futures de tous les slots, puis régénère l'ensemble.

### Impact sur les couches

| Couche | Changement |
|---|---|
| DB | Nouvelle table `task_time_slots` ; colonnes `task_time_slot_id` sur `task_occurrences` et `task_overrides` |
| `OccurrenceRefreshService` | Charge les slots de la règle ; si slots > 0, itère jours × slots ; sinon comportement existant |
| `TaskService.createTask` | Champ optionnel `timeSlots: List<TimeSlotRequest>` dans `CreateTaskRequest` |
| `TaskService.listOccurrences` | `TaskOccurrenceResponse` inclut `slotOrder` (int, null si legacy), `totalSlotsPerDay` (int) |
| `TaskService.updateOccurrence` | Override keyed on `(rule_id, date, task_time_slot_id)` — null pour legacy |
| `TaskService.deleteOccurrence` | Purge les occurrences de TOUS les slots de la règle pour la date ou les dates futures |
| Angular `task-manage-page` | Affiche `slotOrder` dans la carte occurrence si `totalSlotsPerDay > 1` |
| Angular `task-create-page` | Section optionnelle « Créneaux dans la journée » — ajouter/supprimer/réordonner des slots |

### Migrations Liquibase à créer

```
015-task-time-slots.yaml        — CREATE TABLE task_time_slots
016-occurrence-slot-fk.yaml     — ADD COLUMN task_time_slot_id → task_occurrences
017-override-slot-fk.yaml       — ADD COLUMN task_time_slot_id + swap unique constraint
```

### Ce qui est différé

- Mutation d'un slot individuel indépendamment des autres (THIS_AND_FOLLOWING ciblé sur 1 slot)
- UX « répéter toutes les X min, N fois » (helper UI — crée les slots automatiquement côté frontend)
- Statistiques de complétion par slot (streak par créneau)
