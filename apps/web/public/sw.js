// Service worker — PWA badge + Web Push notifications
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (e) => e.waitUntil(self.clients.claim()));

// ── Push event: show native notification ──────────────
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

// ── Notification click: handle action or open app ─────
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
      } catch (e) {
        // Silently fail — user can still use the app
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
