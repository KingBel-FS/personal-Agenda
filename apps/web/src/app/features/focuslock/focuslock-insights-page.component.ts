import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FocuslockApiService, FlInsights } from './focuslock-api.service';

@Component({
  selector: 'app-focuslock-insights',
  imports: [RouterLink],
  template: `
    <section class="fl-page">
      <header class="fl-hero">
        <div class="fl-hero__copy">
          <p class="fl-eyebrow">FocusLock</p>
          <h1>Insights</h1>
          <p class="fl-lead">Comprends tes tendances d'usage et repère les heures critiques de rechute.</p>
        </div>
      </header>

      @if (errorMessage()) {
        <p class="fl-error" role="alert">{{ errorMessage() }}</p>
      }

      @if (loading()) {
        <div class="fl-grid">
          @for (_ of [1,2]; track $index) {
            <article class="fl-card fl-card--full">
              <div class="fl-placeholder-bar fl-placeholder-bar--wide"></div>
              <div class="fl-placeholder-bar"></div>
            </article>
          }
        </div>
      } @else if (insights(); as ins) {
        <div class="fl-grid">
          <article class="fl-card fl-card--full">
            <p class="fl-card__eyebrow">Cette semaine</p>
            <h2 class="fl-card__title">Progression hebdomadaire</h2>
            @if (ins.weeklyBreakdown.length > 0) {
              <div class="fl-bar-chart" aria-label="Temps consommé par jour cette semaine">
                @for (day of ins.weeklyBreakdown; track day.date) {
                  <div class="fl-bar-chart__col">
                    <strong class="fl-bar-chart__value">{{ formatMinutesShort(day.totalMinutes) }}</strong>
                    <div class="fl-bar-chart__bar" [style.height]="barHeight(day.totalMinutes, maxDaily(ins)) + '%'"></div>
                    <span class="fl-bar-chart__label">{{ day.dayLabel }}</span>
                  </div>
                }
              </div>
              <p class="fl-card__hint">Total : {{ formatMinutes(weekTotal(ins)) }}</p>
            } @else {
              <div class="fl-empty-state">
                <p>Aucune donnée d'usage cette semaine.</p>
                <p class="fl-empty-state__hint">Les données apparaissent une fois l'iPhone relié et actif.</p>
              </div>
            }
          </article>

          <article class="fl-card">
            <p class="fl-card__eyebrow">Top distractions</p>
            <h2 class="fl-card__title">Apps les plus consommées</h2>
            @if (ins.topApps.length > 0) {
              <ul class="fl-distraction-list">
                @for (app of ins.topApps; track app.targetIdentifier) {
                  <li class="fl-distraction-item">
                    <span class="fl-distraction-item__name">{{ app.targetIdentifier }}</span>
                    <div class="fl-gauge fl-gauge--inline">
                      <div class="fl-gauge__track">
                        <div class="fl-gauge__fill fl-gauge__fill--warn" [style.width]="appPct(app.consumedMinutes, ins) + '%'"></div>
                      </div>
                    </div>
                    <span class="fl-distraction-item__pct">{{ formatMinutesShort(app.consumedMinutes) }}</span>
                  </li>
                }
              </ul>
            } @else {
              <div class="fl-empty-state">
                <p>Aucune donnée d'usage disponible.</p>
                <p class="fl-empty-state__hint">Relie ton iPhone pour voir les stats.</p>
              </div>
            }
          </article>

          <article class="fl-card">
            <p class="fl-card__eyebrow">Résumé</p>
            <h2 class="fl-card__title">Cette semaine en chiffres</h2>
            <div class="fl-kpi-grid">
              <div class="fl-kpi">
                <span>Jours actifs</span>
                <strong>{{ activeDays(ins) }}</strong>
              </div>
              <div class="fl-kpi">
                <span>Temps total</span>
                <strong>{{ formatMinutes(weekTotal(ins)) }}</strong>
              </div>
              <div class="fl-kpi">
                <span>Apps suivies</span>
                <strong>{{ ins.topApps.length }}</strong>
              </div>
              <div class="fl-kpi">
                <span>Pic journalier</span>
                <strong>{{ formatMinutes(maxDaily(ins)) }}</strong>
              </div>
            </div>
          </article>
        </div>
      }

      <div class="fl-actions">
        <a routerLink="/focuslock" class="fl-btn">Retour au dashboard</a>
        <a routerLink="/focuslock/rules" class="fl-btn">Gérer les règles</a>
      </div>
    </section>
  `
})
export class FocuslockInsightsPageComponent implements OnInit {
  private readonly api = inject(FocuslockApiService);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly insights = signal<FlInsights | null>(null);

  ngOnInit(): void {
    this.api.getInsights().subscribe({
      next: ({ data }) => {
        this.insights.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les insights.');
        this.loading.set(false);
      }
    });
  }

  protected maxDaily(ins: FlInsights): number {
    return Math.max(...ins.weeklyBreakdown.map(d => d.totalMinutes), 1);
  }

  protected barHeight(minutes: number, max: number): number {
    return Math.max(4, Math.round((minutes / max) * 100));
  }

  protected weekTotal(ins: FlInsights): number {
    return ins.weeklyBreakdown.reduce((acc, d) => acc + d.totalMinutes, 0);
  }

  protected activeDays(ins: FlInsights): number {
    return ins.weeklyBreakdown.filter(d => d.totalMinutes > 0).length;
  }

  protected appPct(minutes: number, ins: FlInsights): number {
    const max = Math.max(...ins.topApps.map(a => a.consumedMinutes), 1);
    return Math.round((minutes / max) * 100);
  }

  protected formatMinutes(min: number): string {
    if (min === 0) return '—';
    if (min < 60) return `${min} min`;
    const h = Math.floor(min / 60);
    const m = min % 60;
    return m > 0 ? `${h}h ${m}min` : `${h}h`;
  }

  protected formatMinutesShort(min: number): string {
    if (min === 0) return '—';
    if (min < 60) return `${min}m`;
    const h = Math.floor(min / 60);
    const m = min % 60;
    return m > 0 ? `${h}h${m}` : `${h}h`;
  }
}
