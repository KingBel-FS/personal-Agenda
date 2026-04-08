import { Component, inject, OnInit, signal } from '@angular/core';
import { PushNotificationService, type PushPermissionState } from './push-notification.service';

@Component({
  selector: 'app-notification-banner',
  template: `
    @if (visible()) {
      <div class="notif-banner notif-banner--{{ bannerType() }}" role="alert">
        <p class="notif-banner__text">{{ bannerMessage() }}</p>
        <div class="notif-banner__actions">
          <button type="button" class="notif-banner__btn" (click)="onAction()" [disabled]="loading()">
            {{ loading() ? 'Activation...' : actionLabel() }}
          </button>
          <button type="button" class="notif-banner__close" (click)="dismiss()" aria-label="Fermer">&times;</button>
        </div>
      </div>
    }
  `,
  styles: [`
    .notif-banner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      border-radius: 0.5rem;
      margin: 0.5rem 1rem;
      font-size: 0.875rem;
      line-height: 1.4;
    }
    .notif-banner--info {
      background: var(--info-bg, #e0f0ff);
      color: var(--info);
      border: 1px solid color-mix(in srgb, var(--info) 24%, transparent);
    }
    .notif-banner--warning {
      background: var(--warning-bg, #fff8e1);
      color: var(--warning);
      border: 1px solid color-mix(in srgb, var(--warning) 24%, transparent);
    }
    .notif-banner--error {
      background: var(--danger-bg, #fde8e8);
      color: var(--danger);
      border: 1px solid color-mix(in srgb, var(--danger) 24%, transparent);
    }
    .notif-banner__text {
      margin: 0;
      flex: 1;
    }
    .notif-banner__actions {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-shrink: 0;
    }
    .notif-banner__btn {
      padding: 0.375rem 0.75rem;
      border: none;
      border-radius: 0.375rem;
      font-size: 0.8125rem;
      font-weight: 600;
      cursor: pointer;
      background: var(--brand, #4f46e5);
      color: #fff;
    }
    .notif-banner__btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .notif-banner__close {
      background: none;
      border: none;
      font-size: 1.25rem;
      cursor: pointer;
      color: inherit;
      opacity: 0.6;
      line-height: 1;
    }
    .notif-banner__close:hover { opacity: 1; }
  `]
})
export class NotificationBannerComponent implements OnInit {
  private readonly pushService = inject(PushNotificationService);

  protected readonly visible = signal(false);
  protected readonly loading = signal(false);
  protected readonly bannerType = signal<'info' | 'warning' | 'error'>('info');
  protected readonly bannerMessage = signal('');
  protected readonly actionLabel = signal('');

  private dismissed = false;

  ngOnInit(): void {
    this.pushService.checkStatus().then(() => this.evaluate());
  }

  protected onAction(): void {
    const state = this.pushService.permissionState();
    if (state === 'denied') {
      // Can't request again — user must go to browser settings
      return;
    }
    this.loading.set(true);
    this.pushService.enroll().then((success) => {
      this.loading.set(false);
      if (success) {
        this.visible.set(false);
      } else {
        this.evaluate();
      }
    });
  }

  protected dismiss(): void {
    this.dismissed = true;
    this.visible.set(false);
  }

  private evaluate(): void {
    if (this.dismissed) return;

    const state: PushPermissionState = this.pushService.permissionState();
    const subscribed = this.pushService.subscribed();

    if (state === 'unsupported') {
      this.visible.set(false);
      return;
    }

    if (state === 'denied') {
      this.bannerType.set('warning');
      this.bannerMessage.set('Les notifications sont désactivées dans ton navigateur.');
      this.actionLabel.set('Voir les paramètres');
      this.visible.set(true);
      return;
    }

    if (state === 'default' || !subscribed) {
      this.bannerType.set('info');
      this.bannerMessage.set('Autorise les notifications pour recevoir tes rappels de tâches.');
      this.actionLabel.set('Autoriser');
      this.visible.set(true);
      return;
    }

    this.visible.set(false);
  }
}
