---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/ux-design-specification.md'
  - '_bmad-output/planning-artifacts/architecture.md'
---

# IA - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for IA, decomposing the requirements from the PRD, UX Design, and Architecture into implementable stories aligned with the chosen stack: Angular PWA, Spring Boot API, Spring Boot worker, PostgreSQL, and Liquibase.

## Requirements Inventory

### Functional Requirements

FR1: Un visiteur peut créer un compte avec pseudo, email, mot de passe, date de naissance, photo optionnelle et zone géographique.  
FR2: Un visiteur reçoit un email d'activation après inscription et doit activer son compte avant connexion.  
FR3: Un utilisateur peut se connecter avec son email et mot de passe via JWT.  
FR4: Un utilisateur peut demander la réinitialisation de son mot de passe via email sécurisé.  
FR5: Un utilisateur peut modifier ses informations de profil.  
FR6: Un utilisateur peut supprimer son compte et toutes ses données.  
FR7: Un utilisateur peut modifier sa zone géographique avec confirmation d'impact sur les jours fériés futurs.  
FR8: Un utilisateur peut configurer une heure de réveil distincte pour chaque catégorie de jour.  
FR9: Un utilisateur peut déclarer des jours de vacances dans l'application.  
FR10: Le système récupère automatiquement les jours fériés selon la zone géographique à la création de compte.  
FR11: Le système rafraîchit annuellement les jours fériés disponibles.  
FR12: Le système classifie automatiquement chaque jour et skip les tâches non applicables.  
FR13: Un utilisateur peut créer une tâche ponctuelle complète.  
FR14: Un utilisateur peut créer une tâche récurrente avec fréquence et date de fin optionnelle.  
FR15: Un utilisateur ne peut pas créer une tâche avec une date de début dans le passé.  
FR16: Un utilisateur peut modifier une occurrence unique d'une tâche récurrente.  
FR17: Un utilisateur peut modifier une occurrence et toutes les occurrences futures.  
FR18: Un utilisateur peut supprimer une occurrence unique sans affecter les autres.  
FR19: Un utilisateur peut supprimer une occurrence et toutes les occurrences futures.  
FR20: Un utilisateur ne peut pas modifier ou supprimer une occurrence passée.  
FR21: Un utilisateur peut suspendre une occurrence pour un jour spécifique.  
FR22: Un utilisateur peut marquer une tâche comme exécutée ou non exécutée jusqu'à minuit.  
FR23: Un utilisateur peut consulter les tâches ponctuelles et occurrences passées en lecture seule depuis l'agenda.  
FR24: Le système envoie une notification push 15 minutes avant une tâche.  
FR25: Le système envoie une notification push 2 minutes avant une tâche.  
FR26: Le système envoie une notification push à l'heure exacte avec boutons d'action.  
FR27: Le système envoie une notification push 1 heure après l'heure programmée si la tâche n'est pas exécutée.  
FR28: Le système annule automatiquement les notifications restantes dès qu'une tâche est marquée exécutée.  
FR29: Le système envoie une notification push à 20h si la streak est active et qu'aucune tâche du jour n'est exécutée.  
FR30: Le système envoie une notification push le jour d'anniversaire de l'utilisateur.  
FR31: Le système envoie une notification push le dimanche soir si un objectif hebdomadaire n'est pas atteint.  
FR32: Le système envoie une notification push le dernier jour du mois si un objectif mensuel n'est pas atteint.  
FR33: Un utilisateur peut interagir avec les boutons d'action directement depuis la notification push.  
FR34: L'application affiche une confirmation après une action effectuée depuis une notification push.  
FR35: L'application affiche un banner persistant si les notifications push sont désactivées.  
FR36: L'application maintient un centre de notifications in-app.  
FR37: Le système calcule et affiche en temps réel la streak en cours.  
FR38: Le système calcule et affiche la streak la plus longue de l'historique.  
FR39: Une suspension déclarée avant l'heure programmée ne rompt pas la streak.  
FR40: Le système débloque des badges visuels à des jalons de streak.  
FR41: Le système affiche une alerte sur une tâche suspendue trop fréquemment.  
FR42: Un utilisateur peut créer des objectifs de nombre de tâches à exécuter par semaine ou par mois.  
FR43: Un utilisateur peut créer des objectifs associés à une tâche récurrente spécifique éligible.  
FR44: Le système affiche la progression des objectifs en temps réel.  
FR45: Un utilisateur peut voir les tâches du jour ordonnées par heure effective dans une vue principale.  
FR46: La vue principale affiche une barre de progression du jour.  
FR47: Le badge de l'icône PWA affiche le nombre de tâches restantes.  
FR48: Un utilisateur peut visualiser son agenda en vue mois et semaine.  
FR49: L'agenda affiche pour chaque jour un indicateur visuel de statut et sa catégorie de jour.  
FR50: Un utilisateur peut créer, modifier et supprimer des tâches et occurrences depuis l'agenda via une modale contextuelle.  
FR50b: Un utilisateur peut, sur une série future, modifier la date de fin, les jours de semaine, les jours concernés et conserver le mode horaire d'origine sans conversion implicite.  
FR51: L'application synchronise les vues en temps réel entre onglets et appareils du même compte.  
FR52: Un utilisateur peut consulter ses statistiques hebdomadaires, mensuelles, annuelles et globales.  
FR53: Un utilisateur peut consulter ses statistiques par tâche individuelle.  
FR54: Le dashboard affiche des KPI et comparaisons N vs N-1 avec graphiques.  
FR55: Un utilisateur peut exporter son historique, ses statistiques et sa liste de tâches.  
FR56: Le dashboard affiche le suivi des objectifs avec progression et historique.  
FR57: Un utilisateur peut basculer entre thème clair et sombre avec détection système.  
FR58: Un utilisateur peut accéder aux mentions légales et à la politique de confidentialité.  
FR59: L'application recueille le consentement explicite à la collecte de données lors de l'inscription.  
FR60: La photo de profil est incluse dans le payload des notifications push comme image d'expéditeur.  
FR61: La streak active est représentée par une flamme vive, la streak cassée par une flamme éteinte, et chaque badge déclenche une animation.  
FR62: Le formulaire de création d'une tâche est structuré en étapes progressives avec aperçu de la prochaine occurrence.

