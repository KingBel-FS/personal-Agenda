import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FocuslockApiService, FlRule, FlRuleRequest } from './focuslock-api.service';

type Tab = 'daily' | 'schedule' | 'web';

interface RuleForm {
  name: string;
  targetType: string;
  targetIdentifier: string;
  ruleType: string;
  limitMinutes: number | null;
  frictionType: string;
  scheduleStart: string;
  scheduleEnd: string;
  scheduleDays: Record<string, boolean>;
  domains: string;
}

@Component({
  selector: 'app-focuslock-rules',
  imports: [RouterLink, FormsModule],
  template: `
    <section class="fl-page">
      <header class="fl-hero">
        <div class="fl-hero__copy">
          <p class="fl-eyebrow">FocusLock</p>
          <h1>Règles</h1>
          <p class="fl-lead">Crée des limites quotidiennes, des blocages horaires et des filtres de domaines.</p>
        </div>
      </header>

      @if (successMessage()) {
        <p class="fl-success" role="status">{{ successMessage() }}</p>
      }
      @if (errorMessage()) {
        <p class="fl-error" role="alert">{{ errorMessage() }}</p>
      }

      <div class="fl-tabs" role="tablist">
        <button type="button" role="tab" class="fl-tab" [class.fl-tab--active]="tab() === 'daily'" (click)="tab.set('daily')" [attr.aria-selected]="tab() === 'daily'">Limites quotidiennes</button>
        <button type="button" role="tab" class="fl-tab" [class.fl-tab--active]="tab() === 'schedule'" (click)="tab.set('schedule')" [attr.aria-selected]="tab() === 'schedule'">Blocages horaires</button>
        <button type="button" role="tab" class="fl-tab" [class.fl-tab--active]="tab() === 'web'" (click)="tab.set('web')" [attr.aria-selected]="tab() === 'web'">Domaines web</button>
      </div>

      <!-- FORMULAIRE -->
      @if (showForm()) {
        <form class="fl-rule-builder" (ngSubmit)="submitForm()" #ruleForm="ngForm">
          <p class="fl-rule-builder__title">{{ editingRule() ? 'Modifier la règle' : 'Nouvelle règle' }}</p>

          <div class="fl-form-row">
            <label class="fl-label" for="fl-name">Nom de la règle</label>
            <input id="fl-name" class="fl-input" type="text" [(ngModel)]="form.name" name="name" required placeholder="ex. Mode travail">
          </div>

          @if (tab() !== 'web') {
            <div class="fl-form-row">
              <label class="fl-label" for="fl-target">Application ou catégorie cible</label>
              <input id="fl-target" class="fl-input" type="text" [(ngModel)]="form.targetIdentifier" name="targetIdentifier" required placeholder="ex. com.instagram.ios ou Instagram">
            </div>
          }

          @if (tab() === 'daily') {
            <div class="fl-form-row">
              <label class="fl-label" for="fl-limit">Durée maximale par jour (minutes)</label>
              <input id="fl-limit" class="fl-input" type="number" [(ngModel)]="form.limitMinutes" name="limitMinutes" required min="1" placeholder="30">
            </div>
          }

          @if (tab() === 'schedule') {
            <div class="fl-form-row">
              <label class="fl-label">Plage horaire</label>
              <div class="fl-time-range">
                <input class="fl-input" type="time" [(ngModel)]="form.scheduleStart" name="scheduleStart" required>
                <span>→</span>
                <input class="fl-input" type="time" [(ngModel)]="form.scheduleEnd" name="scheduleEnd" required>
              </div>
            </div>
            <div class="fl-form-row">
              <label class="fl-label">Jours</label>
              <div class="fl-days">
                @for (day of dayKeys; track day.key) {
                  <button
                    type="button"
                    class="fl-day"
                    [class.fl-day--active]="form.scheduleDays[day.key]"
                    (click)="toggleDay(day.key)"
                  >{{ day.label }}</button>
                }
              </div>
            </div>
          }

          @if (tab() === 'web') {
            <div class="fl-form-row">
              <label class="fl-label" for="fl-domains">Domaines à bloquer (un par ligne)</label>
              <textarea id="fl-domains" class="fl-input fl-textarea" [(ngModel)]="form.domains" name="domains" rows="4" placeholder="reddit.com&#10;twitter.com"></textarea>
            </div>
          }

          <div class="fl-form-row">
            <label class="fl-label" for="fl-friction">Friction sur override</label>
            <select id="fl-friction" class="fl-input" [(ngModel)]="form.frictionType" name="frictionType">
              <option value="NONE">Aucune</option>
              <option value="DELAY_60">Délai 60 secondes</option>
              <option value="CONFIRMATION">Double confirmation</option>
            </select>
          </div>

          <div class="fl-actions">
            <button type="submit" class="fl-btn fl-btn--primary" [disabled]="busy()">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
              {{ busy() ? 'Enregistrement...' : (editingRule() ? 'Mettre à jour' : 'Créer la règle') }}
            </button>
            <button type="button" class="fl-btn" (click)="cancelForm()">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              Annuler
            </button>
          </div>
        </form>
      } @else {
        <div class="fl-tab-content">
          <button type="button" class="fl-btn fl-btn--primary" (click)="openForm()">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            Nouvelle règle
          </button>
        </div>
      }

      <!-- LISTE DES RÈGLES -->
      @if (loading()) {
        <div class="fl-placeholder-bar fl-placeholder-bar--wide" style="margin-top:1.5rem"></div>
        <div class="fl-placeholder-bar" style="margin-top:0.5rem"></div>
      } @else {
        <ul class="fl-rule-list">
          @for (rule of filteredRules(); track rule.id) {
            <li class="fl-rule-item" [class.fl-rule-item--inactive]="!rule.active">
              <div class="fl-rule-item__head">
                <div class="fl-rule-item__info">
                  <strong>{{ rule.name }}</strong>
                  <span class="fl-rule-item__meta">
                    {{ rule.targetIdentifier }}
                    @if (rule.ruleType === 'DAILY_LIMIT') {
                      · {{ rule.limitMinutes }} min/jour
                    } @else {
                      @for (s of rule.schedules; track s.id) {
                        · {{ s.startTime }}–{{ s.endTime }} ({{ translateDays(s.daysOfWeek) }})
                      }
                    }
                    @if (rule.domains.length > 0) {
                      · {{ rule.domains.length }} domaine(s)
                    }
                  </span>
                </div>
                <div class="fl-rule-item__actions">
                  <button type="button" class="fl-rule-action" (click)="toggleRule(rule)" [title]="rule.active ? 'Désactiver' : 'Activer'">
                    @if (rule.active) {
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                    } @else {
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/></svg>
                    }
                  </button>
                  <button type="button" class="fl-rule-action" (click)="editRule(rule)" title="Modifier">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                  </button>
                  <button type="button" class="fl-rule-action fl-rule-action--danger" (click)="deleteRule(rule)" title="Supprimer">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
                  </button>
                </div>
              </div>
              @if (!rule.active) {
                <span class="fl-rule-item__badge">Inactive</span>
              }
              @if (rule.overrideCount > 0) {
                <span class="fl-rule-item__badge fl-rule-item__badge--warn">{{ rule.overrideCount }} override(s)</span>
              }
            </li>
          } @empty {
            <li class="fl-empty-state">
              <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <p>Aucune règle dans cette catégorie.</p>
              <p class="fl-empty-state__hint">Clique sur "+ Nouvelle règle" pour commencer.</p>
            </li>
          }
        </ul>
      }

      <div class="fl-actions" style="margin-top:1rem">
        <a routerLink="/focuslock" class="fl-btn">Retour au dashboard</a>
      </div>
    </section>
  `
})
export class FocuslockRulesPageComponent implements OnInit {
  private readonly api = inject(FocuslockApiService);

