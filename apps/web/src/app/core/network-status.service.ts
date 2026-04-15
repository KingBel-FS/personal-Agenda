import { Injectable, NgZone, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class NetworkStatusService {
  private readonly authService = inject(AuthService);
  private readonly ngZone = inject(NgZone);

  readonly isOffline = signal(!navigator.onLine);
  readonly pendingCount = signal(0);
  readonly syncing = signal(false);

  private started = false;
  private onOnline = () => this.ngZone.run(() => this.handleOnline());
  private onOffline = () => this.ngZone.run(() => this.handleOffline());
  private onSwMessage = (event: MessageEvent) => this.ngZone.run(() => this.handleSwMessage(event));

  start(): void {
    if (this.started) return;
    this.started = true;

    window.addEventListener('online', this.onOnline);
    window.addEventListener('offline', this.onOffline);
    navigator.serviceWorker?.addEventListener('message', this.onSwMessage);

    // Ask SW for current queue count
    this.postToSw({ type: 'GET_PENDING_COUNT' });
  }

  stop(): void {
    this.started = false;
    window.removeEventListener('online', this.onOnline);
    window.removeEventListener('offline', this.onOffline);
    navigator.serviceWorker?.removeEventListener('message', this.onSwMessage);
  }

  private handleOnline(): void {
    this.isOffline.set(false);

    // Cancel local notifs (push server will take over)
    this.postToSw({ type: 'CANCEL_LOCAL_NOTIFS' });

    // Replay queued mutations with fresh token
    const token = this.authService.getAccessToken();
    this.postToSw({ type: 'REPLAY_QUEUE', token });
  }

  private handleOffline(): void {
    this.isOffline.set(true);

    // Schedule local notifications from cached today data
    this.scheduleLocalNotifsFromCache();
  }

  private handleSwMessage(event: MessageEvent): void {
    const { type } = event.data || {};

    if (type === 'QUEUE_UPDATED') {
      this.pendingCount.set(event.data.pendingCount || 0);
    }

    if (type === 'SYNC_STARTED') {
      this.syncing.set(true);
    }

    if (type === 'QUEUE_REPLAYED') {
      this.syncing.set(false);
    }

    if (type === 'TOKEN_NEEDED') {
      const token = this.authService.getAccessToken();
      if (token) {
        this.postToSw({ type: 'REPLAY_QUEUE', token });
      }
    }
  }

  private async scheduleLocalNotifsFromCache(): Promise<void> {
    try {
      // Fetch today data — the SW will return it from cache
      const response = await fetch('/api/v1/today');
      if (!response.ok) return;
      const { data } = await response.json();
      if (!data?.occurrences) return;

      const now = new Date();
      const occurrences = data.occurrences
        .filter((o: any) => o.status === 'planned' && o.occurrenceTime)
        .map((o: any) => {
          // Build a full datetime from today's date + occurrence time
          const [hours, minutes] = o.occurrenceTime.split(':').map(Number);
          const dt = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hours, minutes);
          return {
            occurrenceId: o.id,
            taskName: o.taskName,
            body: o.taskName + " — C'est l'heure !",
            occurrenceDateTime: dt.toISOString()
          };
        })
        .filter((o: any) => new Date(o.occurrenceDateTime).getTime() > now.getTime());

      if (occurrences.length > 0) {
        this.postToSw({ type: 'SCHEDULE_LOCAL_NOTIFS', occurrences });
      }
    } catch {
      // Cache miss or parse error — silently ignore
    }
  }

  private postToSw(msg: Record<string, unknown>): void {
    navigator.serviceWorker?.controller?.postMessage(msg);
  }
}
