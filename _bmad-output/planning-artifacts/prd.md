---
stepsCompleted: ['step-01-init', 'step-02-discovery', 'step-02b-vision', 'step-02c-executive-summary', 'step-03-success', 'step-04-journeys', 'step-05-domain', 'step-06-innovation', 'step-07-project-type', 'step-08-scoping', 'step-09-functional', 'step-10-nonfunctional', 'step-11-polish']
inputDocuments: ['_bmad-output/brainstorming/brainstorming-session-2026-03-24-1430.md']
workflowType: 'prd'
classification:
  projectType: 'web_app'
  domain: 'general'
  complexity: 'medium'
  projectContext: 'greenfield'
briefCount: 0
researchCount: 0
brainstormingCount: 1
projectDocsCount: 0
---

# Product Requirements Document - IA

**Author:** Gino Cachondeo
**Date:** 2026-03-24

## Executive Summary

**IA** est une Progressive Web App mobile-first de gestion d'habitudes et de tâches personnelles, conçue pour s'adapter au rythme de vie réel de l'utilisateur plutôt que de lui imposer un calendrier uniforme. Elle s'adresse à un cercle restreint d'utilisateurs (usage personnel + proches) qui souhaitent construire et maintenir des habitudes durables avec un outil qui comprend leur quotidien.

Le problème résolu : les apps de tâches et rappels existantes traitent tous les jours de manière identique — elles ignorent que l'utilisateur travaille certains jours, est en vacances d'autres, et que les jours fériés modifient son rythme. Il en résulte des notifications hors contexte, une friction de gestion manuelle, et un abandon progressif.

**IA** résout cela en structurant le temps autour de 3 profils de jours (jours travaillés, vacances, weekends+fériés), en synchronisant automatiquement les jours fériés via l'API gouvernementale française (zones métropole et Alsace-Lorraine), et en sautant automatiquement les tâches inadaptées à la catégorie du jour — sans intervention de l'utilisateur.

### Différenciation & Proposition de Valeur

**Différenciation principale :** L'intelligence contextuelle du calendrier couplée à un système de motivation honnête. Là où les concurrents proposent des rappels génériques, IA adapte ce qu'elle demande en fonction de qui vous êtes ce jour-là.

**Insights clés :**
- Les tâches se planifient en heures/minutes après le réveil (configurable par profil de jour) ou à heure fixe — l'heure de la tâche est ancrée dans votre rythme biologique, pas dans un horaire abstrait.
- Le système de streaks distingue l'absence intentionnelle (suspension déclarée) de l'échec passif (non-exécution) — seul l'échec passif casse la streak.
- Les notifications push sont séquentielles (-15 min, -2 min, heure exacte, +1h) et intelligentes : elles s'annulent si la tâche est déjà exécutée.
- Un dashboard KPI complet avec comparaisons N vs N-1 transforme l'app en outil d'auto-analyse comportementale.

**Pourquoi maintenant :** L'API gouvernementale française des jours fériés et la maturité des PWA (notifications push, badge d'icône, installation native) rendent ce niveau de contextualisation possible sans développement natif iOS/Android.

## Project Classification

- **Type de projet :** Web App / PWA (SPA, mobile-first, browser-based)
- **Domaine :** Productivité personnelle / gestion d'habitudes
- **Complexité :** Moyenne — logique métier riche (récurrences, timezone Europe/Paris, notifications séquentielles, gamification) sans contraintes réglementaires
- **Contexte :** Greenfield — nouveau produit from scratch

## Success Criteria

### User Success

- L'utilisateur crée ses tâches récurrentes et ponctuelles sans friction et les retrouve correctement dans l'agenda et la vue "Aujourd'hui"
- Les streaks se calculent et s'affichent fidèlement — la distinction suspension intentionnelle / non-exécution passive fonctionne sans anomalie
- L'utilisateur reçoit les notifications au bon moment (-15 min, -2 min, heure exacte, +1h) et peut interagir avec les boutons Exécuté/Non exécuté directement depuis la notification
- Le skip automatique des tâches sur jours fériés/vacances opère sans intervention manuelle
- Les stats et objectifs reflètent exactement l'historique réel de l'utilisateur

### Business Success

- Zéro bug fonctionnel constaté sur les flux critiques : notifications, streak, récurrences, statut jour-J
- L'app est adoptée et utilisée quotidiennement par l'utilisateur et ses proches sans plainte de dysfonctionnement
- Stabilité à long terme : aucune perte de données, aucune dérive de timezone, aucune notification manquée ou en doublon

### Technical Success

