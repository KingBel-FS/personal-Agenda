---
title: 'Mode Offline PWA Complet'
slug: 'offline-pwa'
created: '2026-04-14'
status: 'ready-for-dev'
stepsCompleted: [1, 2, 3, 4]
tech_stack: ['Angular 20 standalone', 'Custom Service Worker', 'IndexedDB (native)', 'Cache API (native)', 'Spring Boot 3', 'SCSS with CSS tokens']
files_to_modify: ['apps/web/public/sw.js', 'apps/web/src/app/core/app-shell.component.html', 'apps/web/src/app/core/app-shell.component.ts']
files_to_create: ['apps/web/src/app/core/network-status.service.ts', 'apps/web/src/app/core/offline-banner.component.ts']
code_patterns: ['signals for reactive state', 'inject() for DI', '@if control flow', 'BEM-lite CSS (.component--modifier)', 'CSS variables (--warning, --sp-*, --dur)', 'HttpInterceptorFn (functional interceptor)', 'Observable<{ data: T }> API response wrapper']
test_patterns: ['No existing SW tests', 'Component specs use HttpTestingController', 'Jasmine + Angular TestBed']
---

# Tech-Spec: Mode Offline PWA Complet

**Created:** 2026-04-14

## Overview

### Problem Statement

Quand le réseau tombe ou que le serveur est KO, l'app Personal Habit Tracker est totalement inutilisable : aucune page ne charge, aucune donnée n'est accessible, et les notifications push ne partent plus. Pour un outil d'habitudes quotidiennes consulté des dizaines de fois par jour, c'est un deal-breaker.

### Solution

Transformer le service worker existant (`sw.js`) en couche offline complète :
- **Cache statique** : app shell + assets pour que l'app démarre offline
- **Cache API** : stratégie stale-while-revalidate sur tous les GET API pour afficher les données en lecture
- **Queue mutations** : IndexedDB pour stocker les mutations (POST/PUT/PATCH/DELETE) et les rejouer au retour online
- **Notifs locales** : scheduling ON_TIME dans le SW à partir des données cachées quand le push serveur ne peut pas arriver
- **Indicateur UI** : banner discret online/offline

**Contrainte critique** : Zéro modification du code applicatif existant (services, composants, interceptors). Le offline se greffe exclusivement via le SW et de nouveaux fichiers Angular dédiés.

### Scope

**In Scope:**
- Cache app shell (index.html, JS bundles, CSS, fonts, icons, manifest)
- Cache API stale-while-revalidate sur tous les GET `/api/v1/*`
- Queue IndexedDB pour toutes les mutations (POST/PUT/PATCH/DELETE)
- Replay automatique de la queue au retour online (FIFO)
- Résolution de conflits : action utilisateur gagne (last-write-wins)
- Notifications locales ON_TIME dans le SW quand offline
- Indicateur visuel online/offline dans l'app shell
- Le cache reste valide tant qu'il n'y a pas de réseau ou que le serveur est KO (pas de TTL)

**Out of Scope:**
- Notifications séquentielles offline (rappels 5min, 15min après)
- SSE/realtime sync offline
- Création de compte offline
- Sync bi-directionnelle complexe (CRDT, etc.)

## Context for Development

### Codebase Patterns

- **State management** : Signals Angular (`signal()`, `.asReadonly()`) — pas de NgRx ni Subject pour le state
- **DI** : `inject(Service)` dans le constructeur ou en field initializer
- **Templates** : `@if` / `@for` control flow (Angular 17+)
- **CSS** : BEM-lite (`.notif-banner--warning`, `.offline-banner__text`), tokens CSS (`--warning`, `--sp-4`, `--dur`), dark mode via `[data-theme="dark"]`
- **API** : Tous les services retournent `Observable<{ data: T }>`, utilisent `inject(HttpClient)` avec URL statique
- **SW** : Custom, pas de Workbox ni ngsw. Registration silencieuse dans `main.ts`
- **Pas d'IndexedDB** existant, pas de Cache API, pas de `navigator.onLine` — infrastructure offline 100% nouvelle

### Files to Reference