### NonFunctional Requirements

NFR1: API < 200 ms au p95 sur les opérations courantes.  
NFR2: FCP < 2 s sur connexion 4G simulée.  
NFR3: Synchronisation multi-clients en moins de 2 s.  
NFR4: Badge PWA mis à jour en moins de 1 s après changement de statut.  
NFR5: Notifications push délivrées dans les 30 s suivant le déclenchement du planificateur.  
NFR6: Exports CSV/PDF générés en moins de 10 s sur 12 mois d'historique.  
NFR7: Mots de passe hachés avec Argon2id ou bcrypt fort.  
NFR8: Access token 15 min et refresh token rotatif 30 jours.  
NFR9: Médias en bucket privé avec URLs signées à durée limitée.  
NFR10: TLS 1.2 minimum, TLS 1.3 préféré.  
NFR11: Clés VAPID stockées uniquement côté serveur.  
NFR12: Tokens d'activation et de reset à usage unique, 1 h max.  
NFR13: Disponibilité du service >= 99%.  
NFR14: Aucune perte de données une fois la réponse HTTP 200 renvoyée.  
NFR15: Le planificateur reprend les tâches en attente après redémarrage.  
NFR16: Moteur de récurrence déterministe en timezone `Europe/Paris`.  
NFR17: Architecture supportant une croissance 10x sans refactoring structurel.  
NFR18: Requêtes DB courantes < 50 ms jusqu'à 100 000 occurrences par utilisateur.  
NFR19: Conformité RGAA 4.1 / WCAG AA complète.  
NFR20: Contrastes >= 4.5:1 et navigation clavier complète.  
NFR21: Support `prefers-reduced-motion`.  
NFR22: API jours fériés avec retry exponentiel et dégradation gracieuse.  
NFR23: Purge automatique des subscriptions Web Push invalides.  
NFR24: Hébergement des données en Union Européenne.  
NFR25: Suppression irréversible des données personnelles sous 30 minutes.  
NFR26: Consentement horodaté conservé.

