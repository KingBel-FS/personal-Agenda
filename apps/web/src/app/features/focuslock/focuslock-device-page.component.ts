import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FocuslockApiService, FlDevice, FlPairingToken } from './focuslock-api.service';

@Component({
  selector: 'app-focuslock-device',
  imports: [RouterLink, FormsModule],
  template: `
    <section class="fl-page">
      <header class="fl-hero">
        <div class="fl-hero__copy">
          <p class="fl-eyebrow">FocusLock</p>
          <h1>État iPhone</h1>
          <p class="fl-lead">Relie ton iPhone, vérifie les autorisations Apple et surveille la synchronisation.</p>
        </div>
      </header>

      @if (successMessage()) {
        <p class="fl-success" role="status">{{ successMessage() }}</p>
      }
      @if (errorMessage()) {
        <p class="fl-error" role="alert">{{ errorMessage() }}</p>
      }

      @if (loading()) {
        <div class="fl-placeholder-bar fl-placeholder-bar--wide"></div>
        <div class="fl-placeholder-bar" style="margin-top:0.5rem"></div>
      } @else {
        <!-- Appareils actifs -->
        @for (device of activeDevices(); track device.id) {
          <div class="fl-device-card fl-device-card--active">
            <div class="fl-device-card__icon fl-device-card__icon--ok">
              <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
            </div>
            <div class="fl-device-card__body">
              <p class="fl-device-card__title">{{ device.deviceName }}</p>
              <p class="fl-device-card__sub">
                Relié {{ device.pairedAt ? 'le ' + formatDate(device.pairedAt) : '' }}
                · Vu {{ device.lastSeenAt ? formatDate(device.lastSeenAt) : 'jamais' }}
              </p>
            </div>
            <button type="button" class="fl-btn fl-btn--danger" (click)="revokeDevice(device)" [disabled]="busy()">
              Révoquer
            </button>
          </div>

          <section class="fl-permissions" aria-label="Autorisations Apple">
            <h2 class="fl-section-title">Autorisations Apple</h2>
            <ul class="fl-permission-list">
              <li class="fl-permission-item">
                <div class="fl-permission-item__info">
                  <strong>Family Controls</strong>
                  <span>Requis pour appliquer les limites d'apps via Screen Time</span>
                </div>
                <div class="fl-permission-status" [attr.data-status]="device.familyControlsGranted ? 'granted' : 'pending'">
                  @if (device.familyControlsGranted) {
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
                    Accordée
                  } @else {
                    En attente
                  }
                </div>
              </li>
              <li class="fl-permission-item">
                <div class="fl-permission-item__info">
                  <strong>Screen Time</strong>
                  <span>Lecture du temps d'usage et imposition des budgets</span>
                </div>
                <div class="fl-permission-status" [attr.data-status]="device.screenTimeGranted ? 'granted' : 'pending'">
                  @if (device.screenTimeGranted) {
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
                    Accordée
                  } @else {
                    En attente
                  }
                </div>
              </li>
              <li class="fl-permission-item">
                <div class="fl-permission-item__info">
                  <strong>Notifications</strong>
                  <span>Alertes avant dépassement et début/fin de blocage</span>
                </div>
                <div class="fl-permission-status" [attr.data-status]="device.notificationsGranted ? 'granted' : 'pending'">
                  @if (device.notificationsGranted) {
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
                    Accordée
                  } @else {
                    En attente
                  }
                </div>
              </li>
            </ul>
          </section>
        }

        <!-- Aucun appareil actif → formulaire de liaison -->
        @if (activeDevices().length === 0) {
          <div class="fl-device-card">
            <div class="fl-device-card__icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
            </div>
            <div class="fl-device-card__body">
              <p class="fl-device-card__title">Aucun iPhone relié</p>
              <p class="fl-device-card__sub">Génère un code de liaison et saisis-le dans l'app iOS FocusLock.</p>
            </div>
            @if (!pairingToken()) {
              <button type="button" class="fl-btn fl-btn--primary" (click)="generateToken()" [disabled]="busy()">
                {{ busy() ? 'Génération...' : 'Générer un code' }}
              </button>
            }
          </div>

          @if (pairingToken(); as token) {
            <div class="fl-link-code" role="region" aria-label="Code de liaison">
              <p class="fl-link-code__label">Code à saisir dans l'app iOS FocusLock</p>
              <div class="fl-link-code__value" aria-live="polite">{{ token.token }}</div>
              <p class="fl-link-code__hint">Expire le {{ formatDate(token.expiresAt) }}</p>

              <!-- Confirmation manuelle (pour tests sans app iOS) -->
              <div class="fl-link-confirm">
                <p class="fl-label">Confirmer manuellement (dev/test)</p>
                <div class="fl-time-range">
                  <input class="fl-input" type="text" [(ngModel)]="confirmDeviceName" placeholder="Nom de l'appareil" name="devName">
                  <button type="button" class="fl-btn fl-btn--primary" (click)="confirmPairing(token.token)" [disabled]="busy()">
                    Confirmer
                  </button>
                </div>
              </div>
            </div>
          }
        }

        <!-- Appareils révoqués -->
        @if (revokedDevices().length > 0) {
          <section style="margin-top:2rem">
            <h2 class="fl-section-title">Appareils révoqués</h2>
            <ul class="fl-permission-list">
              @for (device of revokedDevices(); track device.id) {
                <li class="fl-permission-item">
                  <div class="fl-permission-item__info">
                    <strong>{{ device.deviceName }}</strong>
                    <span>Révoqué · relié le {{ device.pairedAt ? formatDate(device.pairedAt) : '—' }}</span>
                  </div>
                  <div class="fl-permission-status" data-status="denied">Révoqué</div>
                </li>
              }
            </ul>
          </section>
        }
      }

      <div class="fl-notice" role="note" style="margin-top:2rem">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <p>L'enforcement des restrictions (blocage réel des apps) nécessite l'app iOS native FocusLock. Cette PWA pilote la configuration — le blocage s'applique côté iPhone.</p>
      </div>

      <div class="fl-actions">
        <a routerLink="/focuslock" class="fl-btn">Retour au dashboard</a>
        <a routerLink="/focuslock/rules" class="fl-btn">Voir les règles</a>
      </div>
    </section>
  `
})
export class FocuslockDevicePageComponent implements OnInit {
  private readonly api = inject(FocuslockApiService);

  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly devices = signal<FlDevice[]>([]);
  protected readonly pairingToken = signal<FlPairingToken | null>(null);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected confirmDeviceName = '';