- Tous les temps de chargement < 1 seconde en conditions réseau normales (LCP < 1s, TTI < 1s)
- Fiabilité des notifications push : déclenchement à l'heure exacte (±5 secondes), annulation correcte si tâche déjà exécutée, aucun doublon
- Timezone Europe/Paris respectée sur l'ensemble de la stack (serveur, BDD, moteur de notifications) — aucun bug lors des changements d'heure DST
- API jours fériés synchronisée à la création de compte et refreshée annuellement sans intervention
- JWT valides, sessions sécurisées, isolation totale des données entre comptes

### Measurable Outcomes

- 0 bug fonctionnel critique sur les notifications dans les 30 premiers jours d'utilisation
- 100% des occurrences skip correctement sur jours fériés et jours de vacances déclarés
- Streaks calculées sans erreur sur 100% des jours testés
- Temps de réponse API < 200ms pour les opérations courantes (lecture tâches du jour, mise à jour statut)

## Product Scope

### MVP — Version 1 Complète

Toutes les fonctionnalités en une seule livraison :
- Compte utilisateur (inscription, activation email, connexion, JWT, photo, zone géographique, suppression)
- 3 catégories de jours + configuration des heures de réveil par profil
- Jours fériés auto-fetchés (API gouv, zones métropole/Alsace-Lorraine) + vacances déclarées manuellement
- Tâches ponctuelles et récurrentes (jours fixes, N semaines, N mois) avec date début/fin, icône, photo, description WYSIWYG
- Planification heure fixe ou offset réveil
- Skip automatique par catégorie de jour
- Statut jour-J (exécuté/non exécuté/suspendu) modifiable jusqu'à minuit
- Modification/suppression occurrence unique ou occurrence + suivantes
- Notifications push séquentielles (-15 min, -2 min, heure exacte, +1h) + streak en danger 20h + anniversaire + objectifs non atteints
- Centre de notifications in-app + banner si notifications désactivées
- Vue "Aujourd'hui" (principale) + Agenda mois/semaine (lundi-premier) avec indicateurs visuels par jour
- Streaks (en cours + plus longue) + badges paliers + suspension justifiée protège la streak + warning cooldown
- Système d'objectifs hebdo/mensuel (global + par tâche éligible) avec reporting et export
- Dashboard KPI complet (stats hebdo/mensuel/annuel/global, comparaisons N vs N-1, graphiques)
- Export données (CSV/PDF)
- Thème clair/sombre (détection système, mobile uniquement)
- Navigation 4 onglets : Aujourd'hui / Agenda / Stats+Objectifs / Profil

### Growth Features (Post-MVP)

Aucun — tout est en V1.

### Vision (Future)

Évolutions potentielles selon l'usage réel : widgets natifs, intégration calendrier externe, app mobile native.

## User Journeys

### Journey 1 — Gino, Utilisateur Quotidien (Parcours Nominal)

**Profil :** Gino, 35 ans, développeur. Il utilise IA depuis 3 semaines. Il a configuré 5 tâches récurrentes : méditation (15 min après réveil, jours travaillés), sport (vendredi, 2h après réveil), lecture (tous les jours, heure fixe 21h00), révision projet (lundi+mercredi, jours travaillés), appel famille (dimanche, weekends+fériés).