### Additional Requirements

- Monorepo structuré en `apps/web`, `apps/api`, `apps/worker`.  
- Angular 20 LTS pour la PWA.  
- Spring Boot 4.0.3 pour l'API et le worker.  
- PostgreSQL 17.x comme source unique de vérité.  
- Liquibase comme unique mécanisme de migration de schéma.  
- API REST JSON versionnée sous `/api/v1`.  
- Les calculs temporels métier sont exclusivement côté serveur.  
- Tous les jobs worker sont idempotents.  
- Les formats API utilisent `camelCase`; la base utilise `snake_case`.  
- Les statuts métier autorisés sont `planned`, `done`, `missed`, `suspended`, `skipped`, `canceled`.  
- Le worker gère matérialisation des occurrences, scheduling des notifications, sync jours fériés, recalcul streaks/badges/objectifs.  
- Les mutations critiques doivent être rejouables sans double-effet.  
- Les événements internes suivent le format `domain.entity.verb`.  
- Les évolutions de schéma passent obligatoirement par Liquibase.

### UX Design Requirements

UX-DR1: Implémenter une vue Aujourd'hui de type cockpit contextuel avec contexte du jour, progression et tâches ordonnées.  
UX-DR2: Implémenter les composants métier `TaskCard`, `DayContextHeader`, `StreakFlame`, `StepForm`, `DayIndicator`, `NotificationBanner`.  
UX-DR3: Le formulaire de création/édition de tâche doit être un wizard progressif avec aperçu de prochaine occurrence.  
UX-DR4: Les actions critiques d'occurrence doivent passer par une bottom sheet avec hiérarchie claire occurrence seule vs série future.  
UX-DR5: Les feedbacks critiques utilisent toast, banner et notices inline avec message factuel.  
UX-DR6: La navigation principale repose sur 4 onglets : Aujourd'hui, Agenda, Stats/Objectifs, Profil.  
UX-DR7: La palette sémantique doit distinguer jour travaillé, vacances, weekend/férié, exécution, suspension et streak.  
UX-DR8: Tous les composants critiques respectent RGAA/WCAG AA, focus visible, cibles 44x44, labels explicites et compatibilité lecteur d'écran.  
UX-DR13: Les flux critiques n'affichent jamais de codes métiers bruts ; les libellés visibles doivent être en français accentué et compréhensible.  
UX-DR9: Les animations et célébrations respectent `prefers-reduced-motion`.  
UX-DR10: L'agenda doit afficher une heatmap combinant type de jour et statut d'exécution.  
UX-DR11: Les interactions principales doivent rester directes : tap, swipe, long press, action depuis notification.  
UX-DR12: Le produit doit rester mobile-first avec adaptation tablette/desktop sans casser le modèle mental mobile.

### FR Coverage Map