  ngOnInit(): void {
    this.api.listDevices().subscribe({
      next: ({ data }) => {
        this.devices.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les appareils.');
        this.loading.set(false);
      }
    });
  }

  protected activeDevices(): FlDevice[] {
    return this.devices().filter(d => d.status === 'ACTIVE');
  }

  protected revokedDevices(): FlDevice[] {
    return this.devices().filter(d => d.status === 'REVOKED');
  }

  protected generateToken(): void {
    this.busy.set(true);
    this.errorMessage.set('');
    this.api.generatePairingToken().subscribe({
      next: ({ data }) => {
        this.pairingToken.set(data);
        this.busy.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de générer le code de liaison.');
        this.busy.set(false);
      }
    });
  }

  protected confirmPairing(token: string): void {
    this.busy.set(true);
    this.errorMessage.set('');
    this.api.confirmPairing(token, this.confirmDeviceName || 'iPhone').subscribe({
      next: ({ data }) => {
        this.devices.update(list => [...list, data]);
        this.pairingToken.set(null);
        this.busy.set(false);
        this.showSuccess('iPhone relié avec succès.');
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error?.message ?? 'Liaison échouée.');
        this.busy.set(false);
      }
    });
  }

  protected revokeDevice(device: FlDevice): void {
    if (!confirm(`Révoquer l'appareil "${device.deviceName}" ?`)) return;
    this.busy.set(true);
    this.api.revokeDevice(device.id).subscribe({
      next: () => {
        this.devices.update(list => list.map(d => d.id === device.id ? { ...d, status: 'REVOKED' as const } : d));
        this.busy.set(false);
        this.showSuccess('Appareil révoqué.');
      },
      error: () => {
        this.errorMessage.set('Impossible de révoquer l\'appareil.');
        this.busy.set(false);
      }
    });
  }

  protected formatDate(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' });
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(''), 3000);
  }
}
