// Service worker — PWA offline + Web Push notifications
// Cache names (versioned for clean upgrades)
const STATIC_CACHE = 'pht-static-v1';
const API_CACHE = 'pht-api';
const OFFLINE_DB = 'pht-offline';
const MUTATION_STORE = 'mutation-queue';
const NOTIF_STORE = 'local-notifs';

// ── IndexedDB helpers ────────────────────────────────────
function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(OFFLINE_DB, 1);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(MUTATION_STORE)) {
        db.createObjectStore(MUTATION_STORE, { autoIncrement: true });
      }
      if (!db.objectStoreNames.contains(NOTIF_STORE)) {
        db.createObjectStore(NOTIF_STORE, { keyPath: 'occurrenceId' });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function enqueue(entry) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(MUTATION_STORE, 'readwrite');
    tx.objectStore(MUTATION_STORE).add(entry);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

async function dequeueAll() {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(MUTATION_STORE, 'readwrite');
    const store = tx.objectStore(MUTATION_STORE);
    const entries = [];
    const cursorReq = store.openCursor();
    cursorReq.onsuccess = () => {
      const cursor = cursorReq.result;
      if (cursor) {
        entries.push({ key: cursor.key, value: cursor.value });
        cursor.delete();
        cursor.continue();
      }
    };
    tx.oncomplete = () => resolve(entries.map((e) => e.value));
    tx.onerror = () => reject(tx.error);
  });
}

async function countPending() {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(MUTATION_STORE, 'readonly');
    const req = tx.objectStore(MUTATION_STORE).count();
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

// ── Notify all clients ───────────────────────────────────
async function postToClients(msg) {
  const clients = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
  for (const client of clients) {
    client.postMessage(msg);
  }
}

// ── Install: precache index.html, activate immediately ───
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) => cache.add('/index.html'))
  );
  self.skipWaiting();
});

// ── Activate: clean old caches, claim clients ────────────
self.addEventListener('activate', (event) => {
  const currentCaches = [STATIC_CACHE, API_CACHE];
  event.waitUntil(
    caches.keys().then((names) =>
      Promise.all(
        names
          .filter((name) => name.startsWith('pht-') && !currentCaches.includes(name))
          .map((name) => caches.delete(name))
      )
    ).then(() => self.clients.claim())
  );
});

// ── Fetch: offline strategies ────────────────────────────
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Only handle same-origin requests
  if (url.origin !== self.location.origin) return;

  // Skip: SSE, auth endpoints, export downloads
  if (url.pathname.startsWith('/api/v1/auth/')) return;
  if (url.pathname.startsWith('/api/v1/sync/')) return;
  if (url.pathname === '/api/v1/exports/download') return;

  // API requests
  if (url.pathname.startsWith('/api/v1/')) {
    if (request.method === 'GET') {
      event.respondWith(handleApiGet(request));
    } else {
      event.respondWith(handleApiMutation(request));
    }
    return;
  }

  // Navigation requests → SPA fallback to cached index.html
  if (request.mode === 'navigate') {
    event.respondWith(handleNavigation(request));
    return;
  }

  // Static assets (JS, CSS, images, fonts, manifest)
  if (isStaticAsset(url.pathname)) {
    event.respondWith(handleStaticAsset(request));
    return;
  }
});

function isStaticAsset(pathname) {
  return /\.(js|css|svg|png|ico|jpg|jpeg|webp|woff2?|ttf|eot|webmanifest|json)$/i.test(pathname);
}

// ── Strategy: Navigation → network-first, fallback to cached index.html
async function handleNavigation(request) {
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(STATIC_CACHE);
      cache.put('/index.html', response.clone());
    }
    return response;
  } catch {
    const cached = await caches.match('/index.html');
    return cached || new Response('Offline', { status: 503 });
  }
}

// ── Strategy: Static assets → cache-first, update in background
async function handleStaticAsset(request) {
  const cached = await caches.match(request);
  if (cached) {
    // Update cache in background (don't await)
    fetch(request).then((response) => {
      if (response.ok) {
        caches.open(STATIC_CACHE).then((cache) => cache.put(request, response));
      }
    }).catch(() => {});
    return cached;
  }
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(STATIC_CACHE);
      cache.put(request, response.clone());
    }
    return response;
  } catch {
    return new Response('Offline', { status: 503 });
  }
}

