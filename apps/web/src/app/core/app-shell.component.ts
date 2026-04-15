import { Component, DestroyRef, HostListener, effect, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { TodayApiService } from '../features/today/today-api.service';
import { AuthService } from './auth.service';
import { BadgeService } from './badge.service';
import { MatomoService } from './matomo.service';
import { NetworkStatusService } from './network-status.service';
import { NotificationBannerComponent } from './notification-banner.component';
import { OfflineBannerComponent } from './offline-banner.component';
import { RealtimeSyncService } from './realtime-sync.service';
import { StreakService } from './streak.service';
import { StreakFlameComponent } from '../shared/components/streak-flame.component';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBannerComponent, OfflineBannerComponent, StreakFlameComponent],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss'
})
export class AppShellComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly badgeService = inject(BadgeService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly todayApi = inject(TodayApiService);
  private readonly networkStatusService = inject(NetworkStatusService);
  private readonly realtimeSyncService = inject(RealtimeSyncService);
  private readonly streakService = inject(StreakService);
  private readonly _matomo = inject(MatomoService);

  protected readonly darkMode = signal(false);
  protected readonly mobileNavOpen = signal(false);
  protected readonly loggingOut = signal(false);
  protected readonly streak = this.streakService.streak;
  protected readonly celebrationBadge = this.streakService.celebrationBadge;
  private celebrationTimer: ReturnType<typeof setTimeout> | null = null;
  private themeMediaQuery?: MediaQueryList;
  private readonly onThemePreferenceChange = (event: MediaQueryListEvent) => {
    if (localStorage.getItem('pht_theme')) {
      return;
    }
    this.darkMode.set(event.matches);
    this.applyTheme(event.matches);
  };

  constructor() {
    effect(() => {
      const badge = this.celebrationBadge();
      if (!badge) {
        return;
      }
      if (this.celebrationTimer) {
        clearTimeout(this.celebrationTimer);
      }
      this.celebrationTimer = setTimeout(() => this.dismissCelebration(), 3200);
    });
  }

  ngOnInit(): void {
    const stored = localStorage.getItem('pht_theme');
    this.themeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const prefersDark = this.themeMediaQuery.matches;
    const isDark = stored === 'dark' || (!stored && prefersDark);
    this.darkMode.set(isDark);
    this.applyTheme(isDark);
    this.themeMediaQuery.addEventListener('change', this.onThemePreferenceChange);

    document.body.classList.add('has-shell');
    this.networkStatusService.start();
    this.realtimeSyncService.start();
    this.refreshStreak();
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.mobileNavOpen.set(false);
        this.refreshStreak();
      });
    this.realtimeSyncService.events$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refreshStreak());
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.closeMobileMenu();
    this.dismissCelebration();
  }

  protected toggleTheme(): void {
    const next = !this.darkMode();
    this.darkMode.set(next);
    this.applyTheme(next);
    localStorage.setItem('pht_theme', next ? 'dark' : 'light');
  }

  protected streakLabel(): string {
    const streak = this.streak();
    if (!streak) {
      return '0 jour';
    }
    return `${streak.currentStreak} jour${streak.currentStreak > 1 ? 's' : ''}`;
  }

  protected streakBestLabel(): string {
    const streak = this.streak();
    if (!streak) {
      return 'Record 0 j';
    }
    return `Record ${streak.longestStreak} j`;
  }

  protected logout(): void {
    if (this.loggingOut()) return;
    this.loggingOut.set(true);
    this.http.post('/api/v1/auth/logout', {}, { withCredentials: true }).subscribe({
      next: () => {
        this.realtimeSyncService.stop();
        this.authService.clearAccessToken();
        this.loggingOut.set(false);
        void this.router.navigateByUrl('/login');
      },
      error: () => {
        this.realtimeSyncService.stop();
        this.authService.clearAccessToken();
        this.loggingOut.set(false);
        void this.router.navigateByUrl('/login');
      }
    });
  }

  protected toggleMobileMenu(): void {
    this.mobileNavOpen.update((open) => !open);
  }

  protected closeMobileMenu(): void {
    this.mobileNavOpen.set(false);
  }

  protected badgeLabel(badge: string): string {
    switch (badge) {
      case 'STREAK_3': return '3 jours';
      case 'STREAK_7': return '7 jours';
      case 'STREAK_14': return '14 jours';
      case 'STREAK_30': return '30 jours';
      case 'STREAK_60': return '60 jours';
      case 'STREAK_100': return '100 jours';
      case 'STREAK_365': return '365 jours';
      default: return badge;
    }
  }

  protected dismissCelebration(): void {
    this.streakService.dismissCelebration();
    if (this.celebrationTimer) {
      clearTimeout(this.celebrationTimer);
      this.celebrationTimer = null;
    }
  }

  ngOnDestroy(): void {
    this.networkStatusService.stop();
    this.realtimeSyncService.stop();
    this.themeMediaQuery?.removeEventListener('change', this.onThemePreferenceChange);
    if (this.celebrationTimer) {
      clearTimeout(this.celebrationTimer);
    }
    document.body.classList.remove('has-shell');
  }

  private applyTheme(dark: boolean): void {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
  }

  private refreshStreak(): void {
    this.todayApi.getToday().subscribe({
      next: ({ data }) => {
        this.streakService.set(data.streak);
        this.badgeService.update(data.activeCount);
      },
      error: () => {}
    });
  }
}