**Scène d'ouverture :** Lundi matin, 7h13. Gino déverrouille son téléphone. Il voit 2 notifications empilées sur l'icône IA : le badge affiche "3" (3 tâches restantes aujourd'hui).

**Action montante :**
- 7h15 — Notification push : "🧘 Méditation — dans 2 minutes" avec son avatar en icône. Il glisse pour ouvrir.
- 7h17 — Notification : "🧘 Méditation — maintenant". Boutons : **Exécuté** | **Non exécuté**. Il tape Exécuté depuis la notification sans ouvrir l'app. Toast de confirmation : "✓ Méditation marquée exécutée."
- 8h00 — Il ouvre l'app en mode clair. Vue "Aujourd'hui" : barre de progression "1/3 tâches". Méditation affiche une coche verte.
- 20h00 — Il n'a pas fait sa révision projet. Notification : "🔥 Ta streak de 12 jours est en danger — il te reste jusqu'à minuit." Il ouvre l'app et la marque Exécutée.

**Climax :** À 23h30, il consulte l'onglet Stats. Semaine en cours : 89% d'exécution contre 76% la semaine précédente. La flamme de streak pulse — 13 jours.

**Résolution :** Gino n'a géré aucune configuration ce jour — tout s'est passé automatiquement. C'est lundi, donc "jours travaillés" : méditation et révision projet actives, appel famille absent (dimanche uniquement). Zéro friction, zéro décision manuelle.

---

### Journey 2 — Marie, Nouvelle Utilisatrice (Onboarding)

**Profil :** Marie, proche de Gino. Elle vient de créer son compte sur recommandation. Elle veut suivre ses habitudes : course à pied et lecture.

**Scène d'ouverture :** Marie ouvre l'app pour la première fois après avoir cliqué le lien d'activation dans son email.

**Action montante :**
- **Profil :** Elle entre son pseudo, date de naissance, uploade une photo, sélectionne zone "Métropole". L'app fetch silencieusement les jours fériés 2026-2030.
- **Heures de réveil :** 7h00 (jours travaillés), 8h30 (weekends+fériés), 9h00 (vacances).
- **Première tâche :** Formulaire en étapes — titre "Course à pied" → icône 🏃 → catégorie "jours travaillés", jours : lundi+mercredi+vendredi → heure : 45 min après réveil. Aperçu : "Prochaine occurrence : mercredi à 7h45". Elle valide.
- **Deuxième tâche :** "Lecture" — tous les jours, toutes catégories, heure fixe 21h30.

**Climax :** Le mercredi suivant à 7h30, Marie reçoit sa première notification "🏃 Course à pied — dans 15 minutes". Elle réalise que l'app a calculé 7h45 automatiquement depuis son réveil configuré.

**Résolution :** En 10 minutes d'onboarding, Marie a un outil opérationnel adapté à son rythme.

---

### Journey 3 — Gino, Parcours de Friction (Notifications Désactivées + Jour Férié)

**Scène d'ouverture :** Gino a réinstallé l'app après un changement de téléphone. Les notifications push ne sont pas encore autorisées.

**Friction 1 — Banner notifications :**
Un banner orange persistant s'affiche : "⚠️ Notifications désactivées — tu risques de rater tes tâches. Activer →". Il tape, atterrit dans les paramètres système, autorise. Banner disparaît.

**Friction 2 — Jour de l'Ascension :**
C'est le 29 mai (Ascension). L'app l'a automatiquement détecté via l'API gouv — ses tâches "jours travaillés" n'apparaissent pas dans la vue Aujourd'hui. Seule "Lecture" (tous les jours) est affichée. Dans l'agenda, le 29 mai apparaît en gris (weekends+fériés). Aucune notification intempestive.

**Friction 3 — Streak en danger :**
À 20h, Gino n'a pas fait sa Lecture. Notification : "🔥 Streak de 21 jours en danger". Il marque Lecture exécutée à 20h15. Streak sauvée.

**Résolution :** Trois frictions potentielles — toutes gérées automatiquement. Le jour férié était dans la base, la tâche a skip, la streak a été préservée.

---

### Journey Requirements Summary

| Capacité | Révélée par |
|---|---|
| Calcul heure effective (réveil + offset) | Journey 1, 2 |
| Fetch API jours fériés à la création de compte | Journey 2, 3 |
| Skip automatique tâches/catégorie de jour | Journey 1, 3 |
| Notifications séquentielles avec annulation | Journey 1 |
| Notification streak en danger à 20h | Journey 1, 3 |
| Toast de confirmation depuis notification | Journey 1 |
| Banner notifications désactivées | Journey 3 |
| Vue "Aujourd'hui" avec barre de progression | Journey 1, 2 |
| Formulaire création tâche en étapes avec aperçu | Journey 2 |
| Agenda avec indicateurs visuels par catégorie de jour | Journey 3 |
| Onboarding configuration réveil | Journey 2 |

## Domain-Specific Requirements

### Conformité RGPD

- Politique de confidentialité accessible depuis l'app (avant inscription et depuis le profil)
- Consentement explicite à la collecte de données lors de l'inscription
- Droit à l'effacement : la suppression de compte efface toutes les données personnelles (compte, tâches, historique, photos) de manière immédiate et irréversible
- Aucune donnée personnelle partagée avec des tiers
- Les données sont hébergées en Europe
- Mentions légales accessibles depuis l'app

### Sécurité des Données

- **Mots de passe :** Hachage avec bcrypt ou Argon2 (jamais stockés en clair). Longueur minimale : 8 caractères. Validation côté serveur obligatoire.
- **Photos de profil et de tâches :** Stockage sécurisé (bucket privé, accès par URL signée temporaire). Taille max par photo : à définir lors de l'architecture. Format accepté : JPEG, PNG, WebP.
- **JWT :** Tokens signés avec secret fort, expiration courte (access token + refresh token). Stockage sécurisé côté client (httpOnly cookie ou secure storage).
- **HTTPS obligatoire** sur l'ensemble des endpoints — aucune communication non chiffrée acceptée.
- **Isolation des données :** Toutes les requêtes API vérifient que la ressource appartient à l'utilisateur authentifié — aucune fuite de données inter-comptes possible.

### Notifications Push (Web Push API)

- **Prérequis techniques :** HTTPS obligatoire + Service Worker enregistré + permission push accordée par l'utilisateur.
- **Protocole :** Web Push API avec VAPID — clés publique/privée générées côté serveur.
- **Flux d'abonnement :** À la première connexion, l'app demande la permission push. L'endpoint de souscription est stocké en base associé au compte utilisateur.
- **Déclenchement :** Notifications planifiées côté serveur (cron/job scheduler) en timezone Europe/Paris. Le serveur push envoie la notification à l'endpoint du navigateur à l'heure calculée.
- **Payload :** Titre, corps, icône de la tâche, badge, boutons d'action (Exécuté / Non exécuté) via l'API Notification Actions.
- **Limitations plateforme :** iOS 16.4+ requis pour les notifications push PWA. Android Chrome : supporté. Boutons d'action non garantis sur tous les navigateurs — fallback vers ouverture de l'app si non supporté.
- **Gestion des échecs :** Si l'endpoint est invalide (appareil changé, permission révoquée), le serveur supprime l'abonnement et le banner de réactivation s'affiche dans l'app au prochain accès.

### Risques et Mitigations

| Risque | Mitigation |
|---|---|
| Bug timezone lors du changement d'heure DST | Tout calculé en Europe/Paris serveur-side, jamais côté client |
| Notification push non délivrée (iOS restrictions) | Centre de notifications in-app comme fallback |
| Photo uploadée trop lourde | Compression/resize côté client avant upload |
| JWT compromis | Refresh token rotation + révocation sur suppression de compte |

## Innovation & Novel Patterns

### Detected Innovation Areas

**Intelligence contextuelle du calendrier**
La combinaison "3 profils de jours + skip automatique des tâches selon la catégorie du jour + heure ancrée sur le réveil" est inédite dans les apps de productivité grand public. Les tâches ne sont pas positionnées dans un calendrier — elles suivent le rythme de vie biologique et contextuel de l'utilisateur. Un lundi de travail et un lundi de vacances produisent automatiquement des agendas différents sans aucune intervention manuelle.

**Gamification honnête**
Le système de streaks distingue explicitement l'absence intentionnelle (suspension déclarée avant l'heure de la tâche) de l'échec passif (non-exécution sans action). Seul l'échec passif casse la streak. Cette nuance est absente des apps de suivi d'habitudes existantes (Habitica, Streaks, Duolingo) qui traitent toute absence comme un échec. L'approche récompense la conscience de soi plutôt que la performance brute.

