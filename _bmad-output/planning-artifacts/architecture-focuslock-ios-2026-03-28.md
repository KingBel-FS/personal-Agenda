---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
inputDocuments:
  - C:\Dev\blocker\_bmad-output\planning-artifacts\product-brief-focuslock-ios-2026-03-28.md
  - C:\Dev\blocker\_bmad-output\planning-artifacts\prd-focuslock-ios-2026-03-28.md
  - C:\Dev\blocker\_bmad-output\planning-artifacts\ux-design-specification.md
workflowType: architecture
project_name: FocusLock iPhone
user_name: Gino Cachondeo
date: 2026-03-28
---

# Architecture Decision Document

## 1. Contexte

Le produit cible exclusivement l'iPhone. L'utilisateur veut restreindre l'acces a certaines apps et a certains sites web, avec des limites comme "Instagram 30 min par jour". La stack demandee est Spring + Angular.

## 2. Decision architecturale principale

Nous retenons une architecture en trois couches:

- Angular pour l'interface web/PWA et les surfaces de configuration
- Spring Boot pour l'API, la persistance et l'orchestration
- iOS natif en Swift/SwiftUI pour l'enforcement local sur l'iPhone

## 3. Pourquoi cette architecture

Une PWA seule n'a pas les privileges iOS necessaires pour bloquer d'autres applications ou appliquer de facon fiable des restrictions systeme sur des domaines web. Les restrictions doivent s'appuyer sur les frameworks Apple FamilyControls, ManagedSettings et DeviceActivity.

## 4. Vue logique

### 4.1 Frontend Angular

Responsabilites:

- onboarding
- authentification
- gestion des regles
- dashboard et insights
- etat de synchronisation

Capacites:

- application PWA installable
- design system du produit
- mode lecture seule si l'app iOS n'est pas connectee

### 4.2 Backend Spring Boot

Responsabilites:

- gestion des comptes
- CRUD des regles
- journal d'audit
- consolidation analytics
- preferences de notification
- synchronisation entre clients

Modules:

- `auth-service`
- `rule-service`
- `device-service`
- `usage-service`
- `notification-service`
- `audit-service`

### 4.3 App iOS native

Responsabilites:

- demande d'autorisation Family Controls
- selection des apps/categories/domaines via composants Apple
- application des shields et restrictions
- monitoring des seuils et des horaires
- remontee des etats vers le backend

## 5. Decoupage technique

### Frontend

- Angular 20+
- Angular Router
- Angular Service Worker
- Angular Signals pour l'etat UI si l'equipe le souhaite
- appels API REST ou GraphQL REST-like vers Spring

### Backend

- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis optionnel pour cache et sessions
- OpenAPI pour le contrat

### iOS

- SwiftUI
- FamilyControls
- ManagedSettings
- DeviceActivity
- extension(s) iOS requises pour le monitoring/shielding selon le design final

## 6. Modele de donnees principal

### Utilisateur

- id
- email
- timezone
- created_at
- plan

### Device

- id
- user_id
- platform = ios
- authorization_status
- last_sync_at
- app_version

### Rule

- id
- user_id
- device_id
- type = daily_limit | schedule_block
- target_kind = app | app_category | web_domain | web_category
- target_ref
- threshold_value
- threshold_unit
- recurrence
- override_policy
- enabled

### UsageEvent

- id
- device_id
- rule_id
- event_type = warning | blocked | override_started | override_completed | sync_error
- event_time
- payload_json

## 7. API principales

- `POST /auth/register`
- `POST /auth/login`
- `GET /me`
- `GET /devices`
- `POST /devices/link`
- `GET /rules`
- `POST /rules`
- `PUT /rules/{id}`
- `DELETE /rules/{id}`
- `POST /usage/events`
- `GET /insights/summary`
- `GET /system/status`

## 8. Synchronisation

Strategie:

- source de verite produit: backend Spring
- source de verite enforcement local: app iOS pour l'etat runtime
- reconciliation bidirectionnelle sur lancement, reprise et modifications de regles

## 9. Securite

- JWT ou session securisee selon choix produit
- TLS obligatoire
- rate limiting sur auth et endpoints sensibles
- chiffrement des secrets
- minimisation des donnees d'usage stockees

## 10. Observabilite

- logs structures backend
- tracing des erreurs de synchro
- metrics sur activation, autorisations, creations de regles, blocages et overrides

## 11. Risques

- entitlement Apple potentiellement restrictif
- divergence entre regles backend et etat local iOS
- confusion produit si la PWA est exposee comme surface principale de blocage

## 12. Recommandation de livraison

Phase 1:

- backend Spring
- Angular pour dashboard/configuration
- iOS natif minimal pour autorisation + application des regles

Phase 2:

- insights plus riches
- routines avancees
- override intelligent