FR1: Epic 1 - Compte, consentement et onboarding  
FR2: Epic 1 - Compte, consentement et onboarding  
FR3: Epic 1 - Compte, consentement et onboarding  
FR4: Epic 1 - Compte, consentement et onboarding  
FR5: Epic 1 - Compte, consentement et onboarding  
FR6: Epic 1 - Compte, consentement et onboarding  
FR7: Epic 1 - Compte, consentement et onboarding  
FR8: Epic 1 - Compte, consentement et onboarding  
FR9: Epic 1 - Compte, consentement et onboarding  
FR10: Epic 1 - Compte, consentement et onboarding  
FR11: Epic 1 - Compte, consentement et onboarding  
FR12: Epic 2 - Tâches et moteur de planification  
FR13: Epic 2 - Tâches et moteur de planification  
FR14: Epic 2 - Tâches et moteur de planification  
FR15: Epic 2 - Tâches et moteur de planification  
FR16: Epic 2 - Tâches et moteur de planification  
FR17: Epic 2 - Tâches et moteur de planification  
FR18: Epic 2 - Tâches et moteur de planification  
FR19: Epic 2 - Tâches et moteur de planification  
FR20: Epic 2 - Tâches et moteur de planification  
FR21: Epic 3 - Exécution quotidienne, streaks et vue Aujourd'hui  
FR22: Epic 3 - Exécution quotidienne, streaks et vue Aujourd'hui  
FR23: Epic 5 - Agenda, objectifs, statistiques et exports  
FR24: Epic 4 - Notifications et engagement multi-appareil  
FR25: Epic 4 - Notifications et engagement multi-appareil  
FR26: Epic 4 - Notifications et engagement multi-appareil  
FR27: Epic 4 - Notifications et engagement multi-appareil  
FR28: Epic 4 - Notifications et engagement multi-appareil  
FR29: Epic 4 - Notifications et engagement multi-appareil  
FR30: Epic 4 - Notifications et engagement multi-appareil  
FR31: Epic 4 - Notifications et engagement multi-appareil  
FR32: Epic 4 - Notifications et engagement multi-appareil  
FR33: Epic 4 - Notifications et engagement multi-appareil  
FR34: Epic 4 - Notifications et engagement multi-appareil  
FR35: Epic 4 - Notifications et engagement multi-appareil  
FR36: Epic 4 - Notifications et engagement multi-appareil  
FR37: Epic 3 - Exécution quotidienne, streaks et vue Aujourd'hui  
FR38: Epic 3 - Exécution quotidienne, streaks et vue Aujourd'hui  
FR39: Epic 3 - Exécution quotidienne, streaks et vue Aujourd'hui  
FR40: Epic 3 - Exécution quotidienne, streaks et vue Aujourd'hui  
FR41: Epic 3 - Exécution quotidienne, streaks et vue Aujourd'hui  
FR42: Epic 5 - Agenda, objectifs, statistiques et exports  
FR43: Epic 5 - Agenda, objectifs, statistiques et exports  
FR44: Epic 5 - Agenda, objectifs, statistiques et exports  
FR45: Epic 3 - Exécution quotidienne, streaks et vue Aujourd'hui  
FR46: Epic 3 - Exécution quotidienne, streaks et vue Aujourd'hui  
FR47: Epic 3 - Exécution quotidienne, streaks et vue Aujourd'hui  
FR48: Epic 5 - Agenda, objectifs, statistiques et exports  
FR49: Epic 5 - Agenda, objectifs, statistiques et exports  
FR50: Epic 5 - Agenda, objectifs, statistiques et exports  
FR51: Epic 6 - Expérience transversale, accessibilité et conformité  
FR52: Epic 5 - Agenda, objectifs, statistiques et exports  
FR53: Epic 5 - Agenda, objectifs, statistiques et exports  
FR54: Epic 5 - Agenda, objectifs, statistiques et exports  
FR55: Epic 5 - Agenda, objectifs, statistiques et exports  
FR56: Epic 5 - Agenda, objectifs, statistiques et exports  
FR57: Epic 6 - Expérience transversale, accessibilité et conformité  
FR58: Epic 1 - Compte, consentement et onboarding  
FR59: Epic 1 - Compte, consentement et onboarding  
FR60: Epic 4 - Notifications et engagement multi-appareil  
FR61: Epic 6 - Expérience transversale, accessibilité et conformité  
FR62: Epic 2 - Tâches et moteur de planification

## Epic List

### Epic 1: Compte, Consentement et Onboarding Contextuel
Les utilisateurs peuvent créer leur compte, consentir au traitement des données, configurer leur contexte de vie et démarrer avec un profil de jours exploitable.
**FRs covered:** FR1, FR2, FR3, FR4, FR5, FR6, FR7, FR8, FR9, FR10, FR11, FR58, FR59

### Epic 2: Tâches et Moteur de Planification
Les utilisateurs peuvent créer et maintenir des tâches ponctuelles ou récurrentes avec des règles temporelles fiables et compréhensibles.
**FRs covered:** FR12, FR13, FR14, FR15, FR16, FR17, FR18, FR19, FR20, FR62

### Epic 3: Exécution Quotidienne, Streaks et Vue Aujourd'hui
Les utilisateurs peuvent piloter leur journée, exécuter ou suspendre leurs occurrences, comprendre le contexte du jour et suivre leurs streaks.
**FRs covered:** FR21, FR22, FR37, FR38, FR39, FR40, FR41, FR45, FR46, FR47

