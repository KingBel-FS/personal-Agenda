import { Injectable, NgZone, inject } from '@angular/core';
import { Subject } from 'rxjs';
import { AuthService } from './auth.service';

export interface RealtimeSyncEvent {
  scope: string;
  occurredAt: string;
}

@Injectable({ providedIn: 'root' })
export class RealtimeSyncService {
  private readonly authService = inject(AuthService);
  private readonly ngZone = inject(NgZone);

  private readonly eventsSubject = new Subject<RealtimeSyncEvent>();
  readonly events$ = this.eventsSubject.asObservable();

  private eventSource: EventSource | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private started = false;

  start(): void {
    if (this.started) {
      return;
    }
    this.started = true;
    this.connect();
  }

  stop(): void {
    this.started = false;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  private connect(): void {
    if (!this.started || this.eventSource || typeof EventSource === 'undefined') {
      return;
    }

    const token = this.authService.getAccessToken();
    if (!token) {
      return;
    }

    const url = `/api/v1/sync/events?access_token=${encodeURIComponent(token)}`;
    this.eventSource = new EventSource(url);
    this.eventSource.addEventListener('sync', (event) => {
      const message = JSON.parse((event as MessageEvent<string>).data) as RealtimeSyncEvent;
      this.ngZone.run(() => this.eventsSubject.next(message));
    });
    this.eventSource.onerror = () => {
      this.eventSource?.close();
      this.eventSource = null;
      if (!this.started) {
        return;
      }
      this.reconnectTimer = setTimeout(() => {
        this.reconnectTimer = null;
        this.connect();
      }, 2000);
    };
  }
}
