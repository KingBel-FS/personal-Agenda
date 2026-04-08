import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface WakeUpOverride {
  date: string;
  wakeUpTime: string;
}

@Injectable({ providedIn: 'root' })
export class WakeUpOverrideApiService {
  private static readonly baseUrl = '/api/v1/wake-up-override';

  private readonly http = inject(HttpClient);

  getOverride(date: string) {
    return this.http.get<{ data: WakeUpOverride | null }>(WakeUpOverrideApiService.baseUrl, {
      params: { date }
    });
  }

  upsertOverride(date: string, wakeUpTime: string) {
    return this.http.put<{ data: WakeUpOverride }>(WakeUpOverrideApiService.baseUrl, {
      date,
      wakeUpTime
    });
  }

  deleteOverride(date: string) {
    return this.http.delete<{ data: null }>(WakeUpOverrideApiService.baseUrl, {
      params: { date }
    });
  }
}