**Notifications séquentielles intelligentes**
La combinaison 4 niveaux de notification par tâche (-15 min, -2 min, heure exacte, +1h) avec annulation automatique à l'exécution, plus les conditions précises de déclenchement de la notif streak (streak active + tâches non exécutées), dépasse le niveau de sophistication habituel des PWA.

### Validation Approach

- Validation par l'usage quotidien de l'auteur — l'app est son propre outil de travail
- Le skip automatique est validable dès le premier jour férié suivant le lancement
- La gamification honnête est validée si l'utilisateur maintient une streak plus longtemps qu'avec une app standard (mesurable via les stats)

### Risk Mitigation

| Innovation | Risque | Mitigation |
|---|---|---|
| Skip automatique | Tâche skippée à tort (mauvaise catégorie configurée) | UI claire de la catégorie du jour dans la vue Aujourd'hui + agenda chromé |
| Suspension protège la streak | Abus (suspension systématique pour ne jamais casser) | Warning cooldown après N suspensions / 30 jours |
| Notifications séquentielles | Surcharge perçue comme intrusive | Toutes les notifs d'une tâche s'annulent dès qu'elle est exécutée |

## Web App / PWA Specific Requirements

### Project-Type Overview

Application SPA (Single Page Application) Progressive Web App, mobile-first avec support desktop complet. Routing client-side, installable sur écran d'accueil, notifications push natives via Web Push API. Aucune page publique indexable — l'app est entièrement derrière authentification.

### Browser Matrix

| Navigateur | Mobile | Desktop | Priorité |
|---|---|---|---|
| Chrome | ✅ Requis | ✅ Requis | P1 |
| Safari | ✅ Requis (iOS 16.4+ pour push) | ✅ Requis | P1 |
| Firefox | ✅ Requis | ✅ Requis | P1 |
| Edge | ⚪ Optionnel | ⚪ Optionnel | P2 |

**Contrainte Safari iOS :** Les notifications push PWA nécessitent iOS 16.4 minimum ET que l'app soit installée sur l'écran d'accueil. À documenter dans l'onboarding.

### Responsive Design

