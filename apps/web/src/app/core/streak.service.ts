import { Injectable, signal } from '@angular/core';
import type { StreakInfo } from '../features/today/today-api.service';

@Injectable({ providedIn: 'root' })
export class StreakService {
  readonly streak = signal<StreakInfo | null>(null);
  readonly celebrationBadge = signal<string | null>(null);

  set(streak: StreakInfo | null): void {
    this.streak.set(streak);
  }

  celebrate(badge: string | null): void {
    this.celebrationBadge.set(badge);
  }

  dismissCelebration(): void {
    this.celebrationBadge.set(null);
  }
}