// ── Strategy: API GET → stale-while-revalidate
async function handleApiGet(request) {
  const cache = await caches.open(API_CACHE);
  const cached = await cache.match(request);

  const fetchAndUpdate = fetch(request).then((response) => {
    if (response.ok) {
      cache.put(request, response.clone());
    }
    return response;
  }).catch((err) => {
    if (cached) return null; // We already returned cache, swallow error
    throw err;
  });

  if (cached) {
    // Return stale immediately, revalidate in background
    fetchAndUpdate.catch(() => {});
    return cached;
  }

  // No cache — must wait for network
  try {
    const response = await fetchAndUpdate;
    if (response) return response;
  } catch {
    // No cache, no network
  }
  return new Response(
    JSON.stringify({ error: { message: 'Offline' } }),
    { status: 503, headers: { 'Content-Type': 'application/json' } }
  );
}

// ── Strategy: API mutation → try network, queue on failure
async function handleApiMutation(request) {
  const cloned = request.clone();

  try {
    const response = await fetch(request);
    // Server errors that suggest temporary outage → queue
    if (response.status === 502 || response.status === 503 || response.status === 504) {
      await queueMutation(cloned);
      return new Response(
        JSON.stringify({ data: null, _offlineQueued: true }),
        { status: 202, headers: { 'Content-Type': 'application/json' } }
      );
    }
    return response;
  } catch {
    // Network error (offline)
    await queueMutation(cloned);
    return new Response(
      JSON.stringify({ data: null, _offlineQueued: true }),
      { status: 202, headers: { 'Content-Type': 'application/json' } }
    );
  }
}

async function queueMutation(request) {
  const headers = {};
  for (const [key, value] of request.headers.entries()) {
    // Keep auth and content-type, skip browser-internal headers
    if (['authorization', 'content-type', 'accept'].includes(key.toLowerCase())) {
      headers[key] = value;
    }
  }
  const body = await request.text();
  await enqueue({
    url: request.url,
    method: request.method,
    headers,
    body: body || null,
    timestamp: Date.now(),
    attempts: 0
  });
  const pending = await countPending();
  await postToClients({ type: 'QUEUE_UPDATED', pendingCount: pending });
}

// ── Message handlers ─────────────────────────────────────
self.addEventListener('message', (event) => {
  const { type } = event.data || {};

  if (type === 'REPLAY_QUEUE') {
    event.waitUntil(replayQueue(event.data.token));
  }

  if (type === 'SCHEDULE_LOCAL_NOTIFS') {
    scheduleLocalNotifs(event.data.occurrences || []);
  }

  if (type === 'CANCEL_LOCAL_NOTIFS') {
    cancelLocalNotifs();
  }

  if (type === 'GET_PENDING_COUNT') {
    countPending().then((pendingCount) => {
      postToClients({ type: 'QUEUE_UPDATED', pendingCount });
    });
  }
});

// ── Queue replay ─────────────────────────────────────────
async function replayQueue(token) {
  const entries = await dequeueAll();
  if (entries.length === 0) {
    await postToClients({ type: 'QUEUE_REPLAYED', succeeded: 0, failed: 0 });
    return;
  }

  await postToClients({ type: 'SYNC_STARTED' });

  let succeeded = 0;
  let failed = 0;
  const requeue = [];

  for (const entry of entries) {
    // Use fresh token if provided
    if (token && entry.headers['authorization']) {
      entry.headers['authorization'] = 'Bearer ' + token;
    }
    if (token && entry.headers['Authorization']) {
      entry.headers['Authorization'] = 'Bearer ' + token;
    }

    try {
      const response = await fetch(entry.url, {
        method: entry.method,
        headers: entry.headers,
        body: entry.body
      });

      if (response.status === 401 && token) {
        // Token expired during replay — ask client for fresh one
        await postToClients({ type: 'TOKEN_NEEDED' });
        // Re-queue this and remaining entries
        requeue.push(entry);
        continue;
      }

      if (response.ok || response.status === 409) {
        // 409 Conflict = already processed (idempotent), count as success
        succeeded++;
      } else {
        entry.attempts = (entry.attempts || 0) + 1;
        if (entry.attempts < 3) {
          requeue.push(entry);
        } else {
          failed++;
        }
      }
    } catch {
      // Network error during replay — re-queue everything remaining
      requeue.push(entry);
      break;
    }
  }

  // Re-queue failed entries
  for (const entry of requeue) {
    await enqueue(entry);
  }

  // Invalidate API cache after successful replay to force fresh data
  if (succeeded > 0) {
    await caches.delete(API_CACHE);
  }

  const pending = await countPending();
  await postToClients({ type: 'QUEUE_REPLAYED', succeeded, failed });
  await postToClients({ type: 'QUEUE_UPDATED', pendingCount: pending });
}

