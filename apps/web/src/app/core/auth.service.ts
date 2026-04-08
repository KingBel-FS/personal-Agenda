import { Injectable, signal } from '@angular/core';

const ACCESS_TOKEN_STORAGE_KEY = 'pht_access_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _accessToken = signal<string | null>(this.readStoredAccessToken());

  readonly accessToken = this._accessToken.asReadonly();

  isAuthenticated(): boolean {
    return this._accessToken() !== null;
  }

  setAccessToken(token: string): void {
    this._accessToken.set(token);
    localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, token);
  }

  clearAccessToken(): void {
    this._accessToken.set(null);
    localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
  }

  getAccessToken(): string | null {
    return this._accessToken();
  }

  private readStoredAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
  }
}
