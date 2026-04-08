import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface TodayOccurrenceItem {
  id: string;
  taskDefinitionId: string;
  title: string;
  description: string;
  icon: string;
  occurrenceTime: string;
  status: 'planned' | 'done' | 'missed' | 'skipped' | 'canceled';
  dayCategory: string;
  recurring: boolean;
  slotOrder: number | null;
  totalSlotsPerDay: number;
}

export interface StreakInfo {
  currentStreak: number;
  longestStreak: number;
  streakActive: boolean;
  badges: string[];
}

export interface TodayResponse {
  date: string;
  beforeAccountCreation: boolean;
  dayCategory: string;
  activeCount: number;
  skippedCount: number;
  doneCount: number;
  missedCount: number;
  totalCount: number;
  progressPercent: number;
  occurrences: TodayOccurrenceItem[];
  streak: StreakInfo;
  newBadges: string[];
}

@Injectable({ providedIn: 'root' })
export class TodayApiService {
  private static readonly baseUrl = '/api/v1/today';

  private readonly http = inject(HttpClient);

  getToday() {
    return this.http.get<{ data: TodayResponse }>(TodayApiService.baseUrl);
  }

  getDaily(date: string) {
    return this.http.get<{ data: TodayResponse }>(`${TodayApiService.baseUrl}/daily`, {
      params: { date }
    });
  }

  complete(occurrenceId: string) {
    return this.http.post<{ data: TodayResponse }>(
      `${TodayApiService.baseUrl}/occurrences/${occurrenceId}/complete`, {}
    );
  }

  miss(occurrenceId: string) {
    return this.http.post<{ data: TodayResponse }>(
      `${TodayApiService.baseUrl}/occurrences/${occurrenceId}/miss`, {}
    );
  }

  cancel(occurrenceId: string) {
    return this.http.post<{ data: TodayResponse }>(
      `${TodayApiService.baseUrl}/occurrences/${occurrenceId}/cancel`, {}
    );
  }

  editDescription(occurrenceId: string, description: string) {
    return this.http.post<{ data: TodayResponse }>(
      `${TodayApiService.baseUrl}/occurrences/${occurrenceId}/edit-description`,
      { description }
    );
  }

  editTime(occurrenceId: string, time: string) {
    return this.http.post<{ data: TodayResponse }>(
      `${TodayApiService.baseUrl}/occurrences/${occurrenceId}/edit-time`,
      { time }
    );
  }

  revertToPlanned(occurrenceId: string) {
    return this.http.post<{ data: TodayResponse }>(
      `${TodayApiService.baseUrl}/occurrences/${occurrenceId}/revert`, {}
    );
  }

  reschedule(occurrenceId: string, date: string, time: string) {
    return this.http.post<{ data: TodayResponse }>(
      `${TodayApiService.baseUrl}/occurrences/${occurrenceId}/reschedule`,
      { date, time }
    );
  }
}
