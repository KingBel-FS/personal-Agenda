import { Component, input } from '@angular/core';

@Component({
  selector: 'app-step-form',
  template: `
    <section class="step-form" [attr.aria-labelledby]="titleId()">
      <div class="step-form__head">
        <div>
          @if (eyebrow()) {
            <p class="step-form__eyebrow">{{ eyebrow() }}</p>
          }
          <h2 class="step-form__title" [id]="titleId()">{{ title() }}</h2>
          @if (subtitle()) {
            <p class="step-form__subtitle">{{ subtitle() }}</p>
          }
        </div>
      </div>

      <div class="step-form__body">
        <ng-content />
      </div>
    </section>
  `,
  styles: [`
    .step-form {
      display: grid;
      gap: var(--sp-5);
    }

    .step-form__head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: var(--sp-3);
      flex-wrap: wrap;
    }

    .step-form__eyebrow {
      margin: 0 0 var(--sp-1);
      font-size: 0.7rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.12em;
      color: var(--brand);
    }

    .step-form__title {
      margin: 0;
      font-size: 1.125rem;
      font-weight: 700;
      color: var(--ink);
    }

    .step-form__subtitle {
      margin: var(--sp-2) 0 0;
      font-size: 0.9rem;
      color: var(--ink-3);
    }

    .step-form__body {
      display: grid;
      gap: var(--sp-4);
    }
  `]
})
export class StepFormComponent {
  readonly title = input.required<string>();
  readonly titleId = input.required<string>();
  readonly eyebrow = input('');
  readonly subtitle = input('');
}
