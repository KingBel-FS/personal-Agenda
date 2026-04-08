import { Component, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DayIndicatorComponent } from './day-indicator.component';
import { AgendaApiService, type AgendaDaySummary, type AgendaRangeResponse } from './agenda-api.service';
import { TodayApiService, type TodayOccurrenceItem, type TodayResponse } from '../today/today-api.service';
import { RealtimeSyncService } from '../../core/realtime-sync.service';
import { StreakService } from '../../core/streak.service';

type AgendaView = 'week' | 'month';

@Component({
  selector: 'app-agenda-page',
  imports: [DayIndicatorComponent, FormsModule],
  templateUrl: './agenda-page.component.html',
  styleUrl: './agenda-page.component.scss'
})
export class AgendaPageComponent {
  private readonly agendaApi = inject(AgendaApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly realtimeSyncService = inject(RealtimeSyncService);
  private readonly todayApi = inject(TodayApiService);
  private readonly streakService = inject(StreakService);
  private readonly router = inject(Router);

  protected readonly loading = signal(true);
  protected readonly detailLoading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly detailErrorMessage = signal('');
  protected readonly viewMode = signal<AgendaView>('month');
  protected readonly range = signal<AgendaRangeResponse | null>(null);
  protected readonly detail = signal<TodayResponse | null>(null);
  protected readonly selectedDate = signal(this.todayIso());
  protected readonly toastMessage = signal('');
  protected readonly toastType = signal<'success' | 'error'>('success');
  protected readonly contextMenuOpen = signal(false);
  protected readonly contextTarget = signal<TodayOccurrenceItem | null>(null);
  protected readonly detailSheetOpen = signal(false);
  protected readonly detailTarget = signal<TodayOccurrenceItem | null>(null);
  protected readonly deleteSheetOpen = signal(false);
  protected readonly deleteTarget = signal<TodayOccurrenceItem | null>(null);
  protected readonly editTimeSheetOpen = signal(false);
  protected readonly editTimeTarget = signal<TodayOccurrenceItem | null>(null);
  protected readonly editTimeValue = signal('');
  protected readonly rescheduleSheetOpen = signal(false);
  protected readonly rescheduleTarget = signal<TodayOccurrenceItem | null>(null);
  protected readonly rescheduleDate = signal('');
  protected readonly rescheduleTime = signal('');
  protected readonly busy = signal(false);
  private toastTimer: ReturnType<typeof setTimeout> | null = null;
  private pressTimer: ReturnType<typeof setTimeout> | null = null;
  private pressTriggered = false;

  constructor() {
    this.loadRange(this.selectedDate());
    this.realtimeSyncService.events$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadRange(this.selectedDate()));
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.closeContextMenu();
    this.closeDetailSheet();
    this.closeDeleteSheet();
    this.closeEditTimeSheet();
    this.closeRescheduleSheet();
  }

  protected occurrenceAriaLabel(occ: TodayOccurrenceItem): string {
    return `${occ.title}. ${occ.occurrenceTime}. ${this.detailStatusLabel(occ.status)}.`;
  }

  protected setViewMode(view: AgendaView): void {
    if (this.viewMode() === view) {
      return;
    }
    this.viewMode.set(view);
    this.loadRange(this.selectedDate());
  }

  protected previousRange(): void {
    this.loadRange(this.shiftAnchor(-1));
  }

  protected nextRange(): void {
    this.loadRange(this.shiftAnchor(1));
  }

  protected goToday(): void {
    const today = this.todayIso();
    this.selectedDate.set(today);
    this.loadRange(today);
  }

  protected selectDay(day: AgendaDaySummary): void {
    if (day.beforeAccountCreation) {
      return;
    }
    this.selectedDate.set(day.date);
    this.loadDetail(day.date);
  }

  protected openSelectedDay(): void {
    this.router.navigate(['/daily'], { queryParams: { date: this.selectedDate() } });
  }

  protected createTaskForSelectedDay(): void {
    this.router.navigate(['/tasks/new'], { queryParams: { date: this.selectedDate() } });
  }

  protected onOccurrencePointerDown(occ: TodayOccurrenceItem): void {
    if (this.selectedDaySummary()?.beforeAccountCreation || occ.status === 'skipped') {
      return;
    }
    this.pressTriggered = false;
    this.pressTimer = setTimeout(() => {
      this.pressTriggered = true;
      this.contextTarget.set(occ);
      this.contextMenuOpen.set(true);
    }, 500);
  }

