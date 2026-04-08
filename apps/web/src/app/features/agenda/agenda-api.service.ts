import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface AgendaDaySummary {
  date: string;
  currentMonth: boolean;
  past: boolean;
  today: boolean;
  beforeAccountCreation: boolean;
  dayCategory: 'WORKDAY' | 'VACATION' | 'WEEKEND_HOLIDAY' | string;
  totalCount: number;
  plannedCount: number;
  doneCount: number;
  missedCount: number;
  skippedCount: number;
  statusTone: 'empty' | 'planned' | 'done' | 'missed' | 'skipped' | 'mixed' | string;
  icons: string[];
}

export interface AgendaRangeResponse {
  view: 'week' | 'month';
  anchorDate: string;
  rangeStart: string;
  rangeEnd: string;
  days: AgendaDaySummary[];
}

@Injectable({ providedIn: 'root' })
export class AgendaApiService {
  private static readonly baseUrl = '/api/v1/agenda';

  private readonly http = inject(HttpClient);

  getWeek(date: string) {
    return this.http.get<{ data: AgendaRangeResponse }>(`${AgendaApiService.baseUrl}/week`, {
      params: { date }
    });
  }

  getMonth(date: string) {
    return this.http.get<{ data: AgendaRangeResponse }>(`${AgendaApiService.baseUrl}/month`, {
      params: { date }
    });
  }
}