| File | Purpose | Modifier ? |
| ---- | ------- | ---------- |
| `apps/web/public/sw.js` | Service worker actuel (push only) | OUI — étendre |
| `apps/web/src/main.ts` | Registration du SW | NON |
| `apps/web/src/app/core/auth.interceptor.ts` | Interceptor HTTP auth + refresh | NON |
| `apps/web/src/app/core/auth.service.ts` | Token localStorage | NON |
| `apps/web/src/app/core/realtime-sync.service.ts` | SSE sync | NON |
| `apps/web/src/app/core/push-notification.service.ts` | Push enrollment | NON |
| `apps/web/src/app/core/notification-banner.component.ts` | Banner notif (modèle pour offline banner) | NON |
| `apps/web/src/app/core/badge.service.ts` | App badge API | NON |
| `apps/web/src/app/core/app-shell.component.ts` | Shell component | OUI — inject NetworkStatusService |
| `apps/web/src/app/core/app-shell.component.html` | Shell template | OUI — ajouter `<app-offline-banner />` |
| `apps/web/src/app/app.config.ts` | App providers | NON |
| `apps/web/src/styles.scss` | Design tokens et styles globaux | NON |

### Nouveaux fichiers à créer

| File | Purpose |
| ---- | ------- |
| `apps/web/src/app/core/network-status.service.ts` | Service Angular — signal `isOffline`, `pendingCount`, écoute `online`/`offline` events + SW messages |
| `apps/web/src/app/core/offline-banner.component.ts` | Composant standalone — banner visuel offline + compteur queue + indicateur sync |

### Technical Decisions

- **Zero touch sur le code existant** : tout passe par le SW (fetch intercept) et de nouveaux fichiers Angular
- **Pas de Workbox** : on étend le SW custom existant pour garder la cohérence
- **IndexedDB native** pour la queue (pas localStorage — taille illimitée, API async, accessible depuis le SW)
- **Cache API native** pour les réponses HTTP (accessible depuis le SW)
- **Stale-while-revalidate** pour les GET API : retourne le cache immédiatement, met à jour en background quand online
- **Cache-first** pour les assets statiques : servis depuis le cache, mis à jour en background
- **Pas de TTL** : le cache reste valide tant que le réseau est absent
- **postMessage** bidirectionnel entre SW et app pour : statut online/offline, compteur queue, replay terminé
- **Token auth pour le replay** : le SW demande un token frais au client via `postMessage` avant de rejouer la queue
- **Notifs locales** : `setTimeout` dans le SW basé sur les données `/api/v1/today` cachées, uniquement quand offline

### API Endpoints — Caching Strategy

**GET (stale-while-revalidate) :**
- `/api/v1/today` — vue aujourd'hui
- `/api/v1/today/daily?date=` — vue journalière
- `/api/v1/agenda/week?date=` — semaine
- `/api/v1/agenda/month?date=` — mois
- `/api/v1/stats/dashboard` — statistiques
- `/api/v1/goals` — objectifs
- `/api/v1/profile` — profil
- `/api/v1/notifications/center` — centre notifs
- `/api/v1/notifications/unviewed-count` — compteur
- `/api/v1/wake-up-override/*` — réveil
- `/api/v1/tasks/*` — tâches
- `/api/v1/exports/history` — historique exports

**POST/PUT/PATCH/DELETE (queue offline) :**
- Toutes les mutations `/api/v1/*` sauf auth et SSE

**Exclure du cache/queue :**
- `/api/v1/auth/*` (login, refresh, logout)
- `/api/v1/sync/events` (SSE, non cacheable)
- `/api/v1/exports/download` (blob, trop gros)

## Implementation Plan

### Tasks

- [ ] **Task 1 : SW — Cache statique app shell**
  - File : `apps/web/public/sw.js`
  - Action : Ajouter un listener `fetch` avec stratégie **cache-first** pour les assets statiques (navigation requests → `index.html` ; JS/CSS/SVG/PNG/ICO/webmanifest → cache-first). Au premier fetch d'un asset, le mettre en cache (`pht-static-v1`). Les requêtes de navigation (mode `navigate`) retournent toujours `index.html` depuis le cache (SPA fallback). Nettoyage de l'ancien cache au `activate`.
  - Notes : Pas de liste statique de fichiers — les hashes Angular changent à chaque build. On cache dynamiquement au premier accès. Le nom de cache est versionné (`pht-static-v1`) pour pouvoir purger plus tard.

- [ ] **Task 2 : SW — Cache API GET stale-while-revalidate**
  - File : `apps/web/public/sw.js`
  - Action : Dans le listener `fetch`, intercepter les GET vers `/api/v1/*` (sauf `/api/v1/auth/*`, `/api/v1/sync/*`, `/api/v1/exports/download`). Stratégie stale-while-revalidate : retourner le cache (`pht-api`) immédiatement s'il existe, puis lancer un fetch réseau en background pour mettre à jour le cache. Si pas de cache et pas de réseau → retourner une `Response` 503 JSON `{ error: { message: "Offline" } }`.
  - Notes : Le cache API est un seul store `pht-api`. La clé de cache est l'URL complète (incluant query params). On ne cache pas les réponses avec status >= 400.