  protected readonly tab = signal<Tab>('daily');
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly showForm = signal(false);
  protected readonly editingRule = signal<FlRule | null>(null);
  protected readonly rules = signal<FlRule[]>([]);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');

  protected form: RuleForm = this.emptyForm();

  protected readonly dayKeys = [
    { key: 'MON', label: 'L' }, { key: 'TUE', label: 'M' }, { key: 'WED', label: 'M' },
    { key: 'THU', label: 'J' }, { key: 'FRI', label: 'V' }, { key: 'SAT', label: 'S' }, { key: 'SUN', label: 'D' }
  ];

  ngOnInit(): void {
    this.loadRules();
  }

  protected filteredRules(): FlRule[] {
    const t = this.tab();
    return this.rules().filter(r => {
      if (t === 'daily') return r.ruleType === 'DAILY_LIMIT';
      if (t === 'schedule') return r.ruleType === 'TIME_BLOCK';
      if (t === 'web') return r.domains.length > 0;
      return true;
    });
  }

  protected openForm(): void {
    this.form = this.emptyForm();
    this.editingRule.set(null);
    this.showForm.set(true);
    this.errorMessage.set('');
  }

  protected cancelForm(): void {
    this.showForm.set(false);
    this.editingRule.set(null);
  }

  protected editRule(rule: FlRule): void {
    this.editingRule.set(rule);
    const s = rule.schedules[0];
    const days: Record<string, boolean> = {};
    this.dayKeys.forEach(d => { days[d.key] = false; });
    if (s) {
      s.daysOfWeek.split(',').forEach(d => { days[d.trim()] = true; });
    }
    this.form = {
      name: rule.name,
      targetType: rule.targetType,
      targetIdentifier: rule.targetIdentifier,
      ruleType: rule.ruleType,
      limitMinutes: rule.limitMinutes,
      frictionType: rule.frictionType,
      scheduleStart: s?.startTime ?? '09:00',
      scheduleEnd: s?.endTime ?? '18:00',
      scheduleDays: days,
      domains: rule.domains.map(d => d.domain).join('\n')
    };
    this.showForm.set(true);
  }