### Epic 4: Notifications et Engagement Multi-Appareil
Les utilisateurs reçoivent des notifications utiles, peuvent agir dessus directement et gardent une compréhension claire de l'état des rappels.
**FRs covered:** FR24, FR25, FR26, FR27, FR28, FR29, FR30, FR31, FR32, FR33, FR34, FR35, FR36, FR60

### Epic 5: Agenda, Objectifs, Statistiques et Exports
Les utilisateurs peuvent explorer leur historique, gérer l'agenda, suivre leurs objectifs, analyser leur performance et exporter leurs données.
**FRs covered:** FR23, FR42, FR43, FR44, FR48, FR49, FR50, FR52, FR53, FR54, FR55, FR56

### Epic 6: Expérience Transversale, Accessibilité et Confiance Produit
Les utilisateurs bénéficient d'une expérience cohérente, accessible, synchronisée et visuellement stable sur tous les contextes d'usage.
**FRs covered:** FR51, FR57, FR61

## Epic 1: Compte, Consentement et Onboarding Contextuel

Permettre à un utilisateur de créer son compte, activer sa session, configurer ses paramètres de base et établir le contexte initial de planification.

### Story 1.1: Inscription, consentement et activation de compte

As a visitor,  
I want to create my account and activate it securely,  
So that I can start using IA with my own protected data.

**Acceptance Criteria:**

**Given** a visitor is on the registration flow  
**When** they submit pseudo, email, password, date of birth, consent, optional photo and geographic zone  
**Then** the API creates the pending account and stores a timestamped consent record  
**And** an activation email with a one-time token expiring in 1 hour is sent  
**And** the user cannot authenticate before activation  
**And** the initial Liquibase changelog creates only the account, consent, refresh token, asset and holiday-sync prerequisite tables needed by this story.

### Story 1.2: Connexion, refresh token et réinitialisation de mot de passe

As an activated user,  
I want to log in, stay signed in securely and recover access if needed,  
So that my account remains usable without weakening security.

**Acceptance Criteria:**

**Given** an activated account exists  
**When** the user logs in with valid credentials  
**Then** the API returns an access token and sets a secure refresh token flow aligned with Spring Security  
**And** invalid credentials return the standardized API error contract  
**And** the password reset flow issues a single-use token valid for 1 hour  
**And** successful password reset invalidates previous refresh tokens.

### Story 1.3: Profil utilisateur, heures de réveil et zone géographique

As a signed-in user,  
I want to manage my profile, wake-up times and geographic zone,  
So that the app can calculate my schedule correctly.

**Acceptance Criteria:**

**Given** the user is authenticated  
**When** they open the profile area  
**Then** they can edit pseudo, profile photo, zone and one wake-up time per day category  
**And** changing geographic zone requires an explicit impact confirmation  
**And** saved values are persisted in PostgreSQL and immediately reflected in read models used by planning logic.

### Story 1.4: Vacances utilisateur et synchronisation des jours fériés

As a user,  
I want my holidays and vacations to be known by the system,  
So that daily classification is automatic from the start.

**Acceptance Criteria:**

**Given** the account is created with a geographic zone  
**When** onboarding completes  
**Then** the worker fetches official holidays for the configured zone without blocking account usage  
**And** the user can create, edit and remove vacation periods manually  
**And** annual holiday refresh is scheduled  
**And** API failures on holiday sync trigger retry and graceful degradation with a visible profile alert.

### Story 1.5: Mentions légales, confidentialité et suppression irréversible du compte

As a user,  
I want to access legal information and delete my account if I choose,  
So that I retain trust and control over my data.

**Acceptance Criteria:**

**Given** the user is in the app  
**When** they access legal surfaces  
**Then** mentions légales and privacy policy are reachable from the profile and onboarding flows  
**And** account deletion requires explicit confirmation  
**And** deleting the account triggers removal of personal data, subscriptions and private media within the defined retention window  
**And** the user session is revoked immediately.

## Epic 2: Tâches et Moteur de Planification

Permettre aux utilisateurs de définir leurs tâches et aux services backend de produire des occurrences déterministes selon le contexte du jour.

