---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - C:\Dev\personal-Agenda\_bmad-output2\planning-artifacts\product-brief-focuslock-ios-2026-03-28.md
  - C:\Dev\personal-Agenda\_bmad-output2\planning-artifacts\prd-focuslock-ios-2026-03-28.md
  - C:\Dev\personal-Agenda\_bmad-output2\planning-artifacts\ux-design-specification.md
  - C:\Dev\personal-Agenda\_bmad-output2\planning-artifacts\architecture-focuslock-ios-2026-03-28.md
---

# FocusLock iPhone - Epic Breakdown

## Overview

Ce document decoupe le MVP en epics et user stories actionnables pour une application iPhone de restriction d'apps et de sites, avec Angular, Spring et une couche native iOS obligatoire.

## Requirements Inventory

### Functional Requirements

- onboarding clair et honest marketing
- compte utilisateur et synchronisation
- permissions Screen Time
- selection apps/sites/categories
- limites quotidiennes
- blocages horaires
- shield personnalise
- contournement avec friction
- dashboard et insights

### NonFunctional Requirements

- securite
- confidentialite
- clarte sur les limites techniques
- accessibilite
- resilience de synchronisation

### Additional Requirements

- cible iPhone uniquement
- stack Angular + Spring
- ne pas promettre un blocage via PWA pure

### UX Design Requirements

- experience adulte, calme, volontaire
- temps jusqu'a premiere regle tres court
- etat iPhone et permissions visible partout

## Epic List

- Epic 1: Fondations produit et experience de base
- Epic 2: Integration iPhone et autorisations Apple
- Epic 3: Moteur de regles de limitation d'apps
- Epic 4: Blocage de sites web et shielding
- Epic 5: Dashboard, insights et contournements

## Epic 1: Fondations produit et experience de base

Poser le socle Angular + Spring, le compte utilisateur et le parcours de premiere activation.

### Story 1.1: Initialiser le frontend Angular/PWA

As a utilisateur,
I want ouvrir une interface moderne installable,
So that je puisse gerer mes regles rapidement.

**Acceptance Criteria:**

**Given** un nouveau visiteur
**When** il ouvre le produit
**Then** l'application charge une landing et un shell Angular responsives
**And** la promesse produit precise que l'enforcement iPhone necessite une couche native

### Story 1.2: Mettre en place l'authentification Spring

As a utilisateur,
I want creer un compte et me connecter,
So that mes regles soient sauvegardees.

**Acceptance Criteria:**

**Given** un utilisateur non authentifie
**When** il s'inscrit ou se connecte
**Then** le backend cree ou authentifie son compte
**And** le frontend recupere une session valide

### Story 1.3: Afficher l'etat de l'appareil et de la synchronisation

As a utilisateur,
I want voir si mon iPhone est bien relie et autorise,
So that je sache si mes blocages sont reellement actifs.

**Acceptance Criteria:**

**Given** un utilisateur connecte
**When** il ouvre le dashboard
**Then** il voit un indicateur de connexion appareil et de permissions
**And** un etat degrade est affiche si l'enforcement n'est pas disponible

## Epic 2: Integration iPhone et autorisations Apple

Rendre le produit capable d'obtenir les autorisations Apple et de communiquer avec le backend.

### Story 2.1: Demander l'autorisation Family Controls

As a utilisateur iPhone,
I want autoriser FocusLock a gerer mes restrictions,
So that l'app puisse appliquer les limites.

**Acceptance Criteria:**

**Given** l'app iOS est installee
**When** l'utilisateur lance l'onboarding d'autorisation
**Then** la demande Apple est declenchee
**And** le statut d'autorisation est remonte au backend

### Story 2.2: Lier l'iPhone au compte utilisateur

As a utilisateur,
I want relier mon iPhone a mon compte,
So that mes regles web et mobile soient synchronisees.

**Acceptance Criteria:**

**Given** un utilisateur authentifie
**When** il termine la liaison appareil
**Then** un device iOS est cree cote backend
**And** il apparait comme actif dans le dashboard

### Story 2.3: Selectionner des cibles de restriction sur iPhone

As a utilisateur,
I want choisir les apps, categories et domaines web a limiter,
So that je configure mes distractions reelles.

