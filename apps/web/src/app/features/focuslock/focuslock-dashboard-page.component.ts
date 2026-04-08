import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FocuslockApiService, FlDashboard, FlRule } from './focuslock-api.service';

@Component({
  selector: 'app-focuslock-dashboard',
  imports: [RouterLink],
  template: `
    <section class="fl-page">
      <header class="fl-hero">
        <div class="fl-hero__copy">
          <p class="fl-eyebrow">FocusLock</p>
          <h1>Dashboard</h1>
          <p class="fl-lead">Vue d'ensemble de tes verrous actifs et du temps consommé aujourd'hui.</p>
        </div>
        @if (dashboard(); as d) {
          @if (d.activeDevice) {
            <div class="fl-device-badge fl-device-badge--ok">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
              {{ d.activeDevice.deviceName }} actif
            </div>
          } @else {
            <div class="fl-device-badge fl-device-badge--pending">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
              iPhone non relié
            </div>
          }
        }
      </header>

      @if (!dashboard()?.activeDevice) {
        <div class="fl-notice" role="note">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <p>L'enforcement des restrictions nécessite l'app iOS FocusLock. <a routerLink="/focuslock/device" class="fl-inline-link">Relier mon iPhone →</a></p>
        </div>
      }

      @if (errorMessage()) {
        <p class="fl-error" role="alert">{{ errorMessage() }}</p>
      }

      @if (loading()) {
        <div class="fl-grid">
          @for (_ of [1,2,3,4]; track $index) {
            <article class="fl-card">
              <div class="fl-placeholder-bar fl-placeholder-bar--wide"></div>
              <div class="fl-placeholder-bar"></div>
            </article>
          }
        </div>
      } @else if (dashboard(); as d) {
        <div class="fl-grid">
          <article class="fl-card">
            <p class="fl-card__eyebrow">Aujourd'hui</p>
            <h2 class="fl-card__title">Temps total consommé</h2>
            <div class="fl-big-stat">
              <span class="fl-big-stat__value">{{ formatMinutes(d.totalMinutesToday) }}</span>
            </div>
            @if (d.todayUsage.length > 0) {
              <ul class="fl-usage-list">
                @for (u of d.todayUsage; track u.targetIdentifier) {
                  <li class="fl-usage-row">
                    <span>{{ u.targetIdentifier }}</span>
                    <strong>{{ formatMinutes(u.consumedMinutes) }}</strong>
                  </li>
                }
              </ul>
            } @else {
              <p class="fl-card__hint">Aucune donnée d'usage pour aujourd'hui.</p>
            }
          </article>

          <article class="fl-card">
            <p class="fl-card__eyebrow">Règles</p>
            <h2 class="fl-card__title">Verrous actifs</h2>
            <div class="fl-big-stat">
              <span class="fl-big-stat__value">{{ d.activeRuleCount }}</span>
              <span class="fl-big-stat__sub">/ {{ d.totalRuleCount }} au total</span>
            </div>
            @if (d.activeRules.length > 0) {
              <ul class="fl-rule-chips">
                @for (r of d.activeRules.slice(0, 4); track r.id) {
                  <li class="fl-rule-chip" [attr.data-type]="r.ruleType">
                    {{ r.name }}
                    @if (r.ruleType === 'DAILY_LIMIT') {
                      <span>{{ r.limitMinutes }} min</span>
                    } @else {
                      <span>Horaire</span>
                    }
                  </li>
                }
              </ul>
            } @else {
              <p class="fl-card__hint">Aucune règle active. <a routerLink="/focuslock/rules" class="fl-inline-link">Créer une règle →</a></p>
            }
          </article>

          <article class="fl-card">
            <p class="fl-card__eyebrow">Proche du seuil</p>
            <h2 class="fl-card__title">Limites quotidiennes</h2>
            @if (rulesNearLimit(d).length > 0) {
              @for (r of rulesNearLimit(d); track r.id) {
                <div class="fl-gauge">
                  <div class="fl-gauge__track">
                    <div class="fl-gauge__fill fl-gauge__fill--warn" [style.width]="usagePct(d, r) + '%'"></div>
                  </div>
                  <span>{{ usagePct(d, r) }}% — {{ r.name }}</span>
                </div>
              }
            } @else {
              <p class="fl-card__hint">Aucune limite proche du seuil.</p>
            }
          </article>

          <article class="fl-card">
            <p class="fl-card__eyebrow">Appareil</p>
            <h2 class="fl-card__title">État iPhone</h2>
            @if (d.activeDevice; as device) {
              <ul class="fl-permission-mini-list">
                <li [class.fl-permission-mini--ok]="device.familyControlsGranted" [class.fl-permission-mini--ko]="!device.familyControlsGranted">
                  Family Controls
                </li>
                <li [class.fl-permission-mini--ok]="device.screenTimeGranted" [class.fl-permission-mini--ko]="!device.screenTimeGranted">
                  Screen Time
                </li>
                <li [class.fl-permission-mini--ok]="device.notificationsGranted" [class.fl-permission-mini--ko]="!device.notificationsGranted">
                  Notifications
                </li>
              </ul>
              <p class="fl-card__hint">Vu : {{ device.lastSeenAt ? formatDate(device.lastSeenAt) : 'jamais' }}</p>
            } @else {
              <p class="fl-card__hint">Aucun iPhone relié.</p>
              <a routerLink="/focuslock/device" class="fl-btn fl-btn--primary" style="margin-top:0.5rem">Relier</a>
            }
          </article>
        </div>
      }

      <div class="fl-actions">
        <a routerLink="/focuslock/rules" class="fl-btn fl-btn--primary">Gérer les règles</a>
        <a routerLink="/focuslock/device" class="fl-btn">État iPhone</a>
        <a routerLink="/focuslock/insights" class="fl-btn">Insights</a>
      </div>
    </section>
  `
})
export class FocuslockDashboardPageComponent implements OnInit {
  private readonly api = inject(FocuslockApiService);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly dashboard = signal<FlDashboard | null>(null);

  ngOnInit(): void {
    this.api.getDashboard().subscribe({
      next: ({ data }) => {
        this.dashboard.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le dashboard FocusLock.');
        this.loading.set(false);
      }
    });
  }

  protected rulesNearLimit(d: FlDashboard): FlRule[] {
    return d.activeRules.filter(r => {
      if (r.ruleType !== 'DAILY_LIMIT' || !r.limitMinutes) return false;
      const usage = d.todayUsage.find(u => u.targetIdentifier === r.targetIdentifier);
      if (!usage) return false;
      return (usage.consumedMinutes / r.limitMinutes) >= 0.5;
    });
  }

  protected usagePct(d: FlDashboard, r: FlRule): number {
    if (!r.limitMinutes) return 0;
    const usage = d.todayUsage.find(u => u.targetIdentifier === r.targetIdentifier);
    if (!usage) return 0;
    return Math.min(100, Math.round((usage.consumedMinutes / r.limitMinutes) * 100));
  }

  protected formatMinutes(min: number): string {
    if (min < 60) return `${min} min`;
    const h = Math.floor(min / 60);
    const m = min % 60;
    return m > 0 ? `${h}h ${m}min` : `${h}h`;
  }

  protected formatDate(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' });
  }
}
