import { DestroyRef, effect, inject, Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { CookieConsentService } from './cookie-consent.service';

declare global {
  interface Window {
    _paq?: Array<unknown[]>;
  }
}

/**
 * Matomo analytics — only active when cookie consent is accepted.
 * Self-hosted Matomo instance at configurable URL.
 */
@Injectable({ providedIn: 'root' })
export class MatomoService {
  private readonly consent = inject(CookieConsentService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private initialized = false;
  private routeSub: { unsubscribe(): void } | null = null;

  /** Matomo instance URL — adjust for your deployment */
  private readonly matomoUrl = '/matomo/';
  private readonly siteId = '1';

  constructor() {
    // React to consent changes
    effect(() => {
      const accepted = this.consent.isAccepted();
      if (accepted && !this.initialized) {
        this.enable();
      } else if (!accepted && this.initialized) {
        this.disable();
      }
    });
  }

  private enable(): void {
    this.initialized = true;

    // Initialize Matomo tracker
    window._paq = window._paq || [];
    window._paq.push(['disableCookies']); // Use cookieless tracking by default
    window._paq.push(['setTrackerUrl', this.matomoUrl + 'matomo.php']);
    window._paq.push(['setSiteId', this.siteId]);
    window._paq.push(['enableLinkTracking']);
    window._paq.push(['trackPageView']);

    // Inject Matomo script
    const script = document.createElement('script');
    script.id = 'matomo-script';
    script.async = true;
    script.src = this.matomoUrl + 'matomo.js';
    document.head.appendChild(script);

    // Track route changes
    this.routeSub = this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe(event => {
      window._paq?.push(['setCustomUrl', event.urlAfterRedirects]);
      window._paq?.push(['setDocumentTitle', document.title]);
      window._paq?.push(['trackPageView']);
    });
  }

  private disable(): void {
    this.initialized = false;

    // Remove Matomo script
    const script = document.getElementById('matomo-script');
    if (script) script.remove();

    // Clear tracker
    delete window._paq;

    // Stop route tracking
    if (this.routeSub) {
      this.routeSub.unsubscribe();
      this.routeSub = null;
    }
  }
}
