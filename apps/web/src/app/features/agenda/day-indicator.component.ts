import { Component, input, output } from '@angular/core';
import { AgendaDaySummary } from './agenda-api.service';

@Component({
  selector: 'app-day-indicator',
  templateUrl: './day-indicator.component.html',
  styleUrl: './day-indicator.component.scss'
})
export class DayIndicatorComponent {
  readonly day = input.required<AgendaDaySummary>();
  readonly buttonId = input('');
  readonly ariaLabel = input('');
  readonly selected = input(false);
  readonly activate = output<void>();
  readonly navigate = output<KeyboardEvent>();

  protected onActivate(): void {
    if (this.day().beforeAccountCreation) {
      return;
    }
    this.activate.emit();
  }

  protected onNavigate(event: KeyboardEvent): void {
    if (this.day().beforeAccountCreation) {
      return;
    }
    this.navigate.emit(event);
  }
}
