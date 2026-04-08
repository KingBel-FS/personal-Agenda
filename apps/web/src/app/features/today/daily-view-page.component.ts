import { Component, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LowerCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TodayApiService, type TodayOccurrenceItem, type TodayResponse } from './today-api.service';
import { WakeUpOverrideApiService, type WakeUpOverride } from './wake-up-override-api.service';
import { RealtimeSyncService } from '../../core/realtime-sync.service';
import { StreakService } from '../../core/streak.service';

@Component({
  selector: 'app-daily-view-page',
  imports: [LowerCasePipe, FormsModule],
  templateUrl: './daily-view-page.component.html',
  styleUrl: './daily-view-page.component.scss'
})
export class DailyViewPageComponent {
  private readonly todayApi = inject(TodayApiService);
  private readonly wakeUpApi = inject(WakeUpOverrideApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly realtimeSyncService = inject(RealtimeSyncService);
  private readonly streakService = inject(StreakService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly data = signal<TodayResponse | null>(null);
  protected readonly selectedDate = signal(this.todayISO());

  protected readonly toastMessage = signal('');
  protected readonly toastType = signal<'success' | 'error'>('success');
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly contextMenuOpen = signal(false);
  protected readonly contextTarget = signal<TodayOccurrenceItem | null>(null);

  protected readonly editSheetOpen = signal(false);
  protected readonly editTarget = signal<TodayOccurrenceItem | null>(null);
  protected readonly editDescriptionValue = signal('');
  protected readonly editTimeSheetOpen = signal(false);
  protected readonly editTimeTarget = signal<TodayOccurrenceItem | null>(null);
  protected readonly editTimeValue = signal('');

  protected readonly deleteSheetOpen = signal(false);
  protected readonly deleteTarget = signal<TodayOccurrenceItem | null>(null);

  protected readonly rescheduleSheetOpen = signal(false);
  protected readonly rescheduleTarget = signal<TodayOccurrenceItem | null>(null);
  protected readonly rescheduleDate = signal('');
  protected readonly rescheduleTime = signal('');

  protected readonly detailSheetOpen = signal(false);
  protected readonly detailTarget = signal<TodayOccurrenceItem | null>(null);

  protected readonly wakeUpSheetOpen = signal(false);
  protected readonly wakeUpOverride = signal<WakeUpOverride | null>(null);
  protected readonly wakeUpTimeValue = signal('');

  protected readonly busy = signal(false);

  private pressTimer: ReturnType<typeof setTimeout> | null = null;
  private pressTriggered = false;

  constructor() {
    const requestedDate = this.route.snapshot.queryParamMap.get('date');
    if (requestedDate) {
      this.selectedDate.set(requestedDate);
    }
    this.load(this.selectedDate());
    this.realtimeSyncService.events$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load(this.selectedDate()));
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.closeContextMenu();
    this.closeEditSheet();
    this.closeEditTimeSheet();
    this.closeDeleteSheet();
    this.closeRescheduleSheet();
    this.closeDetailSheet();
    this.closeWakeUpSheet();
  }

  protected onDateChange(date: string): void {
    if (!date) return;
    this.selectedDate.set(date);
    this.syncQueryDate(date);
    this.load(date);
  }

  protected prevDay(): void {
    const d = new Date(this.selectedDate());
    d.setDate(d.getDate() - 1);
    const iso = this.toISO(d);
    this.selectedDate.set(iso);
    this.syncQueryDate(iso);
    this.load(iso);
  }

  protected nextDay(): void {
    const d = new Date(this.selectedDate());
    d.setDate(d.getDate() + 1);
    const iso = this.toISO(d);
    this.selectedDate.set(iso);
    this.syncQueryDate(iso);
    this.load(iso);
  }

  protected goToday(): void {
    const iso = this.todayISO();
    this.selectedDate.set(iso);
    this.syncQueryDate(iso);
    this.load(iso);
  }

  protected isToday(): boolean {
    return this.selectedDate() === this.todayISO();
  }

  protected createTaskForSelectedDay(): void {
    this.router.navigate(['/tasks/new'], { queryParams: { date: this.selectedDate() } });
  }

  protected dayCategoryLabel(category: string): string {
    switch (category) {
      case 'WORKDAY': return 'Jour de travail';
      case 'VACATION': return 'Vacances';
      case 'WEEKEND_HOLIDAY': return 'Week-end / Jour f\u00e9ri\u00e9';
      default: return category;
    }
  }

  protected formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  }