### Story 2.1: Création d'une tâche ponctuelle via wizard progressif

As a user,  
I want to create a one-time task through a guided form,  
So that I can schedule a task without dealing with raw complexity.

**Acceptance Criteria:**

**Given** the user is authenticated  
**When** they launch task creation  
**Then** the UI presents a step-by-step form following the UX specification  
**And** the form captures title, icon, optional photo, description, day categories, day selection, time mode and start date  
**And** start dates in the past are rejected by frontend and backend validation  
**And** the final step shows the next computed occurrence preview before confirmation.

### Story 2.2: Création et validation des tâches récurrentes

As a user,  
I want to define recurring tasks with realistic frequencies,  
So that the app follows my real habits over time.

**Acceptance Criteria:**

**Given** the user is in the task wizard  
**When** they choose a recurring task  
**Then** they can select weekly or monthly recurrence patterns and optional end date  
**And** the backend persists the rule set needed to materialize future occurrences  
**And** effective scheduling uses wake-up offsets or fixed times consistently in `Europe/Paris`  
**And** the domain model stores recurrence rules separately from occurrence overrides.

### Story 2.3: Matérialisation des occurrences et classification des jours

As a user,  
I want the system to prepare the right occurrences for each day automatically,  
So that my day view is always contextualized correctly.

**Acceptance Criteria:**

**Given** tasks, holidays, vacations and wake-up profiles exist  
**When** the worker materializes occurrences on a rolling horizon  
**Then** each day est classifié comme workday, vacation ou weekend/holiday  
**And** tasks whose categories do not apply are materialized as skipped or excluded according to the domain rule  
**And** occurrence computation is deterministic across DST changes  
**And** the scheduling worker can resume after restart without duplicating occurrences.

### Story 2.4: Modification et suppression d'occurrences uniques ou futures

As a user,  
I want to edit or delete one occurrence or all future ones,  
So that I can adapt habits without breaking history.

**Acceptance Criteria:**

**Given** a recurring task exists  
**When** the user edits or deletes an occurrence from the agenda or task flow  
**Then** they must choose between "this occurrence" and "this and following occurrences"  
**And** the backend persists the change as an override or rule mutation consistent with the selected scope  
**And** past occurrences cannot be modified or deleted  
**And** future notifications impacted by the change are recalculated or canceled.  
**And** the dedicated occurrences screen lists future occurrences by default through backend pagination with filters on name, occurrence dates, task type and fixed time or wake-up offset.

## Epic 3: Exécution Quotidienne, Streaks et Vue Aujourd'hui

Permettre à l'utilisateur de vivre la journée comme un cockpit de décision, avec feedback immédiat, progression visible et streaks fiables.

### Story 3.1: Vue Aujourd'hui et contexte de journée

As a user,  
I want to open the app and instantly understand today's context,  
So that I know what matters now without searching.

**Acceptance Criteria:**

**Given** the user has planned occurrences for the current day  
**When** they open the Today view  
**Then** tasks are ordered by effective time  
**And** the screen shows the day context header, active count, skipped count and progress bar  
**And** the bottom navigation exposes Today, Agenda, Stats/Objectifs and Profil  
**And** the view respects the mobile-first cockpit design from the UX specification.

### Story 3.2: Marquage exécuté, non exécuté et suspension d'occurrence

As a user,  
I want to change the status of today's occurrence quickly,  
So that the system reflects what I actually did.

**Acceptance Criteria:**

**Given** a current-day occurrence is still actionable  
**When** the user taps, swipes or long-presses a task card  
**Then** they can mark it `done`, `missed` or `suspended` until midnight  
**And** suspension uses a confirmation flow explaining streak protection  
**And** each successful status change returns a toast and updates the progress bar  
**And** the API mutation is idempotent and durable once HTTP 200 is returned.

### Story 3.3: Badge PWA, synchronisation locale et feedback immédiat

As a user,  
I want all immediate counters and indicators to update right after my action,  
So that I never doubt the system state.

**Acceptance Criteria:**

**Given** an occurrence status changes  
**When** the action is accepted by the backend  
**Then** the Today view, local badge count and task card state update in under 1 second  
**And** the PWA icon badge shows the remaining tasks count  
**And** stale client caches are invalidated predictably  
**And** toast/live-region feedback is emitted accessibly.

