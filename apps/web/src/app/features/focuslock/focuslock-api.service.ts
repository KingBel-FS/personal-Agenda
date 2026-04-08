import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface FlDevice {
  id: string;
  deviceName: string;
  status: 'PENDING' | 'ACTIVE' | 'REVOKED';
  familyControlsGranted: boolean;
  screenTimeGranted: boolean;
  notificationsGranted: boolean;
  pairedAt: string | null;
  lastSeenAt: string | null;
  createdAt: string;
}

export interface FlPairingToken {
  token: string;
  expiresAt: string;
}

export interface FlScheduleItem {
  id?: string;
  startTime: string;
  endTime: string;
  daysOfWeek: string;
}

export interface FlWebDomainItem {
  id?: string;
  domain: string;
}

export interface FlRule {
  id: string;
  name: string;
  targetType: 'APP' | 'CATEGORY' | 'DOMAIN';
  targetIdentifier: string;
  ruleType: 'DAILY_LIMIT' | 'TIME_BLOCK';
  limitMinutes: number | null;
  frictionType: 'NONE' | 'DELAY_60' | 'CONFIRMATION';
  active: boolean;
  schedules: FlScheduleItem[];
  domains: FlWebDomainItem[];
  overrideCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface FlRuleRequest {
  name: string;
  targetType: string;
  targetIdentifier: string;
  ruleType: string;
  limitMinutes?: number | null;
  frictionType?: string;
  schedules?: FlScheduleItem[];
  domains?: string[];
}

export interface FlUsageItem {
  targetIdentifier: string;
  consumedMinutes: number;
}

export interface FlWeeklyDayItem {
  date: string;
  dayLabel: string;
  totalMinutes: number;
}

export interface FlDashboard {
  activeDevice: FlDevice | null;
  activeRuleCount: number;
  totalRuleCount: number;
  totalMinutesToday: number;
  todayUsage: FlUsageItem[];
  activeRules: FlRule[];
}

export interface FlInsights {
  topApps: FlUsageItem[];
  weeklyBreakdown: FlWeeklyDayItem[];
}

@Injectable({ providedIn: 'root' })
export class FocuslockApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/focuslock';

  // ── Devices ────────────────────────────────────────────────────────
  listDevices(): Observable<{ data: FlDevice[] }> {
    return this.http.get<{ data: FlDevice[] }>(`${this.base}/devices`);
  }

  generatePairingToken(): Observable<{ data: FlPairingToken }> {
    return this.http.post<{ data: FlPairingToken }>(`${this.base}/devices/pair-token`, {});
  }

  confirmPairing(token: string, deviceName?: string): Observable<{ data: FlDevice }> {
    return this.http.post<{ data: FlDevice }>(`${this.base}/devices/confirm`, { token, deviceName });
  }

  revokeDevice(id: string): Observable<{ data: null }> {
    return this.http.delete<{ data: null }>(`${this.base}/devices/${id}`);
  }

  // ── Rules ───────────────────────────────────────────────────────────
  listRules(): Observable<{ data: FlRule[] }> {
    return this.http.get<{ data: FlRule[] }>(`${this.base}/rules`);
  }

  createRule(request: FlRuleRequest): Observable<{ data: FlRule }> {
    return this.http.post<{ data: FlRule }>(`${this.base}/rules`, request);
  }

  updateRule(id: string, request: FlRuleRequest): Observable<{ data: FlRule }> {
    return this.http.put<{ data: FlRule }>(`${this.base}/rules/${id}`, request);
  }

  toggleRule(id: string): Observable<{ data: FlRule }> {
    return this.http.patch<{ data: FlRule }>(`${this.base}/rules/${id}/toggle`, {});
  }

  deleteRule(id: string): Observable<{ data: null }> {
    return this.http.delete<{ data: null }>(`${this.base}/rules/${id}`);
  }

  // ── Dashboard & Insights ────────────────────────────────────────────
  getDashboard(): Observable<{ data: FlDashboard }> {
    return this.http.get<{ data: FlDashboard }>(`${this.base}/dashboard`);
  }

  getInsights(): Observable<{ data: FlInsights }> {
    return this.http.get<{ data: FlInsights }>(`${this.base}/insights`);
  }

  recordUsage(deviceId: string, targetIdentifier: string, consumedMinutes: number, eventDate: string): Observable<{ data: null }> {
    return this.http.post<{ data: null }>(`${this.base}/usage`, { deviceId, targetIdentifier, consumedMinutes, eventDate });
  }
}
