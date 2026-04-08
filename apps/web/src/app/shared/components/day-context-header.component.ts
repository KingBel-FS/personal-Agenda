import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-day-context-header',
  template: `
    <header class="day-context" aria-label="Contexte du jour">
      <div class="day-context__top">
        <div class="day-context__date">
          <span class="day-context__day-label">{{ dateLabel() }}</span>
          <span class="day-context__badge" [class]="'day-context__badge day-context__badge--' + dayVariant()">
            {{ categoryLabel() }}
          </span>
        </div>

        @if (showCreate()) {
          <button type="button" class="day-context__create" (click)="create.emit()">
            Créer une tâche
          </button>
        }
      </div>

      <div
        class="day-context__progress-wrap"
        role="progressbar"
        [attr.aria-valuenow]="progressPercent()"
        aria-valuemin="0"
        aria-valuemax="100"
        [attr.aria-label]="'Progression : ' + progressPercent() + '%'"
      >
        <div class="day-context__progress" [style.width.%]="progressPercent()"></div>
      </div>
      <p class="day-context__progress-label">{{ progressPercent() }}% accompli</p>

      <div class="day-context__stats" aria-label="Résumé du jour">
        <div class="day-context__chip day-context__chip--active">
          <span class="day-context__chip-value">{{ activeCount() }}</span>
          <span class="day-context__chip-label">à faire</span>
        </div>
        <div class="day-context__chip day-context__chip--done">
          <span class="day-context__chip-value">{{ doneCount() }}</span>
          <span class="day-context__chip-label">faites</span>
        </div>
        @if (missedCount() > 0) {
          <div class="day-context__chip day-context__chip--missed">
            <span class="day-context__chip-value">{{ missedCount() }}</span>
            <span class="day-context__chip-label">manquées</span>
          </div>
        }
        @if (skippedCount() > 0) {
          <div class="day-context__chip day-context__chip--skipped">
            <span class="day-context__chip-value">{{ skippedCount() }}</span>
            <span class="day-context__chip-label">ignorées</span>
          </div>
        }
      </div>

      @if (hint()) {
        <p class="day-context__hint">{{ hint() }}</p>
      }
    </header>
  `,
  styles: [`
    .day-context {
      background: var(--surface);
      border-radius: 1rem;
      padding: 1.25rem 1.25rem 1rem;
      margin-bottom: 1.25rem;
      box-shadow: var(--shadow-sm);
      border: 1px solid var(--border);
      display: grid;
      gap: 0.9rem;
    }

    .day-context__top {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.9rem;
      flex-wrap: wrap;
    }

    .day-context__date {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
    }

    .day-context__day-label {
      font-size: 1.1rem;
      font-weight: 700;
      color: var(--ink);
      text-transform: capitalize;
    }

    .day-context__badge {
      display: inline-flex;
      align-items: center;
      padding: 0.24rem 0.72rem;
      border-radius: 999px;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.02em;
      border: 1px solid transparent;
    }

    .day-context__badge--workday {
      background: var(--semantic-workday-bg);
      color: var(--semantic-workday-ink);
      border-color: color-mix(in srgb, var(--semantic-workday-ink) 16%, transparent);
    }

    .day-context__badge--vacation {
      background: var(--semantic-vacation-bg);
      color: var(--semantic-vacation-ink);
      border-color: color-mix(in srgb, var(--semantic-vacation-ink) 16%, transparent);
    }

    .day-context__badge--weekend-holiday {
      background: var(--semantic-weekend-bg);
      color: var(--semantic-weekend-ink);
      border-color: color-mix(in srgb, var(--semantic-weekend-ink) 16%, transparent);
    }

    .day-context__create {
      min-height: 2.75rem;
    }

    .day-context__progress-wrap {
      height: 8px;
      background: var(--bg-alt);
      border-radius: 999px;
      overflow: hidden;
    }

    .day-context__progress {
      height: 100%;
      background: linear-gradient(90deg, var(--brand), color-mix(in srgb, var(--brand) 68%, white 32%));
      border-radius: 999px;
      transition: width var(--dur-md) var(--ease);
    }

    .day-context__progress-label {
      font-size: 0.78rem;
      color: var(--ink-3);
      margin: -0.3rem 0 0;
    }

    .day-context__stats {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    .day-context__chip {
      flex: 1;
      min-width: 4.7rem;
      display: grid;
      justify-items: center;
      gap: 0.12rem;
      padding: 0.55rem 0.35rem;
      border-radius: 0.75rem;
      border: 1px solid transparent;
    }

    .day-context__chip-value {
      font-size: 1.25rem;
      font-weight: 800;
      line-height: 1;
    }

    .day-context__chip-label {
      font-size: 0.65rem;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      text-align: center;
    }

    .day-context__chip--active {
      background: var(--status-planned-bg);
      color: var(--status-planned-ink);
    }

    .day-context__chip--done {
      background: var(--status-done-bg);
      color: var(--status-done-ink);
    }

    .day-context__chip--missed {
      background: var(--status-missed-bg);
      color: var(--status-missed-ink);
    }

    .day-context__chip--skipped {
      background: var(--status-skipped-bg);
      color: var(--status-skipped-ink);
    }

    .day-context__hint {
      margin: 0;
      font-size: 0.72rem;
      color: var(--ink-4);
      text-align: center;
    }

    @media (max-width: 639px) {
      .day-context__top {
        flex-direction: column;
        align-items: stretch;
      }

      .day-context__create {
        width: 100%;
      }
    }
  `]
})
export class DayContextHeaderComponent {
  readonly dateLabel = input.required<string>();
  readonly categoryLabel = input.required<string>();
  readonly dayVariant = input.required<'workday' | 'vacation' | 'weekend-holiday'>();
  readonly progressPercent = input.required<number>();
  readonly activeCount = input.required<number>();
  readonly doneCount = input.required<number>();
  readonly missedCount = input(0);
  readonly skippedCount = input(0);
  readonly hint = input('');
  readonly showCreate = input(false);
  readonly create = output<void>();
}