**Acceptance Criteria:**

**Given** l'autorisation Apple est accordee
**When** l'utilisateur ouvre le selecteur de cibles
**Then** il peut choisir des apps, categories ou domaines supportes
**And** la selection est sauvegardee dans une regle

## Epic 3: Moteur de regles de limitation d'apps

Permettre la creation et l'application de limites quotidiennes et de plages horaires.

### Story 3.1: Creer une limite quotidienne par app

As a utilisateur,
I want definir Instagram a 30 minutes par jour,
So that mon usage reste sous controle.

**Acceptance Criteria:**

**Given** une app cible a ete selectionnee
**When** l'utilisateur enregistre une limite de 30 minutes
**Then** la regle est stockee cote backend
**And** l'app iOS la synchronise et l'applique

### Story 3.2: Creer un blocage horaire recurrent

As a utilisateur,
I want bloquer des apps pendant mes heures de travail,
So that je protege mes plages de concentration.

**Acceptance Criteria:**

**Given** l'utilisateur choisit une cible et des jours
**When** il configure une plage horaire recurrente
**Then** le systeme enregistre le planning
**And** l'enforcement est applique au bon moment

### Story 3.3: Prevenir avant depassement de limite

As a utilisateur,
I want etre averti avant d'etre bloque,
So that je puisse sortir proprement de l'app.

**Acceptance Criteria:**

**Given** une limite quotidienne active
**When** l'utilisateur approche du seuil defini
**Then** une notification ou un feedback visible est emis
**And** l'evenement est journalise

## Epic 4: Blocage de sites web et shielding

Appliquer les restrictions web et l'ecran de blocage personnalise.

### Story 4.1: Ajouter des domaines web a bloquer

As a utilisateur,
I want saisir des domaines web distractifs,
So that je puisse les rendre inaccessibles selon mes regles.

**Acceptance Criteria:**

**Given** un utilisateur edite une regle
**When** il ajoute un ou plusieurs domaines
**Then** les domaines sont valides et sauvegardes
**And** l'app iOS recupere la configuration applicable

### Story 4.2: Afficher un shield personnalise

As a utilisateur,
I want voir un ecran de blocage clair,
So that je comprenne pourquoi l'acces est refuse.

**Acceptance Criteria:**

**Given** une cible restreinte est ouverte apres depassement ou pendant un blocage
**When** l'acces est refuse
**Then** un shield FocusLock est affiche
**And** le nom de la regle et le contexte du blocage sont visibles

### Story 4.3: Gerer les limites de capacite et d'erreur

As a utilisateur,
I want etre informe quand une restriction ne peut pas etre appliquee,
So that je sache qu'une action est necessaire.

**Acceptance Criteria:**

**Given** une contrainte technique empeche une application correcte
**When** l'etat de la regle est degrade
**Then** le systeme remonte une erreur comprehensible
**And** propose une action de resolution

## Epic 5: Dashboard, insights et contournements

Donner a l'utilisateur du feedback utile sans alourdir l'experience.

### Story 5.1: Construire le dashboard quotidien

As a utilisateur,
I want voir mon etat du jour en un coup d'oeil,
So that je sache ou j'en suis face a mes limites.

**Acceptance Criteria:**

**Given** des regles et evenements existent
**When** l'utilisateur ouvre l'accueil
**Then** il voit le temps consomme, les alertes et les prochaines restrictions
**And** les donnees proviennent de l'API Spring

### Story 5.2: Autoriser un override avec friction

As a utilisateur,
I want pouvoir contourner exceptionnellement un blocage,
So that le systeme reste utilisable sans devenir trop permissif.

**Acceptance Criteria:**

**Given** une regle autorise un override
**When** l'utilisateur demande une exception
**Then** une friction configurable est imposee
**And** l'action est journalisee avec horodatage

### Story 5.3: Afficher les insights hebdomadaires

As a utilisateur,
I want comprendre mes tendances d'usage,
So that j'ajuste mes regles intelligemment.

**Acceptance Criteria:**

**Given** des evenements d'usage ont ete synchronises
**When** l'utilisateur consulte Insights
**Then** il voit les principales distractions, l'evolution hebdomadaire et les jours reussis
**And** aucun detail sensible inutile n'est expose

