import { Component, DestroyRef, HostListener, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TodayApiService, type TodayOccurrenceItem, type TodayResponse } from './today-api.service';
import { BadgeService } from '../../core/badge.service';
import { RealtimeSyncService } from '../../core/realtime-sync.service';
import { StreakService } from '../../core/streak.service';
import { DayContextHeaderComponent } from '../../shared/components/day-context-header.component';
import { TaskCardComponent } from '../../shared/components/task-card.component';

@Component({
  selector: 'app-today-page',
  imports: [FormsModule, DayContextHeaderComponent, TaskCardComponent],
  templateUrl: './today-page.component.html',
  styleUrl: './today-page.component.scss'
})
export class TodayPageComponent implements OnInit {
  private readonly todayApi = inject(TodayApiService);
  private readonly badge = inject(BadgeService);
  private readonly realtimeSyncService = inject(RealtimeSyncService);
  private readonly streakService = inject(StreakService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly today = signal<TodayResponse | null>(null);

  // Toast
  protected readonly toastMessage = signal('');
  protected readonly toastType = signal<'success' | 'error'>('success');
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  // Context menu (long press)
  protected readonly contextMenuOpen = signal(false);
  protected readonly contextTarget = signal<TodayOccurrenceItem | null>(null);

  // Edit description sheet
  protected readonly editSheetOpen = signal(false);
  protected readonly editTarget = signal<TodayOccurrenceItem | null>(null);
  protected readonly editDescriptionValue = signal('');

  // Delete confirmation sheet
  protected readonly deleteSheetOpen = signal(false);
  protected readonly deleteTarget = signal<TodayOccurrenceItem | null>(null);

  // Reschedule sheet (one-time tasks only)
  protected readonly rescheduleSheetOpen = signal(false);
  protected readonly rescheduleTarget = signal<TodayOccurrenceItem | null>(null);
  protected readonly rescheduleDate = signal('');
  protected readonly rescheduleTime = signal('');

  // Detail view sheet
  protected readonly detailSheetOpen = signal(false);
  protected readonly detailTarget = signal<TodayOccurrenceItem | null>(null);

  // Busy guard (prevent double-tap)
  protected readonly busy = signal(false);

  // Long-press detection
  private pressTimer: ReturnType<typeof setTimeout> | null = null;
  private pressTriggered = false;

  private readonly onVisibilityChange = () => {
    if (document.visibilityState === 'visible' && !this.loading()) {
      this.refresh();
    }
  };

  constructor() {
    this.load();
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.closeContextMenu();
    this.closeEditSheet();
    this.closeDeleteSheet();
    this.closeRescheduleSheet();
    this.closeDetailSheet();
  }

  ngOnInit(): void {
    document.addEventListener('visibilitychange', this.onVisibilityChange);
    this.destroyRef.onDestroy(() => {
      document.removeEventListener('visibilitychange', this.onVisibilityChange);
    });

    // Check for push action feedback (from service worker notification click)
    this.route.queryParams.subscribe(params => {
      const pushAction = params['pushAction'];
      if (pushAction === 'DONE') {
        this.showToast('Tâche marquée terminée depuis la notification.', 'success');
        this.router.navigate([], { queryParams: {}, replaceUrl: true });
        this.refresh();
      } else if (pushAction === 'MISSED') {
        this.showToast('Tâche marquée manquée depuis la notification.', 'success');
        this.router.navigate([], { queryParams: {}, replaceUrl: true });
        this.refresh();
      }
    });

    this.realtimeSyncService.events$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refresh());
  }

  // ── Data helpers ──────────────────────────────────────────

  protected dayCategoryLabel(category: string): string {
    switch (category) {
      case 'WORKDAY':         return 'Jour de travail';
      case 'VACATION':        return 'Vacances';
      case 'WEEKEND_HOLIDAY': return 'Week-end / Jour férié';
      default:                return category;
    }
  }

