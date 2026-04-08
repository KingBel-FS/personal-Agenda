import { Injectable, signal } from '@angular/core';

export type CookieConsent = 'accepted' | 'refused' | null;

const STORAGE_KEY = 'pht_cookie_consent';

@Injectable({ providedIn: 'root' })
export class CookieConsentService {
  private readonly _consent = signal<CookieConsent>(this.loadFromStorage());

  readonly consent = this._consent.asReadonly();

  hasDecided(): boolean {
    return this._consent() !== null;
  }

  accept(): void {
    this._consent.set('accepted');
    localStorage.setItem(STORAGE_KEY, 'accepted');
  }

  refuse(): void {
    this._consent.set('refused');
    localStorage.setItem(STORAGE_KEY, 'refused');
  }

  isAccepted(): boolean {
    return this._consent() === 'accepted';
  }

  private loadFromStorage(): CookieConsent {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'accepted' || stored === 'refused') {
      return stored;
    }
    return null;
  }
}
