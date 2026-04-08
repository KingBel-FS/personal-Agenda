import { DatePipe } from '@angular/common';
import { Component, HostListener, inject, signal } from '@angular/core';
import { StatsApiService, type StatsDashboardResponse, type StatsHistoryPoint, type StatsPeriod, type StatsTaskDetail, type StatsTaskSummary, type StatsSnapshot } from './stats-api.service';
import { StreakService } from '../../core/streak.service';
import { StreakFlameComponent } from '../../shared/components/streak-flame.component';

type PeriodType = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

@Component({
  selector: 'app-stats-page',
  imports: [DatePipe, StreakFlameComponent],
  templateUrl: './stats-page.component.html',
  styleUrl: './stats-page.component.scss'
})
export class StatsPageComponent {
  private readonly statsApi = inject(StatsApiService);
  private readonly streakService = inject(StreakService);
  private readonly detailHistoryWindowSize = 3;

  protected readonly loading = signal(true);
  protected readonly detailLoading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly dashboard = signal<StatsDashboardResponse | null>(null);
  protected readonly streak = this.streakService.streak;
  protected readonly selectedTaskDetail = signal<StatsTaskDetail | null>(null);
  protected readonly selectedPeriodDetail = signal<StatsPeriod | null>(null);
  protected readonly detailHistoryStart = signal(0);
  protected readonly dailyAnchor = signal(this.todayIso());
  protected readonly weeklyAnchor = signal(this.todayIso());
  protected readonly monthlyAnchor = signal(this.todayIso());
  protected readonly yearlyAnchor = signal(this.todayIso());

  constructor() {
    this.loadDashboard();
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.closeTaskDetail();
  }

  protected periods(): StatsPeriod[] {
    const dashboard = this.dashboard();
    if (!dashboard) {
      return [];
    }
    return [dashboard.daily, dashboard.weekly, dashboard.monthly, dashboard.yearly];
  }

  protected previousPeriod(period: StatsPeriod): void {
    const periodType = period.periodType as PeriodType;
    const next = this.shiftIso(this.anchorFor(periodType), periodType, -1);
    this.setAnchor(periodType, next);
    this.loadDashboard();
  }

  protected nextPeriod(period: StatsPeriod): void {
    const periodType = period.periodType as PeriodType;
    const next = this.shiftIso(this.anchorFor(periodType), periodType, 1);
    if (next > this.todayIso()) {
      return;
    }
    this.setAnchor(periodType, next);
    this.loadDashboard();
  }

  protected disableNext(period: StatsPeriod): boolean {
    const periodType = period.periodType as PeriodType;
    return this.shiftIso(this.anchorFor(periodType), periodType, 1) > this.todayIso();
  }

  protected currentHeading(period: StatsPeriod): string {
    return this.headingForRange(period.periodType as PeriodType, period.current.periodStart, period.current.periodEnd);
  }

  protected previousHeading(period: StatsPeriod): string {
    return this.headingForRange(period.periodType as PeriodType, period.previous.periodStart, period.previous.periodEnd);
  }

