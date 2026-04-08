import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface StatsSnapshot {
  periodStart: string;
  periodEnd: string;
  totalCount: number;
  doneCount: number;
  missedCount: number;
  skippedCount: number;
  plannedCount: number;
  taskCount: number;
  completionRate: number;
}

export interface StatsHistoryPoint {
  label: string;
  periodStart: string;
  periodEnd: string;
  totalCount: number;
  doneCount: number;
  missedCount: number;
  skippedCount: number;
  plannedCount: number;
  completionRate: number;
}

export interface StatsDelta {
  totalCountDelta: number;
  doneCountDelta: number;
  completionRateDelta: number;
}

export interface StatsTaskSummary {
  taskDefinitionId: string;
  title: string;
  icon: string;
  totalCount: number;
  doneCount: number;
  missedCount: number;
  skippedCount: number;
  plannedCount: number;
  completionRate: number;
  doneCountDelta: number;
  completionRateDelta: number;
}

export interface StatsPeriod {
  periodType: 'WEEKLY' | 'MONTHLY' | 'YEARLY' | 'GLOBAL' | string;
  label: string;
  current: StatsSnapshot;
  previous: StatsSnapshot;
  delta: StatsDelta;
  comparisonLabel: string;
  taskBreakdown: StatsTaskSummary[];
  history: StatsHistoryPoint[];
}

export interface StatsDashboardResponse {
  generatedAt: string;
  accountCreatedAt: string;
  daily: StatsPeriod;
  weekly: StatsPeriod;
  monthly: StatsPeriod;
  yearly: StatsPeriod;
}

export interface StatsTaskRecentOccurrence {
  occurrenceDate: string;
  occurrenceTime: string;
  status: string;
  dayCategory: string;
}

export interface StatsTaskDetail {
  taskDefinitionId: string;
  title: string;
  icon: string;
  periodType: 'WEEKLY' | 'MONTHLY' | 'YEARLY' | 'GLOBAL' | string;
  label: string;
  current: StatsSnapshot;
  previous: StatsSnapshot;
  delta: StatsDelta;
  recentOccurrences: StatsTaskRecentOccurrence[];
}

@Injectable({ providedIn: 'root' })
export class StatsApiService {
  private static readonly baseUrl = '/api/v1/stats';

  private readonly http = inject(HttpClient);

  getDashboard(anchors?: { dailyAnchor?: string; weeklyAnchor?: string; monthlyAnchor?: string; yearlyAnchor?: string }) {
    const params: Record<string, string> = {};
    if (anchors?.dailyAnchor) params['dailyAnchor'] = anchors.dailyAnchor;
    if (anchors?.weeklyAnchor) params['weeklyAnchor'] = anchors.weeklyAnchor;
    if (anchors?.monthlyAnchor) params['monthlyAnchor'] = anchors.monthlyAnchor;
    if (anchors?.yearlyAnchor) params['yearlyAnchor'] = anchors.yearlyAnchor;

    return this.http.get<{ data: StatsDashboardResponse }>(`${StatsApiService.baseUrl}/dashboard`, { params });
  }

  getTaskDetail(taskDefinitionId: string, periodType: string, anchorDate?: string) {
    return this.http.get<{ data: StatsTaskDetail }>(`${StatsApiService.baseUrl}/tasks/${taskDefinitionId}`, {
      params: anchorDate ? { periodType, anchorDate } : { periodType }
    });
  }
}