### Story 3.4: Streaks, badges et protection contre l'abus de suspension

As a user,  
I want my streaks and badges to reflect my real habit history,  
So that motivation remains fair and trustworthy.

**Acceptance Criteria:**

**Given** the user has historical occurrences  
**When** statuses are recalculated or changed  
**Then** the system computes current streak and longest streak deterministically  
**And** a valid pre-task suspension does not break the streak  
**And** badge milestones are unlocked according to configured thresholds  
**And** repeated suspension abuse triggers a warning indicator on the relevant task.

## Epic 4: Notifications et Engagement Multi-Appareil

Permettre à l'utilisateur de recevoir les bons rappels au bon moment et d'agir sans friction depuis l'appareil.

### Story 4.1: Enrollment Web Push, permission banner et centre de notifications

As a user,  
I want the app to guide me when notifications are unavailable,  
So that I can restore reminders without guessing what is wrong.

**Acceptance Criteria:**

**Given** the PWA is installed or used on a supported device  
**When** notification permission is missing or revoked  
**Then** a persistent banner explains the issue and offers a clear activation path  
**And** successful subscription stores a VAPID-compatible endpoint linked to the user  
**And** every generated notification is recorded in the in-app notification center with status metadata.

### Story 4.2: Scheduling des notifications séquentielles de tâche

As a user,  
I want reminders to be scheduled automatically around each task,  
So that I am reminded before and after the planned time if needed.

**Acceptance Criteria:**

**Given** a future actionable occurrence exists  
**When** the worker prepares notification jobs  
**Then** it creates reminders at -15 min, -2 min, exact time and +1 h  
**And** scheduling uses `Europe/Paris` and survives worker restarts  
**And** notification jobs are idempotent and not duplicated  
**And** delivery timestamps are observable for troubleshooting.

### Story 4.3: Actions depuis notification push et annulation automatique

As a user,  
I want to complete or miss a task directly from the notification,  
So that I do not have to open the app for routine confirmations.

**Acceptance Criteria:**

**Given** the platform supports notification actions  
**When** the user taps `Exécuté` or `Non exécuté` from a push notification  
**Then** the status is persisted through the backend API contract  
**And** the app shows a confirmation toast or equivalent feedback when reopened  
**And** remaining scheduled notifications for the same occurrence are canceled automatically  
**And** the notification payload can include the user's profile photo as sender image where supported.

### Story 4.4: Notifications contextuelles de streak, anniversaire et objectifs

As a user,  
I want secondary reminders to reflect my real progress and context,  
So that the app stays helpful rather than noisy.

**Acceptance Criteria:**

**Given** contextual conditions are met  
**When** the worker evaluates daily and periodic reminder rules  
**Then** it emits a streak danger reminder at 20h when applicable  
**And** anniversary, weekly goal and monthly goal reminders are scheduled according to the PRD  
**And** invalid subscriptions are purged automatically after provider failure  
**And** reminder generation is visible in the notification center.

## Epic 5: Agenda, Objectifs, Statistiques et Exports

Permettre à l'utilisateur d'explorer son passé, de piloter ses objectifs et d'extraire ses données.

### Story 5.1: Agenda semaine/mois avec indicateurs de jour et lecture seule du passé

As a user,  
I want to navigate my agenda visually,  
So that I can inspect past and future activity at different granularities.

**Acceptance Criteria:**

**Given** occurrences exist across multiple dates  
**When** the user opens the agenda  
**Then** month and week views are available with Monday-first conventions  
**And** each day cell shows category and execution state via the agreed visual indicator component  
**And** past occurrences remain consultable in read-only mode  
**And** agenda rendering remains accessible and keyboard operable.

### Story 5.2: Actions contextuelles depuis l'agenda

As a user,  
I want to manage tasks and occurrences from the agenda,  
So that I can adjust my schedule in the context of the calendar.

**Acceptance Criteria:**