- **Mobile-first** : breakpoints pensés mobile avant desktop
- Thème clair/sombre : détection système (`prefers-color-scheme`) sur mobile, switch manuel dans les paramètres
- Boutons d'action pleine largeur sur mobile (touch targets ≥ 44px)
- Modales fermables par swipe down sur mobile
- Pickers natifs pour les heures sur mobile
- Interface desktop fonctionnelle mais non prioritaire sur les optimisations d'espace

### Performance Targets

- **LCP (Largest Contentful Paint) :** < 1s sur connexion 4G
- **TTI (Time to Interactive) :** < 1s
- **Réponse API :** < 200ms pour les opérations courantes
- **Bundle size :** optimisé via code splitting et lazy loading par route
- **PWA installable :** Web App Manifest complet (icônes, couleurs, display standalone)
- **Service Worker :** enregistré pour notifications push — pas de cache offline (app connectée uniquement)

### Real-Time Requirements

- **Synchronisation temps réel obligatoire** entre onglets/appareils simultanés
- Mise à jour du statut d'une tâche propagée immédiatement à tous les clients connectés du même compte
- Mise à jour du badge PWA et de la barre de progression en temps réel
- **Matérialisation des occurrences en quasi temps réel** après création, modification, suppression scoped d'une tâche ou changement de contexte calendrier impactant les occurrences futures
- **Pas d'attente d'un batch quotidien** pour voir apparaître une nouvelle tâche dans les vues qui consomment les occurrences matérialisées
- Implémentation : WebSocket ou Server-Sent Events (SSE) — à décider lors de l'architecture
- Pas de polling — latence maximale acceptable : 2 secondes

### SEO Strategy

Aucune — l'app est entièrement privée. Pas de pages publiques indexables en V1.

### Accessibility Level — RGAA 100%

Conformité **RGAA 100%** (équivalent WCAG 2.1 AA complet avec exigences françaises).

- Tous les éléments interactifs accessibles au clavier (focus visible, ordre logique)
- Labels ARIA sur tous les composants interactifs (boutons, inputs, modales, notifications)
- Contrastes conformes RGAA (ratio ≥ 4.5:1 texte normal, ≥ 3:1 texte large)
- Alternatives textuelles sur toutes les images et icônes fonctionnelles
- ARIA live regions pour les mises à jour dynamiques (statut tâche, streak, toast)
- Modales avec focus trap et restauration du focus à la fermeture
- Formulaires avec labels explicites et messages d'erreur associés aux champs
- Structure sémantique HTML correcte (headings, landmarks, listes)
- Compatibilité lecteurs d'écran : VoiceOver iOS/macOS, TalkBack Android, NVDA/JAWS Windows
- Respect de `prefers-reduced-motion` pour les animations

## Project Scoping & Phased Development

### MVP Strategy & Philosophy

**Approche MVP :** Experience MVP — livraison de l'expérience utilisateur complète en une seule version. Justifiée par le contexte d'usage personnel : l'auteur est l'utilisateur principal, le périmètre est parfaitement défini, aucune validation marché externe requise.

**Ressources :** Développeur solo. Architecture et stack à choisir pour maximiser la productivité individuelle.

### MVP Feature Set (Phase 1 = V1 Complète)

**Parcours utilisateurs supportés :** Tous les journeys documentés — utilisateur quotidien, onboarding nouvel utilisateur, parcours de friction.

**Périmètre :** L'intégralité des fonctionnalités listées dans `## Product Scope` est livrée en V1. Voir cette section pour le détail exhaustif.

### Post-MVP Features

**Phase 2 (Évolutions futures selon usage réel) :**
- Widgets natifs (iOS/Android)
- Landing page publique

**Phase 3 (Vision long terme) :**
- Intégration calendrier externe (Google Cal, Outlook)
- App mobile native

### Risk Mitigation Strategy

| Risque | Mitigation |
|---|---|
| Notifications push non fiables sur iOS | Tests sur appareils réels iOS 16.4+ dès le début. Fallback centre de notifications in-app. |
| Complexité logique récurrences + timezone | Tests unitaires exhaustifs sur le moteur de calcul d'occurrences en priorité |
| Synchronisation temps réel | Prototype early WebSocket/SSE avant de construire l'UI dessus |
| RGAA 100% coûteux en temps | Intégrer l'accessibilité dès les premiers composants, pas en fin de projet |
| Développeur solo | Prioriser stabilité et tests sur flux critiques (notifications, streaks, récurrences) |

## Functional Requirements

### Gestion de Compte & Authentification