  protected statusLabel(status: string): string {
    switch (status) {
      case 'done': return 'Termin\u00e9e';
      case 'missed': return 'Manqu\u00e9e';
      case 'skipped': return 'Ignor\u00e9e';
      default: return '\u00c0 faire';
    }
  }

  protected statusIcon(status: string): string {
    switch (status) {
      case 'done': return '✓';
      case 'missed': return '✕';
      case 'skipped': return '—';
      default: return '';
    }
  }

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

  protected closeContextMenu(): void {
    this.contextMenuOpen.set(false);
    this.contextTarget.set(null);
  }

  protected contextAction(action: 'done' | 'missed' | 'edit' | 'edit-time' | 'edit-series' | 'delete' | 'reschedule' | 'revert'): void {
    const occ = this.contextTarget();
    this.closeContextMenu();
    if (!occ) return;
    switch (action) {
      case 'done':
        this.markDone(occ);
        break;
      case 'missed':
        this.markMissed(occ);
        break;
      case 'edit':
        this.openEditSheet(occ);
        break;
      case 'edit-time':
        this.openEditTimeSheet(occ);
        break;
      case 'edit-series':
        this.openManageEditor(occ, 'THIS_AND_FOLLOWING');
        break;
      case 'delete':
        this.openDeleteSheet(occ);
        break;
      case 'reschedule':
        this.openRescheduleSheet(occ);
        break;
      case 'revert':
        this.revertToPlanned(occ);
        break;
    }
  }

  protected canEditPastOccurrence(): boolean {
    return !!this.data() && this.data()!.date < this.todayISO();
  }

  protected closeEditSheet(): void {
    this.editSheetOpen.set(false);
    this.editTarget.set(null);
  }