  protected onOccurrencePointerUp(occ: TodayOccurrenceItem): void {
    if (this.pressTimer) {
      clearTimeout(this.pressTimer);
      this.pressTimer = null;
    }
    if (this.pressTriggered) {
      return;
    }
    this.detailTarget.set(occ);
    this.detailSheetOpen.set(true);
  }

  protected onOccurrencePointerLeave(): void {
    if (this.pressTimer) {
      clearTimeout(this.pressTimer);
      this.pressTimer = null;
    }
  }

  protected weeks(): AgendaDaySummary[][] {
    const days = this.range()?.days ?? [];
    const weeks: AgendaDaySummary[][] = [];
    for (let index = 0; index < days.length; index += 7) {
      weeks.push(days.slice(index, index + 7));
    }
    return weeks;
  }

  protected selectedDaySummary(): AgendaDaySummary | null {
    return this.range()?.days.find((day) => day.date === this.selectedDate()) ?? null;
  }

  protected isSelected(day: AgendaDaySummary): boolean {
    return this.selectedDate() === day.date;
  }

  protected onDayKeydown(event: KeyboardEvent, day: AgendaDaySummary): void {
    const direction = this.keyDirection(event.key);
    if (direction === null) {
      return;
    }
    event.preventDefault();

    const days = this.range()?.days ?? [];
    const currentIndex = days.findIndex((candidate) => candidate.date === day.date);
    const nextIndex = currentIndex + direction;
    if (currentIndex === -1 || nextIndex < 0 || nextIndex >= days.length) {
      return;
    }

    const nextDay = days[nextIndex];
    this.selectDay(nextDay);
    setTimeout(() => document.getElementById(this.dayButtonId(nextDay.date))?.focus());
  }

  protected rangeLabel(): string {
    const currentRange = this.range();
    if (!currentRange) {
      return '';
    }

    if (this.viewMode() === 'week') {
      return `${this.formatDate(currentRange.rangeStart, false)} au ${this.formatDate(currentRange.rangeEnd, true)}`;
    }

    return this.formatMonth(currentRange.anchorDate);
  }

  protected dayAriaLabel(day: AgendaDaySummary): string {
    if (day.beforeAccountCreation) {
      return `${this.formatDate(day.date, true)}. Jour indisponible avant la creation du compte.`;
    }
    const category = this.dayCategoryLabel(day.dayCategory);
    const countLabel = day.totalCount === 0 ? 'aucune occurrence' : `${day.totalCount} occurrence${day.totalCount > 1 ? 's' : ''}`;
    return `${this.formatDate(day.date, true)}. ${category}. ${countLabel}. Statut ${this.statusToneLabel(day.statusTone)}.`;
  }

  protected dayCategoryLabel(category: string): string {
    switch (category) {
      case 'WORKDAY':
        return 'Jour de travail';
      case 'VACATION':
        return 'Vacances';
      case 'WEEKEND_HOLIDAY':
        return 'Week-end ou f\u00e9ri\u00e9';
      default:
        return category;
    }
  }

  protected statusToneLabel(tone: string): string {
    switch (tone) {
      case 'done':
        return 'journ\u00e9e accomplie';
      case 'planned':
        return 'journ\u00e9e planifi\u00e9e';
      case 'missed':
        return 'journ\u00e9e manqu\u00e9e';
      case 'mixed':
        return 'journ\u00e9e mixte';
      case 'skipped':
        return 'journ\u00e9e ignor\u00e9e';
      default:
        return 'journ\u00e9e vide';
    }
  }

  protected detailStatusLabel(status: string): string {
    switch (status) {
      case 'done':
        return 'Termin\u00e9e';
      case 'missed':
        return 'Manqu\u00e9e';
      case 'skipped':
        return 'Ignor\u00e9e';
      default:
        return '\u00c0 faire';
    }
  }

  protected readonlyMessage(): string {
    return "Cette journ\u00e9e est pass\u00e9e. Les occurrences restent consultables et seules l'heure ainsi que le statut d'ex\u00e9cution restent modifiables.";
  }

  protected unavailableMessage(): string {
    return "Cette journ\u00e9e est ant\u00e9rieure \u00e0 la cr\u00e9ation du compte. Elle n'est pas accessible.";
  }