- **FR1 :** Un visiteur peut créer un compte avec pseudo, email, mot de passe, date de naissance, photo optionnelle et zone géographique (Métropole ou Alsace-Lorraine)
- **FR2 :** Un visiteur reçoit un email d'activation après inscription et doit activer son compte avant de pouvoir se connecter
- **FR3 :** Un utilisateur peut se connecter avec son email et mot de passe via un système JWT
- **FR4 :** Un utilisateur peut demander la réinitialisation de son mot de passe via un email sécurisé
- **FR5 :** Un utilisateur peut modifier ses informations de profil (pseudo, photo, zone géographique, heures de réveil)
- **FR6 :** Un utilisateur peut supprimer son compte et toutes ses données de manière immédiate et irréversible
- **FR7 :** Un utilisateur peut modifier sa zone géographique avec confirmation explicite de l'impact sur les jours fériés futurs

### Gestion du Calendrier & Catégories de Jours

- **FR8 :** Un utilisateur peut configurer une heure de réveil distincte pour chaque catégorie de jour (jours travaillés, vacances, weekends+fériés)
- **FR9 :** Un utilisateur peut déclarer des jours de vacances dans l'application
- **FR10 :** Le système récupère automatiquement les jours fériés depuis l'API gouvernementale française selon la zone géographique de l'utilisateur à la création de compte
- **FR11 :** Le système rafraîchit annuellement les jours fériés disponibles
- **FR12 :** Le système classifie automatiquement chaque jour selon sa catégorie et skip les tâches dont la catégorie ne correspond pas à la catégorie du jour

### Gestion des Tâches

- **FR13 :** Un utilisateur peut créer une tâche ponctuelle avec titre, icône, photo optionnelle, description WYSIWYG, catégorie(s) de jours cible, jour(s) de la semaine, heure (fixe ou offset après réveil) et date de début
- **FR14 :** Un utilisateur peut créer une tâche récurrente avec toutes les propriétés d'une tâche ponctuelle plus une fréquence (jours fixes de la semaine, toutes les N semaines, tous les N mois) et une date de fin optionnelle
- **FR14b :** Le système matérialise immédiatement les occurrences futures d'une tâche nouvellement créée afin qu'elle apparaisse sans délai dans les vues basées sur les occurrences
- **FR15 :** Un utilisateur ne peut pas créer une tâche avec une date de début dans le passé
- **FR16 :** Un utilisateur peut modifier une occurrence unique d'une tâche récurrente sans affecter les autres occurrences
- **FR17 :** Un utilisateur peut modifier une occurrence et toutes les occurrences futures d'une tâche récurrente à partir d'une date donnée
- **FR18 :** Un utilisateur peut supprimer une occurrence unique sans affecter les autres occurrences
- **FR19 :** Un utilisateur peut supprimer une occurrence et toutes les occurrences futures
- **FR20 :** Un utilisateur ne peut pas modifier ou supprimer une occurrence dont la date est passée
- **FR21 :** Un utilisateur peut suspendre une occurrence pour un jour spécifique
- **FR22 :** Un utilisateur peut marquer une tâche comme exécutée ou non exécutée pour le jour en cours jusqu'à minuit
- **FR23 :** Un utilisateur peut consulter les tâches ponctuelles et occurrences passées en lecture seule depuis l'agenda

### Notifications & Alertes

- **FR24 :** Le système envoie une notification push 15 minutes avant l'heure programmée d'une tâche
- **FR25 :** Le système envoie une notification push 2 minutes avant l'heure programmée d'une tâche
- **FR26 :** Le système envoie une notification push à l'heure exacte d'une tâche avec boutons d'action Exécuté et Non exécuté
- **FR27 :** Le système envoie une notification push 1 heure après l'heure programmée si la tâche n'est pas marquée exécutée
- **FR28 :** Le système annule automatiquement toutes les notifications restantes d'une tâche dès qu'elle est marquée exécutée
- **FR29 :** Le système envoie une notification push à 20h si la streak est active et qu'aucune tâche du jour n'est exécutée
- **FR30 :** Le système envoie une notification push le jour d'anniversaire de l'utilisateur 1 heure après l'heure de réveil configurée
- **FR31 :** Le système envoie une notification push le dimanche soir si un objectif hebdomadaire n'est pas atteint
- **FR32 :** Le système envoie une notification push le dernier jour du mois si un objectif mensuel n'est pas atteint
- **FR33 :** Un utilisateur peut interagir avec les boutons d'action directement depuis la notification push sans ouvrir l'application
- **FR34 :** L'application affiche une confirmation (toast) après une action effectuée depuis une notification push
- **FR35 :** L'application affiche un banner persistant si les notifications push sont désactivées sur l'appareil
- **FR36 :** L'application maintient un centre de notifications in-app listant toutes les notifications générées avec leur statut