  protected formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long'
    });
  }

  protected dayVariant(category: string): 'workday' | 'vacation' | 'weekend-holiday' {
    switch (category) {
      case 'WORKDAY':
        return 'workday';
      case 'VACATION':
        return 'vacation';
      default:
        return 'weekend-holiday';
    }
  }

  protected statusLabel(status: string): string {
    switch (status) {
      case 'done':      return 'Terminée';
      case 'missed':    return 'Manquée';
      case 'skipped':   return 'Ignorée';
      default:          return 'À faire';
    }
  }

  protected statusIcon(status: string): string {
    switch (status) {
      case 'done':      return '✓';
      case 'missed':    return '✗';
      case 'skipped':   return '—';
      default:          return '';
    }
  }

  // ── Gesture handlers ──────────────────────────────────────

  protected onPointerDown(occ: TodayOccurrenceItem): void {
    if (occ.status === 'skipped') return;
    this.pressTriggered = false;
    this.pressTimer = setTimeout(() => {
      this.pressTriggered = true;
      this.openContextMenu(occ);
    }, 500);
  }

  protected onPointerUp(occ: TodayOccurrenceItem): void {
    if (this.pressTimer) {
      clearTimeout(this.pressTimer);
      this.pressTimer = null;
    }
    if (this.pressTriggered) return;
    this.onTap(occ);
  }

  protected onPointerLeave(): void {
    if (this.pressTimer) {
      clearTimeout(this.pressTimer);
      this.pressTimer = null;
    }
  }

  private onTap(occ: TodayOccurrenceItem): void {
    if (this.busy() || occ.status === 'skipped') return;
    this.openDetailSheet(occ);
  }

  // ── Context menu ──────────────────────────────────────────

  private openContextMenu(occ: TodayOccurrenceItem): void {
    this.contextTarget.set(occ);
    this.contextMenuOpen.set(true);
  }

  protected closeContextMenu(): void {
    this.contextMenuOpen.set(false);
    this.contextTarget.set(null);
  }

  protected contextAction(action: 'done' | 'missed' | 'edit' | 'edit-series' | 'delete' | 'reschedule' | 'revert'): void {
    const occ = this.contextTarget();
    this.closeContextMenu();
    if (!occ) return;
    switch (action) {
      case 'done':       this.markDone(occ); break;
      case 'missed':     this.markMissed(occ); break;
      case 'edit':       this.openEditSheet(occ); break;
      case 'edit-series': this.openManageEditor(occ, 'THIS_AND_FOLLOWING'); break;
      case 'delete':     this.openDeleteSheet(occ); break;
      case 'reschedule': this.openRescheduleSheet(occ); break;
      case 'revert':     this.revertToPlanned(occ); break;
    }
  }

  // ── Status mutations ──────────────────────────────────────

  private markDone(occ: TodayOccurrenceItem): void {
    this.busy.set(true);
    this.todayApi.complete(occ.id).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('« ' + occ.title + ' » terminée.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors du marquage.', 'error');
        this.busy.set(false);
      }
    });
  }

  private markMissed(occ: TodayOccurrenceItem): void {
    this.busy.set(true);
    this.todayApi.miss(occ.id).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('Statut mis à jour : « ' + occ.title + ' » manquée.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors du marquage.', 'error');
        this.busy.set(false);
      }
    });
  }

  // ── Edit description ──────────────────────────────────────

  private openEditSheet(occ: TodayOccurrenceItem): void {
    this.editTarget.set(occ);
    this.editDescriptionValue.set(occ.description ?? '');
    this.editSheetOpen.set(true);
  }

  protected closeEditSheet(): void {
    this.editSheetOpen.set(false);
    this.editTarget.set(null);
  }

  protected confirmEditDescription(): void {
    const occ = this.editTarget();
    const desc = this.editDescriptionValue();
    this.closeEditSheet();
    if (!occ) return;
    this.busy.set(true);
    this.todayApi.editDescription(occ.id, desc).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('Description mise à jour.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors de la modification.', 'error');
        this.busy.set(false);
      }
    });
  }

  // ── Detail view ───────────────────────────────────────────

  private openDetailSheet(occ: TodayOccurrenceItem): void {
    this.detailTarget.set(occ);
    this.detailSheetOpen.set(true);
  }

  protected openManageEditor(occ: TodayOccurrenceItem, scope: 'THIS_OCCURRENCE' | 'THIS_AND_FOLLOWING'): void {
    this.router.navigate(['/tasks/manage'], {
      queryParams: {
        occurrenceId: occ.id,
        action: 'edit',
        scope,
        date: this.today()?.date
      }
    });
  }

  protected openCreateTask(): void {
    const date = this.today()?.date;
    void this.router.navigate(['/tasks/new'], {
      queryParams: date ? { date } : undefined
    });
  }

  protected closeDetailSheet(): void {
    this.detailSheetOpen.set(false);
    this.detailTarget.set(null);
  }

  protected detailAction(action: 'done' | 'missed' | 'revert'): void {
    const occ = this.detailTarget();
    this.closeDetailSheet();
    if (!occ) return;
    if (action === 'done') this.markDone(occ);
    else if (action === 'missed') this.markMissed(occ);
    else this.revertToPlanned(occ);
  }

  // ── Delete (cancel) occurrence ────────────────────────────

  private openDeleteSheet(occ: TodayOccurrenceItem): void {
    this.deleteTarget.set(occ);
    this.deleteSheetOpen.set(true);
  }

  protected closeDeleteSheet(): void {
    this.deleteSheetOpen.set(false);
    this.deleteTarget.set(null);
  }

  protected confirmDelete(): void {
    const occ = this.deleteTarget();
    this.closeDeleteSheet();
    if (!occ) return;
    this.busy.set(true);
    this.todayApi.cancel(occ.id).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('« ' + occ.title + ' » supprimée pour aujourd\'hui.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors de la suppression.', 'error');
        this.busy.set(false);
      }
    });
  }

  // ── Reschedule (one-time only) ───────────────────────────

  private openRescheduleSheet(occ: TodayOccurrenceItem): void {
    this.rescheduleTarget.set(occ);
    this.rescheduleDate.set(this.today()?.date ?? '');
    this.rescheduleTime.set(occ.occurrenceTime);
    this.rescheduleSheetOpen.set(true);
  }

  protected closeRescheduleSheet(): void {
    this.rescheduleSheetOpen.set(false);
    this.rescheduleTarget.set(null);
  }

  protected confirmReschedule(): void {
    const occ = this.rescheduleTarget();
    const date = this.rescheduleDate();
    const time = this.rescheduleTime();
    this.closeRescheduleSheet();
    if (!occ || !date || !time) return;
    this.busy.set(true);
    this.todayApi.reschedule(occ.id, date, time).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('« ' + occ.title + ' » décalée au ' + date + ' à ' + time + '.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors du décalage.', 'error');
        this.busy.set(false);
      }
    });
  }

  // ── Revert to planned ──────────────────────────────────

  private revertToPlanned(occ: TodayOccurrenceItem): void {
    this.busy.set(true);
    this.todayApi.revertToPlanned(occ.id).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('« ' + occ.title + ' » remise à faire.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors de la remise à faire.', 'error');
        this.busy.set(false);
      }
    });
  }

  // ── Toast ─────────────────────────────────────────────────

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage.set(message);
    this.toastType.set(type);
    this.toastTimer = setTimeout(() => this.dismissToast(), 3500);
  }

  protected dismissToast(): void {
    this.toastMessage.set('');
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
  }

  // ── Data helpers ───────────────────────────────────────────

  private applyData(data: TodayResponse): void {
    this.today.set(data);
    this.badge.update(data.activeCount);
    this.streakService.set(data.streak);

    if (data.newBadges && data.newBadges.length > 0) {
      this.streakService.celebrate(data.newBadges[0]);
    }
  }

  // ── Data loading ──────────────────────────────────────────

  private load(): void {
    this.todayApi.getToday().subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error?.message ?? 'Impossible de charger la vue du jour.');
        this.loading.set(false);
      }
    });
  }

  /** Silent refetch when tab regains focus — no loading spinner. */
  private refresh(): void {
    this.todayApi.getToday().subscribe({
      next: ({ data }) => this.applyData(data),
      error: () => {}
    });
  }
}
