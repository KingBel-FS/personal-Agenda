import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-task-card',
  template: `
    <article
      class="task-card"
      [class]="'task-card task-card--' + status()"
      [class.task-card--busy]="busy()"
      role="button"
      [attr.aria-label]="ariaLabel()"
      [attr.tabindex]="disabled() ? -1 : 0"
      (pointerdown)="pointerDown.emit()"
      (pointerup)="pointerUp.emit()"
      (pointerleave)="pointerLeave.emit()"
      (keydown.enter)="pointerUp.emit()"
      (keydown.space)="pointerUp.emit(); $event.preventDefault()"
      (contextmenu)="$event.preventDefault()"
    >
      <span class="task-card__icon" aria-hidden="true">{{ icon() }}</span>
      <div class="task-card__body">
        <span class="task-card__title">{{ title() }}</span>
        @if (description()) {
          <span class="task-card__description">{{ description() }}</span>
        }
        @if (totalSlotsPerDay() > 1 && slotOrder() !== null) {
          <span class="task-card__slot" [attr.aria-label]="'Répétition ' + slotOrder() + ' sur ' + totalSlotsPerDay()">
            {{ slotOrder() }}/{{ totalSlotsPerDay() }}
          </span>
        }
      </div>
      <div class="task-card__meta">
        <time class="task-card__time" [attr.datetime]="dateTime()">{{ time() }}</time>
        <span class="task-card__status" [class]="'task-card__status task-card__status--' + status()" aria-hidden="true">
          {{ statusIcon() }}
        </span>
      </div>
    </article>
  `,
  styles: [`
    .task-card {
      display: flex;
      align-items: center;
      gap: 0.875rem;
      padding: 0.95rem 1rem;
      background: var(--surface);
      border-radius: 0.9rem;
      border: 1px solid var(--border);
      box-shadow: var(--shadow-xs);
      transition: opacity var(--dur) var(--ease), transform 80ms var(--ease), background var(--dur) var(--ease), border-color var(--dur) var(--ease);
      cursor: pointer;
      user-select: none;
      -webkit-user-select: none;
      -webkit-touch-callout: none;
    }

    .task-card:active:not(.task-card--busy):not(.task-card--skipped) {
      transform: scale(0.98);
      background: var(--surface-2);
    }

    .task-card:focus-visible {
      outline: 3px solid var(--brand-ring);
      outline-offset: 2px;
    }

    .task-card--done {
      background: color-mix(in srgb, var(--surface) 78%, var(--status-done-bg) 22%);
    }

    .task-card--done .task-card__title {
      color: var(--ink-3);
      text-decoration: line-through;
    }

    .task-card--missed {
      border-color: color-mix(in srgb, var(--status-missed-ink) 42%, var(--border) 58%);
      border-style: dashed;
    }

    .task-card--missed .task-card__title {
      color: var(--status-missed-ink);
    }

    .task-card--skipped {
      opacity: 0.55;
      border-style: dashed;
      cursor: default;
    }

    .task-card--busy {
      pointer-events: none;
      opacity: 0.8;
    }

    .task-card__icon {
      font-size: 1.5rem;
      flex-shrink: 0;
      width: 2.25rem;
      text-align: center;
      line-height: 1;
    }

    .task-card__body {
      flex: 1;
      min-width: 0;
      display: grid;
      gap: 0.14rem;
    }

    .task-card__title {
      font-weight: 700;
      font-size: 0.95rem;
      color: var(--ink);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .task-card__description {
      font-size: 0.8rem;
      color: var(--ink-3);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .task-card__slot {
      display: inline-flex;
      align-items: center;
      width: fit-content;
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.1rem 0.42rem;
      border-radius: 999px;
      background: var(--brand-soft);
      color: var(--brand);
    }

    .task-card__meta {
      display: grid;
      justify-items: end;
      gap: 0.18rem;
      flex-shrink: 0;
    }

    .task-card__time {
      font-size: 0.82rem;
      font-weight: 700;
      color: var(--ink-2);
      font-variant-numeric: tabular-nums;
    }

    .task-card__status {
      font-size: 0.9rem;
      font-weight: 800;
    }

    .task-card__status--planned {
      color: transparent;
    }

    .task-card__status--done {
      color: var(--status-done-ink);
    }

    .task-card__status--missed {
      color: var(--status-missed-ink);
    }

    .task-card__status--skipped {
      color: var(--status-skipped-ink);
    }
  `]
})
export class TaskCardComponent {
  readonly icon = input.required<string>();
  readonly title = input.required<string>();
  readonly description = input('');
  readonly time = input.required<string>();
  readonly dateTime = input.required<string>();
  readonly status = input.required<'planned' | 'done' | 'missed' | 'skipped' | 'canceled'>();
  readonly ariaLabel = input.required<string>();
  readonly slotOrder = input<number | null>(null);
  readonly totalSlotsPerDay = input(1);
  readonly busy = input(false);
  readonly disabled = input(false);
  readonly pointerDown = output<void>();
  readonly pointerUp = output<void>();
  readonly pointerLeave = output<void>();

  protected statusIcon(): string {
    switch (this.status()) {
      case 'done':
        return '✓';
      case 'missed':
        return '✕';
      case 'skipped':
        return '—';
      default:
        return '';
    }
  }
}