  protected confirmEditDescription(): void {
    const occ = this.editTarget();
    const description = this.editDescriptionValue();
    this.closeEditSheet();
    if (!occ) return;
    this.busy.set(true);
    this.todayApi.editDescription(occ.id, description).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('Description mise \u00e0 jour.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors de la modification.', 'error');
        this.busy.set(false);
      }
    });
  }

  protected closeDeleteSheet(): void {
    this.deleteSheetOpen.set(false);
    this.deleteTarget.set(null);
  }

  protected closeEditTimeSheet(): void {
    this.editTimeSheetOpen.set(false);
    this.editTimeTarget.set(null);
  }

  protected confirmEditTime(): void {
    const occ = this.editTimeTarget();
    const time = this.editTimeValue();
    this.closeEditTimeSheet();
    if (!occ || !time) return;
    this.busy.set(true);
    this.todayApi.editTime(occ.id, time).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('Heure mise à jour.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? "Erreur lors de la modification de l'heure.", 'error');
        this.busy.set(false);
      }
    });
  }

  protected confirmDelete(): void {
    const occ = this.deleteTarget();
    this.closeDeleteSheet();
    if (!occ) return;
    this.busy.set(true);
    this.todayApi.cancel(occ.id).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('Occurrence supprim\u00e9e pour cette journ\u00e9e.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors de la suppression.', 'error');
        this.busy.set(false);
      }
    });
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
        this.showToast('T\u00e2che d\u00e9cal\u00e9e au ' + date + ' \u00e0 ' + time + '.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors du decalage.', 'error');
        this.busy.set(false);
      }
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
    if (action === 'done') {
      this.markDone(occ);
    } else if (action === 'missed') {
      this.markMissed(occ);
    } else {
      this.revertToPlanned(occ);
    }
  }

  protected dismissToast(): void {
    this.toastMessage.set('');
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
  }

  protected openManageEditor(occ: TodayOccurrenceItem, scope: 'THIS_OCCURRENCE' | 'THIS_AND_FOLLOWING'): void {
    this.router.navigate(['/tasks/manage'], {
      queryParams: {
        occurrenceId: occ.id,
        action: 'edit',
        scope,
        date: this.data()?.date ?? this.selectedDate()
      }
    });
  }

  protected rescheduleFromDetail(): void {
    const occ = this.detailTarget();
    this.closeDetailSheet();
    if (!occ) return;
    this.openRescheduleSheet(occ);
  }

  private onTap(occ: TodayOccurrenceItem): void {
    if (this.busy() || occ.status === 'skipped') return;
    this.openDetailSheet(occ);
  }

  private openContextMenu(occ: TodayOccurrenceItem): void {
    this.contextTarget.set(occ);
    this.contextMenuOpen.set(true);
  }

  private markDone(occ: TodayOccurrenceItem): void {
    this.busy.set(true);
    this.todayApi.complete(occ.id).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('T\u00e2che marqu\u00e9e termin\u00e9e.', 'success');
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
        this.showToast('T\u00e2che marqu\u00e9e manqu\u00e9e.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors du marquage.', 'error');
        this.busy.set(false);
      }
    });
  }

  private openEditSheet(occ: TodayOccurrenceItem): void {
    this.editTarget.set(occ);
    this.editDescriptionValue.set(occ.description ?? '');
    this.editSheetOpen.set(true);
  }

  private openDeleteSheet(occ: TodayOccurrenceItem): void {
    this.deleteTarget.set(occ);
    this.deleteSheetOpen.set(true);
  }

  protected openEditTimeSheet(occ: TodayOccurrenceItem): void {
    this.editTimeTarget.set(occ);
    this.editTimeValue.set(occ.occurrenceTime);
    this.editTimeSheetOpen.set(true);
  }

  private openRescheduleSheet(occ: TodayOccurrenceItem): void {
    this.rescheduleTarget.set(occ);
    this.rescheduleDate.set(this.data()?.date ?? this.selectedDate());
    this.rescheduleTime.set(occ.occurrenceTime);
    this.rescheduleSheetOpen.set(true);
  }

  private openDetailSheet(occ: TodayOccurrenceItem): void {
    this.detailTarget.set(occ);
    this.detailSheetOpen.set(true);
  }

  private revertToPlanned(occ: TodayOccurrenceItem): void {
    this.busy.set(true);
    this.todayApi.revertToPlanned(occ.id).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.showToast('Occurrence remise \u00e0 faire.', 'success');
        this.busy.set(false);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors de la remise \u00e0 faire.', 'error');
        this.busy.set(false);
      }
    });
  }

  protected openWakeUpSheet(): void {
    const existing = this.wakeUpOverride();
    this.wakeUpTimeValue.set(existing?.wakeUpTime?.substring(0, 5) ?? '07:00');
    this.wakeUpSheetOpen.set(true);
  }

  protected closeWakeUpSheet(): void {
    this.wakeUpSheetOpen.set(false);
  }

  protected confirmWakeUpOverride(): void {
    const time = this.wakeUpTimeValue();
    const date = this.selectedDate();
    this.closeWakeUpSheet();
    if (!time) return;
    this.busy.set(true);
    this.wakeUpApi.upsertOverride(date, time).subscribe({
      next: ({ data }) => {
        this.wakeUpOverride.set(data);
        this.busy.set(false);
        this.showToast('Heure de réveil mise à jour.', 'success');
        this.load(date);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors de la modification du réveil.', 'error');
        this.busy.set(false);
      }
    });
  }

  protected deleteWakeUpOverride(): void {
    const date = this.selectedDate();
    this.closeWakeUpSheet();
    this.busy.set(true);
    this.wakeUpApi.deleteOverride(date).subscribe({
      next: () => {
        this.wakeUpOverride.set(null);
        this.busy.set(false);
        this.showToast('Réveil remis par défaut.', 'success');
        this.load(date);
      },
      error: (err) => {
        this.showToast(err?.error?.error?.message ?? 'Erreur lors de la suppression du réveil.', 'error');
        this.busy.set(false);
      }
    });
  }

  protected isPastDate(): boolean {
    return this.selectedDate() < this.todayISO();
  }

  private loadWakeUpOverride(date: string): void {
    this.wakeUpApi.getOverride(date).subscribe({
      next: ({ data }) => this.wakeUpOverride.set(data),
      error: () => this.wakeUpOverride.set(null)
    });
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toastMessage.set(message);
    this.toastType.set(type);
    this.toastTimer = setTimeout(() => this.dismissToast(), 3500);
  }

  private applyData(data: TodayResponse): void {
    this.data.set(data);
    this.streakService.set(data.streak);
  }

  private load(date: string): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.loadWakeUpOverride(date);
    this.todayApi.getDaily(date).subscribe({
      next: ({ data }) => {
        this.applyData(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error?.message ?? 'Impossible de charger cette journ\u00e9e.');
        this.loading.set(false);
      }
    });
  }

  private todayISO(): string {
    return this.toISO(new Date());
  }

  private toISO(d: Date): string {
    return d.getFullYear() + '-' +
      String(d.getMonth() + 1).padStart(2, '0') + '-' +
      String(d.getDate()).padStart(2, '0');
  }

  private syncQueryDate(date: string): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { date },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }
}
