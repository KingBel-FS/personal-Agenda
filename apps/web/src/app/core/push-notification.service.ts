import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type PushPermissionState = 'default' | 'granted' | 'denied' | 'unsupported';

@Injectable({ providedIn: 'root' })
export class PushNotificationService {
  private readonly http = inject(HttpClient);

  /** Current permission state — reactive signal for the banner */
  readonly permissionState = signal<PushPermissionState>(this.detectPermission());

  /** Whether a valid subscription exists on the server */
  readonly subscribed = signal(false);

  private vapidPublicKey: string | null = null;

  // ── Public API ──────────────────────────────────────

  /** Attempt full enrollment: permission → subscribe → register on server */
  async enroll(): Promise<boolean> {
    if (!this.isSupported()) {
      this.permissionState.set('unsupported');
      return false;
    }

    // Request permission
    const permission = await Notification.requestPermission();
    this.permissionState.set(permission as PushPermissionState);
    if (permission !== 'granted') return false;

    try {
      // Get VAPID key from server
      if (!this.vapidPublicKey) {
        const resp = await firstValueFrom(
          this.http.get<{ data: { publicKey: string } }>('/api/v1/notifications/vapid-key')
        );
        this.vapidPublicKey = resp.data.publicKey;
      }

      // Subscribe via PushManager
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: this.urlBase64ToUint8Array(this.vapidPublicKey).buffer as ArrayBuffer
      });

      // Send subscription to server
      const subJson = subscription.toJSON();
      await firstValueFrom(
        this.http.post('/api/v1/notifications/subscribe', {
          subscription: {
            endpoint: subJson.endpoint,
            keys: {
              auth: subJson.keys!['auth'],
              p256dh: subJson.keys!['p256dh']
            }
          }
        })
      );

      this.subscribed.set(true);
      return true;
    } catch {
      return false;
    }
  }

  /** Unsubscribe from push and revoke on server */
  async unenroll(): Promise<void> {
    try {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      if (subscription) {
        await firstValueFrom(
          this.http.post('/api/v1/notifications/unsubscribe', {
            endpoint: subscription.endpoint
          })
        );
        await subscription.unsubscribe();
      }
      this.subscribed.set(false);
    } catch {
      // Best-effort
    }
  }

  /** Check current subscription status (call on app init) */
  async checkStatus(): Promise<void> {
    this.permissionState.set(this.detectPermission());

    if (!this.isSupported() || Notification.permission !== 'granted') {
      this.subscribed.set(false);
      return;
    }

    try {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      this.subscribed.set(subscription !== null);
    } catch {
      this.subscribed.set(false);
    }
  }

  // ── Helpers ─────────────────────────────────────────

  private isSupported(): boolean {
    return 'Notification' in window && 'serviceWorker' in navigator && 'PushManager' in window;
  }

  private detectPermission(): PushPermissionState {
    if (!this.isSupported()) return 'unsupported';
    return Notification.permission as PushPermissionState;
  }

  private urlBase64ToUint8Array(base64String: string): Uint8Array {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = atob(base64);
    const outputArray = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; i++) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }
}
