import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-streak-flame',
  template: `
    <div class="streak-flame" [class.streak-flame--nav]="variant() === 'nav'" [class.streak-flame--mobile]="variant() === 'mobile'">
      <div class="streak-flame__badge">
        <span class="streak-flame__emoji" [class.streak-flame__emoji--active]="active()">{{ active() ? '🔥' : '🪵' }}</span>
      </div>
      <div class="streak-flame__content">
        <span class="streak-flame__label">{{ label() }}</span>
        <strong class="streak-flame__value">{{ currentLabel() }}</strong>
        <span class="streak-flame__state" [class.streak-flame__state--active]="active()">
          {{ active() ? 'Série active' : 'Série en pause' }}
        </span>
        <span class="streak-flame__sub">Record {{ best() }} j</span>
      </div>
    </div>
  `,
  styles: [`
    .streak-flame {
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 0.75rem;
      align-items: center;
      min-width: 0;
    }

    .streak-flame__badge {
      width: 3rem;
      height: 3rem;
      display: grid;
      place-items: center;
      border-radius: 1rem;
      background: var(--streak-badge-bg);
      border: 1px solid var(--streak-border);
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
    }

    .streak-flame--nav .streak-flame__badge {
      background:
        radial-gradient(circle at 35% 25%, rgba(255, 226, 122, 0.22), transparent 40%),
        var(--streak-badge-bg);
    }

    .streak-flame__emoji {
      font-size: 1.35rem;
      filter: grayscale(0.7);
      transform: translateY(1px);
      transition: filter var(--dur) var(--ease), transform var(--dur) var(--ease);
    }

    .streak-flame__emoji--active {
      filter: none;
      animation: streakPulse 2.8s ease-in-out infinite;
    }

    .streak-flame__content {
      display: grid;
      gap: 0.1rem;
      min-width: 0;
    }

    .streak-flame__label {
      font-size: 0.68rem;
      text-transform: uppercase;
      letter-spacing: 0.12em;
      color: var(--streak-label);
    }

    .streak-flame__value {
      font-size: 1rem;
      line-height: 1.1;
      color: var(--streak-value);
    }

    .streak-flame__state {
      font-size: 0.72rem;
      font-weight: 700;
      color: var(--streak-sub);
    }

    .streak-flame__state--active {
      color: var(--streak-accent);
    }

    .streak-flame__sub {
      font-size: 0.76rem;
      color: var(--streak-sub);
    }

    .streak-flame--mobile .streak-flame__badge {
      width: 2.6rem;
      height: 2.6rem;
      border-radius: 0.9rem;
    }

    .streak-flame--mobile .streak-flame__label {
      color: var(--ink-3);
    }

    .streak-flame--mobile .streak-flame__value {
      color: var(--ink);
    }

    .streak-flame--mobile .streak-flame__sub {
      color: var(--ink-3);
    }

    @keyframes streakPulse {
      0%, 100% { transform: translateY(1px) scale(1); }
      50% { transform: translateY(-1px) scale(1.06); }
    }

    @media (prefers-reduced-motion: reduce) {
      .streak-flame__emoji,
      .streak-flame__emoji--active {
        animation: none;
      }
    }
  `]
})
export class StreakFlameComponent {
  readonly current = input(0);
  readonly best = input(0);
  readonly active = input(false);
  readonly label = input('Série en cours');
  readonly variant = input<'nav' | 'mobile'>('nav');

  protected readonly currentLabel = computed(() => `${this.current()} jour${this.current() > 1 ? 's' : ''}`);
}