### Gamification & Motivation

- **FR37 :** Le système calcule et affiche en temps réel la streak en cours (jours consécutifs avec au moins une tâche exécutée)
- **FR38 :** Le système calcule et affiche la streak la plus longue de l'historique
- **FR39 :** Une suspension d'occurrence déclarée avant l'heure programmée de la tâche ne rompt pas la streak
- **FR40 :** Le système débloque des badges visuels à des jalons de streak prédéfinis
- **FR41 :** Le système affiche un indicateur d'alerte sur une tâche suspendue trop fréquemment sur une période glissante
- **FR42 :** Un utilisateur peut créer des objectifs de nombre de tâches à exécuter par semaine ou par mois
- **FR43 :** Un utilisateur peut créer des objectifs associés à une tâche récurrente spécifique éligible (au moins hebdomadaire)
- **FR44 :** Le système affiche la progression des objectifs en temps réel avec indication visuelle du statut

### Agenda & Visualisation

- **FR45 :** Un utilisateur peut voir les tâches du jour courant ordonnées par heure effective dans une vue principale
- **FR46 :** La vue principale affiche une barre de progression du nombre de tâches exécutées sur le total du jour
- **FR47 :** Le badge de l'icône PWA affiche le nombre de tâches restantes du jour en temps réel
- **FR48 :** Un utilisateur peut visualiser son agenda en vue mois (par défaut, lundi-premier) et vue semaine (lundi-dimanche)
- **FR49 :** L'agenda affiche pour chaque jour un indicateur visuel de statut d'exécution et sa catégorie de jour avec code couleur
- **FR50 :** Un utilisateur peut créer, modifier et supprimer des tâches et occurrences depuis l'agenda via une modale contextuelle
- **FR50b :** Lors de l'édition d'une occurrence récurrente sur le scope "cette occurrence et les suivantes", l'utilisateur peut modifier la date de fin, les jours de semaine, les jours concernés et conserver le mode horaire d'origine sans bascule implicite
- **FR50c :** L'écran "Gérer les occurrences" retourne par défaut uniquement les occurrences futures, sous forme de liste paginée côté backend, avec recherche par nom, filtres par fenêtre de dates d'occurrence, par type (ponctuelle ou récurrente avec date de fin supérieure à une date choisie) et par heure fixe ou décalage réveil
- **FR51 :** L'application synchronise les vues en temps réel entre onglets et appareils simultanés du même compte
- **FR51b :** Le worker maintient une réconciliation continue des occurrences matérialisées comme filet de sécurité, sans dépendre exclusivement d'un run nocturne

### Statistiques & Reporting

- **FR52 :** Un utilisateur peut consulter ses statistiques d'exécution à granularité hebdomadaire, mensuelle, annuelle et globale
- **FR53 :** Un utilisateur peut consulter ses statistiques par tâche individuelle avec drill-down
- **FR54 :** Le dashboard affiche des indicateurs KPI complets avec comparaisons N vs N-1 et graphiques
- **FR55 :** Un utilisateur peut exporter son historique, ses statistiques et sa liste de tâches
- **FR56 :** Le dashboard affiche le suivi des objectifs avec progression et historique

### Personnalisation & Conformité

- **FR57 :** Un utilisateur peut basculer entre le thème clair et sombre, avec détection automatique du thème système
- **FR58 :** Un utilisateur peut accéder aux mentions légales et à la politique de confidentialité depuis l'application
- **FR59 :** L'application recueille le consentement explicite à la collecte de données lors de l'inscription
- **FR60 :** La photo de profil de l'utilisateur est incluse dans le payload des notifications push comme image d'expéditeur
- **FR61 :** La streak active est représentée par une flamme vive, la streak cassée par une flamme éteinte ; chaque déverrouillage de badge de palier déclenche une animation de célébration
- **FR62 :** Le formulaire de création d'une tâche est structuré en étapes progressives (titre+icône → catégorie+jours → heure → description/photo) et affiche un aperçu de la prochaine occurrence calculée avec l'heure effective avant confirmation

## Non-Functional Requirements

### Performance

- **NFR-P1 :** Les API répondent en moins de 200ms au 95e percentile en charge normale
- **NFR-P2 :** Le First Contentful Paint (FCP) est inférieur à 2s sur connexion 4G simulée (réseau throttlé)
- **NFR-P3 :** Les changements de statut de tâche se propagent à tous les clients connectés en moins de 2s via WebSocket ou SSE
- **NFR-P3b :** Une nouvelle tâche ou une mutation de règle impactant le futur devient visible dans les vues basées sur `task_occurrences` en moins de 2s au nominal
- **NFR-P4 :** Le badge PWA se met à jour en moins de 1s après un changement de statut de tâche
- **NFR-P5 :** Les notifications push sont délivrées dans les 30s suivant le déclenchement du planificateur
- **NFR-P6 :** Les exports CSV et PDF se génèrent en moins de 10s pour un historique de 12 mois complet