  protected toggleDay(key: string): void {
    this.form.scheduleDays[key] = !this.form.scheduleDays[key];
  }

  protected submitForm(): void {
    this.errorMessage.set('');
    const t = this.tab();
    const request: FlRuleRequest = {
      name: this.form.name,
      targetType: t === 'web' ? 'DOMAIN' : (this.form.targetType || 'APP'),
      targetIdentifier: t === 'web' ? '_web_' : this.form.targetIdentifier,
      ruleType: t === 'daily' ? 'DAILY_LIMIT' : 'TIME_BLOCK',
      limitMinutes: t === 'daily' ? this.form.limitMinutes : null,
      frictionType: this.form.frictionType || 'NONE',
      schedules: t === 'schedule' ? [this.buildSchedule()] : [],
      domains: t === 'web' ? this.form.domains.split('\n').map(d => d.trim()).filter(Boolean) : []
    };

    this.busy.set(true);
    const editing = this.editingRule();
    const call = editing
      ? this.api.updateRule(editing.id, request)
      : this.api.createRule(request);

    call.subscribe({
      next: ({ data }) => {
        if (editing) {
          this.rules.update(list => list.map(r => r.id === data.id ? data : r));
          this.showSuccess('Règle mise à jour.');
        } else {
          this.rules.update(list => [data, ...list]);
          this.showSuccess('Règle créée.');
        }
        this.busy.set(false);
        this.cancelForm();
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error?.message ?? 'Impossible d\'enregistrer la règle.');
        this.busy.set(false);
      }
    });
  }

  protected toggleRule(rule: FlRule): void {
    this.api.toggleRule(rule.id).subscribe({
      next: ({ data }) => {
        this.rules.update(list => list.map(r => r.id === data.id ? data : r));
      },
      error: () => this.errorMessage.set('Impossible de modifier l\'état de la règle.')
    });
  }

  protected deleteRule(rule: FlRule): void {
    if (!confirm(`Supprimer la règle "${rule.name}" ?`)) return;
    this.api.deleteRule(rule.id).subscribe({
      next: () => {
        this.rules.update(list => list.filter(r => r.id !== rule.id));
        this.showSuccess('Règle supprimée.');
      },
      error: () => this.errorMessage.set('Impossible de supprimer la règle.')
    });
  }

  private static readonly DAY_FR: Record<string, string> = {
    MON: 'LUN', TUE: 'MAR', WED: 'MER', THU: 'JEU', FRI: 'VEN', SAT: 'SAM', SUN: 'DIM'
  };

  protected translateDays(csv: string): string {
    return csv.split(',').map(d => FocuslockRulesPageComponent.DAY_FR[d.trim()] ?? d.trim()).join(',');
  }

  private loadRules(): void {
    this.loading.set(true);
    this.api.listRules().subscribe({
      next: ({ data }) => {
        this.rules.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les règles.');
        this.loading.set(false);
      }
    });
  }

  private buildSchedule(): { startTime: string; endTime: string; daysOfWeek: string } {
    const days = this.dayKeys.filter(d => this.form.scheduleDays[d.key]).map(d => d.key).join(',');
    return { startTime: this.form.scheduleStart, endTime: this.form.scheduleEnd, daysOfWeek: days || 'MON' };
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(''), 3000);
  }

  private emptyForm(): RuleForm {
    const days: Record<string, boolean> = {};
    ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'].forEach(d => { days[d] = d !== 'SAT' && d !== 'SUN'; });
    return {
      name: '', targetType: 'APP', targetIdentifier: '', ruleType: 'DAILY_LIMIT',
      limitMinutes: 30, frictionType: 'NONE', scheduleStart: '09:00', scheduleEnd: '18:00',
      scheduleDays: days, domains: ''
    };
  }
}
