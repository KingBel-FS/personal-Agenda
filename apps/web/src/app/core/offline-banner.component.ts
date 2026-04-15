import { Component, inject } from '@angular/core';
import { NetworkStatusService } from './network-status.service';

@Component({
  selector: 'app-offline-banner',
  template: `
    @if (networkStatus.isOffline() || networkStatus.syncing()) {
      <div class="offline-banner" role="status" [class.offline-banner--syncing]="networkStatus.syncing()">
        <svg class="offline-banner__icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
             fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
             aria-hidden="true" focusable="false">
          @if (networkStatus.syncing()) {
            <polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          } @else {
            <line x1="1" y1="1" x2="23" y2="23"/>
            <path d="M16.72 11.06A10.94 10.94 0 0 1 19 12.55"/>
            <path d="M5 12.55a10.94 10.94 0 0 1 5.17-2.39"/>
            <path d="M10.71 5.05A16 16 0 0 1 22.56 9"/>
            <path d="M1.42 9a15.91 15.91 0 0 1 4.7-2.88"/>
            <path d="M8.53 16.11a6 6 0 0 1 6.95 0"/>
            <line x1="12" y1="20" x2="12.01" y2="20"/>
          }
        </svg>
        <p class="offline-banner__text">
          @if (networkStatus.syncing()) {
            Synchronisation en cours...
          } @else {
            Mode hors-ligne
            @if (networkStatus.pendingCount() > 0) {
              <span class="offline-banner__count">&middot; {{ networkStatus.pendingCount() }} action{{ networkStatus.pendingCount() > 1 ? 's' : '' }} en attente</span>
            }
          }
        </p>
      </div>
    }
  `,
  styles: [`
    .offline-banner {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.5rem 1rem;
      margin: 0.5rem 1rem 0;
      border-radius: 0.5rem;
      font-size: 0.8125rem;
      line-height: 1.4;
      background: var(--warning-bg, #fffbeb);
      color: var(--warning, #d97706);
      border: 1px solid color-mix(in srgb, var(--warning, #d97706) 24%, transparent);
      animation: offlineBannerIn var(--dur, 160ms) var(--ease, ease-out);
    }
    .offline-banner--syncing {
      background: var(--info-bg, #f0f9ff);
      color: var(--info, #0284c7);
      border-color: color-mix(in srgb, var(--info, #0284c7) 24%, transparent);
    }
    .offline-banner__icon {
      flex-shrink: 0;
    }
    .offline-banner--syncing .offline-banner__icon {
      animation: offlineSpin 1s linear infinite;
    }
    .offline-banner__text {
      margin: 0;
      font-weight: 500;
    }
    .offline-banner__count {
      font-weight: 400;
      opacity: 0.85;
    }
    @keyframes offlineBannerIn {
      from { opacity: 0; transform: translateY(-4px); }
      to { opacity: 1; transform: translateY(0); }
    }
    @keyframes offlineSpin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
  `]
})
export class OfflineBannerComponent {
  protected readonly networkStatus = inject(NetworkStatusService);
}