**Given** the user is on an agenda cell or occurrence  
**When** they open the contextual action sheet  
**Then** they can create, edit or delete according to permissions and temporal rules  
**And** destructive options clearly separate single occurrence from future series scope  
**And** changes refresh the relevant agenda and Today projections  
**And** past occurrences cannot be edited.

### Story 5.3: Objectifs hebdomadaires et mensuels

As a user,  
I want to define overall or task-specific goals,  
So that I can measure whether my habits meet my targets.

**Acceptance Criteria:**

**Given** the user has eligible tasks and history  
**When** they create weekly or monthly goals  
**Then** they can define a global goal or a task-specific goal  
**And** progress is recalculated in real time from occurrence outcomes  
**And** the UI exposes current status and recent history  
**And** reminder logic for unmet goals is fed from the same goal projection.

### Story 5.4: Dashboard statistiques et KPI comparatifs

As a user,  
I want to analyze my execution history at multiple levels,  
So that I can understand my trends and improve my routine.

**Acceptance Criteria:**

**Given** the user has enough historical data  
**When** they open the stats dashboard  
**Then** they can view weekly, monthly, annual and global statistics  
**And** they can drill down by task  
**And** KPI cards expose N vs N-1 comparisons with charts or equivalent visualizations  
**And** queries remain performantes pour des historiques de 12 mois et au-delà du baseline MVP.

### Story 5.5: Export des données utilisateur

As a user,  
I want to export my history and task data,  
So that I can keep or analyze my data outside the app.

**Acceptance Criteria:**

**Given** the user requests an export  
**When** they choose CSV or PDF output  
**Then** the backend generates the export from authorized user data only  
**And** the payload includes task list, history and statistics requested by scope  
**And** export generation completes within the NFR target on 12 months of history  
**And** the download is auditable and secure.

## Epic 6: Expérience Transversale, Accessibilité et Confiance Produit

Permettre une expérience stable, accessible et cohérente sur plusieurs appareils et contextes d'usage.

### Story 6.1: Synchronisation temps réel entre onglets et appareils

As a user,  
I want my account state to stay consistent across my open sessions,  
So that I can trust what I see on each device.

**Acceptance Criteria:**

**Given** the same user is connected on multiple tabs or devices  
**When** an occurrence status or task change is committed  
**Then** other active sessions receive the update in under 2 seconds  
**And** Today, Agenda, badges and relevant counters refresh without manual reload  
**And** the synchronization mechanism does not create conflicting double-writes.

### Story 6.2: Thème, tokens visuels et composants UX métier

As a user,  
I want the app to feel consistent and readable in light or dark mode,  
So that my daily usage remains comfortable and recognizable.

**Acceptance Criteria:**

**Given** the design token system is applied  
**When** the user follows system theme or toggles manually  
**Then** the app switches between light and dark modes coherently  
**And** semantic colors for day categories, statuses and streaks remain consistent  
**And** `TaskCard`, `DayContextHeader`, `StreakFlame`, `DayIndicator`, `NotificationBanner` and `StepForm` are implemented as reusable components  
**And** visual hierarchy matches the cockpit contextuel direction from the UX spec.

### Story 6.3: Accessibilité et motion-safe sur les flux critiques

As a user with accessibility needs,  
I want all critical flows to remain operable and understandable,  
So that I can use the product without barriers.

**Acceptance Criteria:**

**Given** the user navigates by keyboard, screen reader or reduced-motion preference  
**When** they use onboarding, Today, Agenda, notifications and task forms  
**Then** focus order is logical, visible and trap-free  
**And** controls expose accessible names and text alternatives  
**And** all custom components satisfy 44x44 touch targets and contrast requirements  
**And** animations and celebrations honor `prefers-reduced-motion`.

### Story 6.4: Expression visuelle de la streak et polish final de confiance

As a user,  
I want motivation cues that are clear but not intrusive,  
So that I feel progress without losing trust in the product.

**Acceptance Criteria:**

**Given** streak and badge data are available  
**When** the user views the Today or Stats experiences  
**Then** the active streak uses the live flame visual and broken streak uses the extinguished flame state  
**And** badge milestone celebrations are brief, memorable and dismissible  
**And** all feedback patterns remain factual and non-punitive  
**And** the implementation preserves accessibility and performance budgets.