  protected dayButtonId(date: string): string {
    return `agenda-day-${date}`;
  }

  protected closeContextMenu(): void {
    this.contextMenuOpen.set(false);
    this.contextTarget.set(null);
  }

  protected closeDetailSheet(): void {
    this.detailSheetOpen.set(false);
    this.detailTarget.set(null);
  }

  protected closeDeleteSheet(): void {
    this.deleteSheetOpen.set(false);
    this.deleteTarget.set(null);
  }

  protected closeEditTimeSheet(): void {
    this.editTimeSheetOpen.set(false);
    this.editTimeTarget.set(null);
  }

  protected closeRescheduleSheet(): void {
    this.rescheduleSheetOpen.set(false);
    this.rescheduleTarget.set(null);
  }

  protected openManageEditor(occ: TodayOccurrenceItem, scope: 'THIS_OCCURRENCE' | 'THIS_AND_FOLLOWING'): void {
    this.closeContextMenu();
    this.closeDetailSheet();
    this.router.navigate(['/tasks/manage'], {
      queryParams: {
        occurrenceId: occ.id,
        action: 'edit',
        scope,
        date: this.detail()?.date ?? this.selectedDate()
      }
    });
  }

  protected contextAction(action: 'done' | 'missed' | 'delete' | 'reschedule' | 'revert' | 'edit-time'): void {
    const occ = this.contextTarget();
    this.closeContextMenu();
    if (!occ) {
      return;
    }
    if (action === 'done') {
      this.markDone(occ);
    } else if (action === 'missed') {
      this.markMissed(occ);
    } else if (action === 'delete') {
      this.deleteTarget.set(occ);
      this.deleteSheetOpen.set(true);
    } else if (action === 'reschedule') {
      this.rescheduleTarget.set(occ);
      this.rescheduleDate.set(this.detail()?.date ?? this.selectedDate());
      this.rescheduleTime.set(occ.occurrenceTime);
      this.rescheduleSheetOpen.set(true);
    } else if (action === 'edit-time') {
      this.openEditTimeSheet(occ);
    } else {
      this.revertToPlanned(occ);
    }
  }

  protected confirmDelete(): void {
    const occ = this.deleteTarget();
    this.closeDeleteSheet();
    if (!occ) {
      return;
    }
    this.busy.set(true);
    this.todayApi.cancel(occ.id).subscribe({
      next: () => this.refreshAgenda('Occurrence supprimee.', 'success'),
      error: (error) => this.handleActionError(error, 'Impossible de supprimer cette occurrence.')
    });
  }

  protected confirmReschedule(): void {
    const occ = this.rescheduleTarget();
    const date = this.rescheduleDate();
    const time = this.rescheduleTime();
    this.closeRescheduleSheet();
    if (!occ || !date || !time) {
      return;
    }
    this.busy.set(true);
    this.todayApi.reschedule(occ.id, date, time).subscribe({
      next: () => this.refreshAgenda('Occurrence decalee.', 'success'),
      error: (error) => this.handleActionError(error, 'Impossible de decaler cette occurrence.')
    });
  }

  protected detailAction(action: 'done' | 'missed' | 'revert'): void {
    const occ = this.detailTarget();
    this.closeDetailSheet();
    if (!occ) {
      return;
    }
    if (action === 'done') {
      this.markDone(occ);
    } else if (action === 'missed') {
      this.markMissed(occ);
    } else {
      this.revertToPlanned(occ);
    }
  }

  protected editTimeFromDetail(): void {
    const occ = this.detailTarget();
    this.closeDetailSheet();
    if (!occ) {
      return;
    }
    this.openEditTimeSheet(occ);
  }

  protected rescheduleFromDetail(): void {
    const occ = this.detailTarget();
    this.closeDetailSheet();
    if (!occ) {
      return;
    }
    this.rescheduleTarget.set(occ);
    this.rescheduleDate.set(this.detail()?.date ?? this.selectedDate());
    this.rescheduleTime.set(occ.occurrenceTime);
    this.rescheduleSheetOpen.set(true);
  }

  protected dismissToast(): void {
    this.toastMessage.set('');
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
  }

  private loadRange(anchorDate: string): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const request = this.viewMode() === 'week'
      ? this.agendaApi.getWeek(anchorDate)
      : this.agendaApi.getMonth(anchorDate);