### Sécurité

- **NFR-S1 :** Les mots de passe sont hachés avec bcrypt (facteur de coût ≥ 12) ou Argon2id avant stockage
- **NFR-S2 :** L'authentification JWT utilise des access tokens de 15 minutes avec rotation des refresh tokens de 30 jours ; un refresh token compromis invalide toute la session
- **NFR-S3 :** Les photos utilisateurs sont stockées dans un bucket privé, accessibles uniquement via des URLs signées à durée de vie limitée (≤ 1h)
- **NFR-S4 :** Toutes les communications client-serveur utilisent HTTPS avec TLS 1.2 minimum (TLS 1.3 préféré)
- **NFR-S5 :** Les clés privées VAPID pour Web Push sont stockées exclusivement côté serveur et ne sont jamais exposées au client
- **NFR-S6 :** Les tokens d'activation de compte et de réinitialisation de mot de passe sont à usage unique et expirent après 1h

### Fiabilité

- **NFR-R1 :** Disponibilité du service ≥ 99% mesurée sur une période glissante de 30 jours (hors maintenance planifiée)
- **NFR-R2 :** Aucune perte de données confirmées (actions Exécuté/Non exécuté) une fois la réponse HTTP 200 retournée au client
- **NFR-R3 :** Le planificateur de notifications reprend les tâches en attente sans perte après un redémarrage serveur
- **NFR-R4 :** Le moteur de récurrences produit des résultats déterministes et identiques quelle que soit la timezone système lors des recalculs ; tous les calculs utilisent la timezone Europe/Paris
- **NFR-R5 :** L'interface utilisateur n'expose jamais les codes métiers bruts (`WORKDAY`, `VACATION`, `WEEKEND_HOLIDAY`) et présente des libellés français accentués cohérents sur tous les flux critiques

### Scalabilité

- **NFR-SC1 :** L'architecture supporte une croissance de 10x du nombre d'utilisateurs actifs sans refactoring structurel
- **NFR-SC2 :** La base de données maintient des temps de requête inférieurs à 50ms pour les opérations courantes jusqu'à 100 000 occurrences de tâches par utilisateur (index appropriés sur user_id, date, status)
- **NFR-SC2b :** La pagination des occurrences supporte des tailles de page 5, 10, 25, 50 et 100 sans dégradation perceptible de l'UI sur mobile

### Accessibilité

- **NFR-A1 :** L'application est conforme RGAA 4.1 (équivalent WCAG 2.1 AA) à 100% — validé par audit outillé (axe, Lighthouse) et test manuel avec lecteur d'écran
- **NFR-A2 :** Tous les éléments textuels et interactifs respectent un ratio de contraste ≥ 4.5:1 (texte normal) et ≥ 3:1 (texte large et composants UI)
- **NFR-A3 :** La navigation est entièrement opérable au clavier — focus visible, ordre logique, pas de piège au clavier
- **NFR-A4 :** Tous les contenus non textuels (icônes, images) disposent d'un équivalent textuel accessible (attribut alt ou aria-label)
- **NFR-A5 :** Les animations et transitions respectent la préférence système `prefers-reduced-motion` en les désactivant ou réduisant à l'essentiel

### Intégrations Externes

- **NFR-I1 :** L'intégration avec l'API jours fériés (calendrier.api.gouv.fr) gère les erreurs réseau avec retry exponentiel (3 tentatives) sans bloquer la création de compte ni l'expérience utilisateur
- **NFR-I2 :** L'indisponibilité de l'API jours fériés déclenche une dégradation gracieuse : création de compte avec liste vide, alerte dans le profil pour relancer la synchronisation manuellement
- **NFR-I3 :** Les abonnements Web Push utilisent le protocole VAPID (RFC 8292) ; les subscriptions expirées ou révoquées par l'appareil sont purgées automatiquement

### Conformité RGPD

- **NFR-G1 :** Toutes les données utilisateurs (compte, tâches, photos, statistiques) sont hébergées dans l'Union Européenne
- **NFR-G2 :** La suppression de compte entraîne la suppression définitive et irréversible de toutes les données personnelles dans un délai maximum de 30 minutes, y compris les photos du bucket de stockage
- **NFR-G3 :** Un enregistrement de consentement horodaté (date, version des CGU, adresse IP hashée) est conservé pour chaque compte pour la durée légale de prescription
