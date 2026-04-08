import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class BadgeService {

  update(activeCount: number): void {
    if (!('setAppBadge' in navigator)) return;

    if (activeCount > 0) {
      navigator.setAppBadge(activeCount).catch(() => {});
    } else {
      navigator.clearAppBadge().catch(() => {});
    }
  }

  clear(): void {
    if (!('clearAppBadge' in navigator)) return;
    navigator.clearAppBadge().catch(() => {});
  }
}