// ── Local notifications (offline) ────────────────────────
const localNotifTimers = new Map();

function scheduleLocalNotifs(occurrences) {
  cancelLocalNotifs();
  const now = Date.now();

  for (const occ of occurrences) {
    const targetTime = new Date(occ.occurrenceDateTime).getTime();
    const delay = targetTime - now;
    if (delay <= 0) continue; // Already past

    const timerId = setTimeout(() => {
      self.registration.showNotification(occ.taskName || 'Habit Tracker', {
        body: occ.body || "C'est l'heure !",
        icon: '/icons/icon-192x192.png',
        badge: '/icons/icon-72x72.png',
        tag: 'local-' + occ.occurrenceId,
        data: {
          notificationJobId: null,
          taskOccurrenceId: occ.occurrenceId,
          deepLink: '/today'
        },
        actions: [
          { action: 'DONE', title: 'Fait' },
          { action: 'MISSED', title: 'Raté' }
        ]
      });
      localNotifTimers.delete(occ.occurrenceId);
    }, delay);

    localNotifTimers.set(occ.occurrenceId, timerId);
  }
}

function cancelLocalNotifs() {
  for (const [, timerId] of localNotifTimers) {
    clearTimeout(timerId);
  }
  localNotifTimers.clear();
}

// ── Push event: show native notification ─────────────────
self.addEventListener('push', (event) => {
  const data = event.data ? event.data.json() : {};
  const options = {
    body: data.body || '',
    icon: data.iconUrl || '/icons/icon-192x192.png',
    badge: data.badgeUrl || '/icons/icon-72x72.png',
    image: data.imageUrl || undefined,
    tag: data.notificationJobId || undefined,
    requireInteraction: data.requireInteraction || false,
    data: {
      notificationJobId: data.notificationJobId,
      taskOccurrenceId: data.taskOccurrenceId,
      deepLink: data.actionUrl || '/'
    },
    actions: data.actions || []
  };

  event.waitUntil(
    self.registration.showNotification(data.title || 'Habit Tracker', options)
  );
});

// ── Notification click: handle action or open app ────────
self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  const notifData = event.notification.data || {};
  const action = event.action; // 'DONE', 'MISSED', or '' (body click)

  event.waitUntil((async () => {
    // If user clicked an action button (DONE / MISSED), call backend
    if (action === 'DONE' || action === 'MISSED') {
      try {
        await fetch('/api/v1/notifications/actions', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            notificationJobId: notifData.notificationJobId,
            action: action,
            taskOccurrenceId: notifData.taskOccurrenceId
          })
        });
      } catch {
        // Offline — queue the action
        await queueMutation(new Request('/api/v1/notifications/actions', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            notificationJobId: notifData.notificationJobId,
            action: action,
            taskOccurrenceId: notifData.taskOccurrenceId
          })
        }));
      }
    }

    // Build deep link with feedback query params
    let deepLink = notifData.deepLink || '/';
    if (action === 'DONE' || action === 'MISSED') {
      const sep = deepLink.includes('?') ? '&' : '?';
      deepLink += sep + 'pushAction=' + action + '&occurrenceId=' + notifData.taskOccurrenceId;
    }

    // Focus existing window or open new one
    const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    for (const win of windows) {
      if (win.url.includes(self.location.origin)) {
        win.focus();
        if (win.navigate) win.navigate(deepLink);
        return;
      }
    }
    return self.clients.openWindow(deepLink);
  })());
});