- [ ] **Task 3 : SW — IndexedDB helpers pour la queue offline**
  - File : `apps/web/public/sw.js`
  - Action : Créer les fonctions helpers IndexedDB en haut du fichier : `openDb()` (ouvre/crée la DB `pht-offline` v1 avec un object store `mutation-queue` auto-increment), `enqueue(entry)` (ajoute une entrée `{ url, method, headers, body, timestamp }`), `dequeueAll()` (récupère toutes les entrées triées par clé, les supprime atomiquement, les retourne), `countPending()` (retourne le nombre d'entrées).
  - Notes : IndexedDB est accessible depuis le SW. Les helpers sont des fonctions pures async/await wrappant les événements IDB.

- [ ] **Task 4 : SW — Queue des mutations offline**
  - File : `apps/web/public/sw.js`
  - Action : Dans le listener `fetch`, intercepter les POST/PUT/PATCH/DELETE vers `/api/v1/*` (sauf `/api/v1/auth/*`). Si le fetch réseau échoue (TypeError = offline, ou status 502/503/504 = serveur KO) : sérialiser la requête (`url`, `method`, `headers` filtrés, `body` en texte) et l'enqueue dans IndexedDB. Retourner une `Response` 202 JSON `{ data: null, _offlineQueued: true }` pour que l'app Angular ne crash pas. Poster un message `{ type: 'QUEUE_UPDATED', pendingCount }` aux clients.
  - Notes : Le header `Authorization` est conservé dans l'entrée pour le replay. On filtre les headers techniques (accept-encoding, etc.). Le body est lu une seule fois via `request.clone().text()`.

- [ ] **Task 5 : SW — Replay de la queue au retour online**
  - File : `apps/web/public/sw.js`
  - Action : Écouter l'événement `message` de type `REPLAY_QUEUE` (envoyé par le client Angular quand le réseau revient). Récupérer toutes les entrées via `dequeueAll()`. Pour chaque entrée en FIFO : refaire le `fetch(entry.url, { method, headers, body })`. Si une requête échoue (pas 2xx), la remettre en queue (pas de retry infini : max 3 tentatives via un champ `attempts`). À la fin, poster `{ type: 'QUEUE_REPLAYED', succeeded, failed }` et `{ type: 'QUEUE_UPDATED', pendingCount }` aux clients. Invalider le cache `pht-api` après replay réussi pour forcer un rafraîchissement des données.
  - Notes : Le replay utilise le token d'auth contenu dans chaque entrée. Si le token a expiré (401), le SW poste `{ type: 'TOKEN_NEEDED' }` au client, attend un `{ type: 'TOKEN_REFRESH', token }` en réponse, et met à jour le header Authorization de toutes les entrées restantes avant de continuer.

- [ ] **Task 6 : SW — Notifications locales ON_TIME quand offline**
  - File : `apps/web/public/sw.js`
  - Action : Ajouter un listener `message` de type `SCHEDULE_LOCAL_NOTIFS`. Le client Angular envoie ce message avec la liste des occurrences `planned` et leur `occurrenceTime` (ISO string) quand il détecte le passage offline. Le SW calcule le délai en ms entre maintenant et chaque `occurrenceTime` future, et planifie un `setTimeout` qui appelle `self.registration.showNotification(title, options)`. Stocker les timer IDs dans une Map pour pouvoir les annuler via un message `CANCEL_LOCAL_NOTIFS` (envoyé quand le réseau revient et que les push serveur reprennent).
  - Notes : `setTimeout` dans un SW est fiable tant que le SW reste actif. Pour les délais > 5 min, utiliser `self.registration.showNotification` déclenché par un `setInterval` de heartbeat qui check les échéances stockées en mémoire (le SW peut être tué et relancé par le browser). Stocker les échéances dans IndexedDB store `local-notifs` pour survivre au restart du SW.

- [ ] **Task 7 : Angular — NetworkStatusService**
  - File : `apps/web/src/app/core/network-status.service.ts` (NOUVEAU)
  - Action : Créer un service Angular `providedIn: 'root'` avec :
    - `readonly isOffline = signal(false)` — basé sur `navigator.onLine` + confirmé par échec/succès des fetch
    - `readonly pendingCount = signal(0)` — nombre de mutations en queue
    - `readonly syncing = signal(false)` — true pendant le replay
    - `start()` — appelé une seule fois dans AppShellComponent.ngOnInit :
      - Écoute `window: online/offline` events → met à jour `isOffline`
      - Écoute `navigator.serviceWorker.controller` messages (`QUEUE_UPDATED`, `QUEUE_REPLAYED`)
      - Sur passage online : envoie `REPLAY_QUEUE` au SW, envoie `CANCEL_LOCAL_NOTIFS` au SW
      - Sur passage offline : lit les données today depuis le cache via un fetch (le SW retournera le cache), envoie `SCHEDULE_LOCAL_NOTIFS` au SW avec les occurrences planned
      - Écoute `TOKEN_NEEDED` du SW : lit le token depuis `AuthService.getAccessToken()`, poste `TOKEN_REFRESH` au SW
    - `stop()` — cleanup des listeners
  - Notes : Utilise `inject(AuthService)` pour le token. Utilise `inject(NgZone)` pour `ngZone.run()` sur les callbacks hors zone Angular. Pattern identique à `RealtimeSyncService`.

- [ ] **Task 8 : Angular — OfflineBannerComponent**
  - File : `apps/web/src/app/core/offline-banner.component.ts` (NOUVEAU)
  - Action : Créer un composant standalone inline (template + styles dans le `.ts`, pattern identique à `NotificationBannerComponent`) :
    - Injecte `NetworkStatusService`
    - Template : `@if (networkStatus.isOffline())` → banner warning "Mode hors-ligne" avec icône wifi-off. `@if (networkStatus.pendingCount() > 0)` → affiche "N actions en attente". `@if (networkStatus.syncing())` → affiche "Synchronisation...".
    - Styles : `.offline-banner` avec `background: var(--warning-bg)`, `color: var(--warning)`, `border-bottom: 1px solid color-mix(in srgb, var(--warning) 25%, transparent)`. Padding `var(--sp-2) var(--sp-4)`. Icône inline SVG 16px. Transition `max-height var(--dur) var(--ease)` pour apparition/disparition smooth. Dark mode via `[data-theme="dark"]` ou media query.
    - Sélecteur : `app-offline-banner`
  - Notes : Pattern copié 1:1 de `notification-banner.component.ts` pour la structure. Pas de bouton d'action (le retry est automatique).

- [ ] **Task 9 : Intégration shell — Brancher le tout**
  - File : `apps/web/src/app/core/app-shell.component.ts`
  - Action : Ajouter `private readonly networkStatus = inject(NetworkStatusService);` aux champs. Ajouter `this.networkStatus.start();` dans `ngOnInit()`. Ajouter `this.networkStatus.stop();` dans `ngOnDestroy()`. Ajouter `OfflineBannerComponent` dans le tableau `imports` du composant.
  - File : `apps/web/src/app/core/app-shell.component.html`
  - Action : Ajouter `<app-offline-banner />` juste après `<app-notification-banner />` (ligne ~105 du template actuel).
  - Notes : Ce sont les 2 seules modifications de fichiers existants. Le reste est du nouveau code.

- [ ] **Task 10 : SW — Nettoyage de cache à la mise à jour**
  - File : `apps/web/public/sw.js`
  - Action : Dans le listener `activate`, nettoyer les anciens caches (tout cache dont le nom commence par `pht-` mais qui n'est pas dans la liste des caches courants `['pht-static-v1', 'pht-api']`). Cela permet de purger proprement quand on incrémente la version du cache.
  - Notes : Pattern standard de nettoyage de cache SW.

### Acceptance Criteria

**Cache statique :**
- [ ] AC 1 : Given l'app a été visitée au moins une fois avec réseau, when le réseau tombe et l'utilisateur ouvre l'app, then l'app shell se charge (HTML + CSS + JS) et affiche la dernière page connue.
- [ ] AC 2 : Given l'app est en cache, when une nouvelle version est déployée et l'utilisateur revisite, then les nouveaux assets sont cachés en background et disponibles au prochain reload.
- [ ] AC 3 : Given une requête de navigation (deep link), when l'utilisateur est offline, then `index.html` est retourné depuis le cache (SPA routing).

**Cache API :**
- [ ] AC 4 : Given l'utilisateur a visité `/today` avec réseau, when il retourne sur `/today` offline, then les données apparaissent depuis le cache (contenu identique à la dernière visite online).
- [ ] AC 5 : Given l'utilisateur est online, when il visite `/agenda`, then la réponse est servie depuis le cache ET mise à jour en background (stale-while-revalidate).
- [ ] AC 6 : Given l'utilisateur n'a jamais visité `/stats` et est offline, when il navigue vers `/stats`, then une erreur 503 est retournée (pas de données en cache).
- [ ] AC 7 : Given une requête GET vers `/api/v1/auth/refresh`, when le SW intercepte, then la requête passe directement au réseau sans caching.

**Queue mutations :**
- [ ] AC 8 : Given l'utilisateur est offline, when il marque une occurrence DONE, then la mutation est stockée dans IndexedDB et une réponse 202 est retournée. L'UI ne crash pas.
- [ ] AC 9 : Given il y a 3 mutations en queue, when le réseau revient, then les 3 mutations sont rejouées en FIFO dans les 5 secondes.
- [ ] AC 10 : Given une mutation en queue échoue au replay (500), when le retry max (3) est atteint, then la mutation est abandonnée et le compteur est mis à jour.
- [ ] AC 11 : Given le token JWT a expiré pendant le offline, when le replay commence et reçoit un 401, then le SW demande un token frais au client Angular et continue le replay.

**Notifications locales :**
- [ ] AC 12 : Given l'utilisateur passe offline à 08:00 et a une tâche planifiée à 08:30, when 08:30 arrive, then une notification locale s'affiche avec le titre de la tâche.
- [ ] AC 13 : Given des notifs locales sont planifiées et le réseau revient, when le client envoie `CANCEL_LOCAL_NOTIFS`, then les timers sont annulés (pas de doublon avec les push serveur).

**Indicateur UI :**
- [ ] AC 14 : Given l'utilisateur perd le réseau, when l'app est ouverte, then un banner warning "Mode hors-ligne" apparaît en haut de l'écran en moins de 2 secondes.
- [ ] AC 15 : Given il y a 5 mutations en queue, when le banner est visible, then il affiche "5 actions en attente".
- [ ] AC 16 : Given le réseau revient et le replay commence, when le banner est visible, then il affiche "Synchronisation..." puis disparaît quand le replay est terminé.

**Zero régression :**
- [ ] AC 17 : Given l'utilisateur est online, when il utilise l'app normalement, then aucun comportement ne change (les requêtes passent au réseau, les réponses sont cachées en background silencieusement).
- [ ] AC 18 : Given une push notification arrive (online), when l'utilisateur clique DONE, then le flow existant fonctionne identiquement (le SW notificationclick handler n'est pas impacté).
- [ ] AC 19 : Given le auth interceptor reçoit un 401, when il tente un refresh, then le flow existant fonctionne identiquement (les requêtes auth sont exclues du cache et de la queue).

## Additional Context

### Dependencies

- Aucune dépendance externe ajoutée
- API natives : Cache API, IndexedDB, Notification API, setTimeout
- Le SW doit supporter les browsers PWA modernes (Chrome 80+, Safari 16.4+, Firefox 44+)

### Testing Strategy

**Tests manuels (priorité haute) :**
1. Ouvrir l'app en ligne → naviguer sur toutes les pages → couper le réseau (DevTools ou avion) → recharger l'app → vérifier que l'app shell charge et les données sont visibles
2. En mode offline, marquer une tâche DONE → vérifier que l'UI ne crash pas → remettre le réseau → vérifier que le statut est synchronisé côté serveur
3. Passer offline avec des tâches futures → vérifier que les notifs locales arrivent à l'heure
4. En mode online, vérifier que tout fonctionne exactement comme avant (zero régression)

**Tests unitaires :**
- `NetworkStatusService` : tester les réactions aux événements online/offline et aux messages SW (mock de `navigator.serviceWorker`)
- `OfflineBannerComponent` : tester l'affichage conditionnel du banner selon les signaux de `NetworkStatusService`

**Test DevTools :**
- Application > Cache Storage : vérifier la présence de `pht-static-v1` et `pht-api`
- Application > IndexedDB > `pht-offline` > `mutation-queue` : vérifier les entrées en queue
- Network > Offline toggle : simuler la perte de réseau

### Notes

**Risques :**
- Le SW peut être tué par le browser après 30s d'inactivité → les `setTimeout` pour les notifs locales doivent être sauvegardés en IndexedDB et recheckés au réveil du SW via un heartbeat
- Safari a des limitations sur la durée de vie du cache SW (~7 jours sans visite) → l'utilisateur doit ouvrir l'app au moins une fois par semaine pour maintenir le cache
- Les réponses API volumineuses (stats avec beaucoup d'historique) peuvent remplir le quota de stockage → prévoir un fallback gracieux si `cache.put()` échoue (QuotaExceededError)

**Futur (hors scope) :**
- Background Sync API (`sync` event) quand le support sera universel
- Periodic Background Sync pour rafraîchir le cache proactivement
- Notifications séquentielles offline (5min, 15min rappels)
- Indicateur de "dernière synchronisation" avec timestamp