    request.subscribe({
      next: ({ data }) => {
        this.range.set(data);
        const nextSelected = data.days.some((day) => day.date === this.selectedDate())
          ? this.selectedDate()
          : anchorDate;
        this.selectedDate.set(nextSelected);
        this.loading.set(false);
        this.loadDetail(nextSelected);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message ?? "Impossible de charger l'agenda.");
        this.loading.set(false);
      }
    });
  }

  private loadDetail(date: string): void {
    this.detailLoading.set(true);
    this.detailErrorMessage.set('');
    this.todayApi.getDaily(date).subscribe({
      next: ({ data }) => {
        this.detail.set(data);
        this.streakService.set(data.streak);
        this.detailLoading.set(false);
      },
      error: (error) => {
        this.detailErrorMessage.set(error?.error?.error?.message ?? 'Impossible de charger la journ\u00e9e.');
        this.detailLoading.set(false);
      }
    });
  }

  private markDone(occ: TodayOccurrenceItem): void {
    this.busy.set(true);
    this.todayApi.complete(occ.id).subscribe({
      next: () => this.refreshAgenda('Occurrence termin\u00e9e.', 'success'),
      error: (error) => this.handleActionError(error, 'Impossible de marquer cette occurrence.')
    });
  }

  private markMissed(occ: TodayOccurrenceItem): void {
    this.busy.set(true);
    this.todayApi.miss(occ.id).subscribe({
      next: () => this.refreshAgenda('Occurrence manqu\u00e9e.', 'success'),
      error: (error) => this.handleActionError(error, 'Impossible de marquer cette occurrence.')
    });
  }

  private revertToPlanned(occ: TodayOccurrenceItem): void {
    this.busy.set(true);
    this.todayApi.revertToPlanned(occ.id).subscribe({
      next: () => this.refreshAgenda('Occurrence remise \u00e0 faire.', 'success'),
      error: (error) => this.handleActionError(error, 'Impossible de remettre cette occurrence \u00e0 faire.')
    });
  }

  private refreshAgenda(message: string, type: 'success' | 'error'): void {
    this.showToast(message, type);
    this.busy.set(false);
    this.loadRange(this.selectedDate());
  }

  private openEditTimeSheet(occ: TodayOccurrenceItem): void {
    this.editTimeTarget.set(occ);
    this.editTimeValue.set(occ.occurrenceTime);
    this.editTimeSheetOpen.set(true);
  }

  protected confirmEditTime(): void {
    const occ = this.editTimeTarget();
    const time = this.editTimeValue();
    this.closeEditTimeSheet();
    if (!occ || !time) {
      return;
    }
    this.busy.set(true);
    this.todayApi.editTime(occ.id, time).subscribe({
      next: () => this.refreshAgenda('Heure mise a jour.', 'success'),
      error: (error) => this.handleActionError(error, "Impossible de modifier l'heure de cette occurrence.")
    });
  }

  private handleActionError(error: unknown, fallbackMessage: string): void {
    const message = (error as { error?: { error?: { message?: string } } })?.error?.error?.message ?? fallbackMessage;
    this.showToast(message, 'error');
    this.busy.set(false);
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toastMessage.set(message);
    this.toastType.set(type);
    this.toastTimer = setTimeout(() => this.dismissToast(), 3500);
  }

  private shiftAnchor(direction: -1 | 1): string {
    const anchor = this.parseIsoDate(this.range()?.anchorDate ?? this.selectedDate());
    if (this.viewMode() === 'week') {
      anchor.setDate(anchor.getDate() + (direction * 7));
    } else {
      anchor.setMonth(anchor.getMonth() + direction);
    }
    return this.toIsoDate(anchor);
  }

  private keyDirection(key: string): number | null {
    switch (key) {
      case 'ArrowRight':
        return 1;
      case 'ArrowLeft':
        return -1;
      case 'ArrowDown':
        return 7;
      case 'ArrowUp':
        return -7;
      default:
        return null;
    }
  }

  private formatMonth(iso: string): string {
    return this.parseIsoDate(iso).toLocaleDateString('fr-FR', {
      month: 'long',
      year: 'numeric'
    });
  }

  private formatDate(iso: string, withYear: boolean): string {
    return this.parseIsoDate(iso).toLocaleDateString('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      ...(withYear ? { year: 'numeric' } : {})
    });
  }

  private todayIso(): string {
    return this.toIsoDate(new Date());
  }

  private parseIsoDate(value: string): Date {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