  protected linePath(period: StatsPeriod): string {
    const points = this.visibleHistory(period);
    if (points.length <= 1) {
      return '';
    }
    const width = 260;
    const height = 96;
    const stepX = width / Math.max(points.length - 1, 1);

    return points.map((point, index) => {
      const x = index * stepX;
      const y = height - ((point.completionRate / 100) * height);
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`;
    }).join(' ');
  }

  protected barHeight(point: StatsHistoryPoint, max: number): number {
    if (max <= 0) {
      return 8;
    }
    return Math.max(8, Math.round((point.doneCount / max) * 100));
  }

  protected historyMax(period: StatsPeriod): number {
    return Math.max(...this.visibleHistory(period).map((item) => item.doneCount), 0);
  }

  protected compactAxisLabel(period: StatsPeriod, point: StatsHistoryPoint): string {
    switch (period.periodType as PeriodType) {
      case 'DAILY':
        return point.label.slice(0, 5);
      case 'WEEKLY':
        return point.label;
      case 'MONTHLY':
        return point.label.slice(0, 3);
      case 'YEARLY':
        return point.label;
      default:
        return point.label;
    }
  }

  protected detailHistoryLabel(period: StatsPeriod): string {
    switch (period.periodType as PeriodType) {
      case 'DAILY':
        return '3 derniers jours';
      case 'WEEKLY':
        return '3 dernières semaines';
      case 'MONTHLY':
        return '3 derniers mois';
      case 'YEARLY':
        return '3 dernières années';
      default:
        return '3 dernières périodes';
    }
  }

  protected visibleHistory(period: StatsPeriod): StatsHistoryPoint[] {
    const history = period.history;
    if (history.length <= this.detailHistoryWindowSize) {
      return history;
    }
    const start = Math.min(this.detailHistoryStart(), history.length - this.detailHistoryWindowSize);
    return history.slice(start, start + this.detailHistoryWindowSize);
  }

  protected canSlideHistoryToPast(period: StatsPeriod): boolean {
    return this.detailHistoryStart() > 0 && period.history.length > this.detailHistoryWindowSize;
  }

  protected canSlideHistoryToFuture(period: StatsPeriod): boolean {
    return this.detailHistoryStart() + this.detailHistoryWindowSize < period.history.length;
  }

  protected slideHistory(period: StatsPeriod, direction: -1 | 1): void {
    if (period.history.length <= this.detailHistoryWindowSize) {
      return;
    }
    const maxStart = Math.max(0, period.history.length - this.detailHistoryWindowSize);
    const next = Math.min(maxStart, Math.max(0, this.detailHistoryStart() + direction));
    this.detailHistoryStart.set(next);
  }

  protected donutBackground(snapshot: StatsSnapshot): string {
    const total = Math.max(snapshot.doneCount + snapshot.missedCount + snapshot.plannedCount + snapshot.skippedCount, 1);
    const done = (snapshot.doneCount / total) * 100;
    const missed = (snapshot.missedCount / total) * 100;
    const planned = (snapshot.plannedCount / total) * 100;
    const skipped = (snapshot.skippedCount / total) * 100;
    const doneEnd = done;
    const missedEnd = doneEnd + missed;
    const plannedEnd = missedEnd + planned;
    const skippedEnd = plannedEnd + skipped;

    return `conic-gradient(
      #15803d 0% ${doneEnd}%,
      #dc2626 ${doneEnd}% ${missedEnd}%,
      #2563eb ${missedEnd}% ${plannedEnd}%,
      #94a3b8 ${plannedEnd}% ${skippedEnd}%,
      color-mix(in srgb, var(--border) 70%, transparent) ${skippedEnd}% 100%
    )`;
  }

  protected openTaskDetail(period: StatsPeriod, task: StatsTaskSummary): void {
    this.detailLoading.set(true);
    this.statsApi.getTaskDetail(task.taskDefinitionId, period.periodType, this.anchorFor(period.periodType as PeriodType)).subscribe({
      next: ({ data }) => {
        this.selectedTaskDetail.set(data);
        this.detailLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message ?? "Impossible de charger le d\u00e9tail de la t\u00e2che.");
        this.detailLoading.set(false);
      }
    });
  }

  protected openPeriodDetail(period: StatsPeriod): void {
    this.selectedPeriodDetail.set(period);
    this.detailHistoryStart.set(Math.max(0, period.history.length - this.detailHistoryWindowSize));
  }

  protected closePeriodDetail(): void {
    this.selectedPeriodDetail.set(null);
    this.detailHistoryStart.set(0);
  }

  protected closeTaskDetail(): void {
    this.selectedTaskDetail.set(null);
    this.detailLoading.set(false);
  }

  protected stopModalPropagation(event: Event): void {
    event.stopPropagation();
  }

  protected statusLabel(status: string): string {
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

  protected dayCategoryLabel(category: string): string {
    switch (category) {
      case 'WORKDAY':
        return 'Travail';
      case 'VACATION':
        return 'Vacances';
      case 'WEEKEND_HOLIDAY':
        return 'Week-end / f\u00e9ri\u00e9';
      default:
        return category;
    }
  }

  private loadDashboard(): void {
    this.loading.set(true);
    this.statsApi.getDashboard({
      dailyAnchor: this.dailyAnchor(),
      weeklyAnchor: this.weeklyAnchor(),
      monthlyAnchor: this.monthlyAnchor(),
      yearlyAnchor: this.yearlyAnchor()
    }).subscribe({
      next: ({ data }) => {
        this.dashboard.set(data);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message ?? 'Impossible de charger le tableau de bord des statistiques.');
        this.loading.set(false);
      }
    });
  }

  private headingForRange(periodType: PeriodType, start: string, end: string): string {
    switch (periodType) {
      case 'DAILY':
        return this.formatDate(start);
      case 'WEEKLY':
        return `Semaine du ${this.formatDate(start)}`;
      case 'MONTHLY':
        return this.formatMonthYear(start);
      case 'YEARLY':
        return new Date(start).getFullYear().toString();
    }
  }

  private anchorFor(periodType: PeriodType): string {
    switch (periodType) {
      case 'DAILY':
        return this.dailyAnchor();
      case 'WEEKLY':
        return this.weeklyAnchor();
      case 'MONTHLY':
        return this.monthlyAnchor();
      case 'YEARLY':
        return this.yearlyAnchor();
    }
  }

  private setAnchor(periodType: PeriodType, value: string): void {
    switch (periodType) {
      case 'DAILY':
        this.dailyAnchor.set(value);
        break;
      case 'WEEKLY':
        this.weeklyAnchor.set(value);
        break;
      case 'MONTHLY':
        this.monthlyAnchor.set(value);
        break;
      case 'YEARLY':
        this.yearlyAnchor.set(value);
        break;
    }
  }

  private shiftIso(value: string, periodType: PeriodType, direction: -1 | 1): string {
    const date = this.parseIso(value);
    switch (periodType) {
      case 'DAILY':
        date.setDate(date.getDate() + direction);
        break;
      case 'WEEKLY':
        date.setDate(date.getDate() + (direction * 7));
        break;
      case 'MONTHLY':
        date.setMonth(date.getMonth() + direction);
        break;
      case 'YEARLY':
        date.setFullYear(date.getFullYear() + direction);
        break;
    }
    return this.toIso(date);
  }

  private formatDate(value: string): string {
    const date = new Date(value);
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  private formatMonthYear(value: string): string {
    const date = new Date(value);
    return date.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }

  private todayIso(): string {
    return this.toIso(new Date());
  }

  private parseIso(value: string): Date {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  }

  private toIso(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
