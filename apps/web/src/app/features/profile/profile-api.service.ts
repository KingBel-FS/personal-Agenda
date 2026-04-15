import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export type DayCategory = 'WORKDAY' | 'VACATION' | 'WEEKEND_HOLIDAY';

export interface DayProfile {
  dayCategory: DayCategory;
  wakeUpTime: string;
}

export interface ProfileResponse {
  userId: string;
  pseudo: string;
  email: string;
  birthDate: string;
  geographicZone: 'METROPOLE' | 'ALSACE_LORRAINE';
  timezoneName: string;
  profilePhotoSignedUrl: string | null;
  dayProfiles: DayProfile[];
  schedulingProfile: {
    geographicZone: string;
    timezoneName: string;
    dayProfiles: DayProfile[];
  };
  holidaySyncStatus: HolidaySyncStatus;
  vacationPeriods: VacationPeriod[];
}

export interface ZoneImpactResponse {
  currentZone: string;
  targetZone: string;
  holidayRulesWillChange: boolean;
  impactMessage: string;
}

export interface HolidaySyncStatus {
  status: 'PENDING' | 'SYNCED' | 'RETRY_SCHEDULED' | 'FAILED';
  lastSyncedYear: number | null;
  lastSyncedAt: string | null;
  nextRetryAt: string | null;
  lastError: string | null;
  alertVisible: boolean;
}

export interface VacationPeriod {
  id: string;
  label: string;
  startDate: string;
  endDate: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private static readonly profileUrl = '/api/v1/profile';
  private static readonly zoneImpactUrl = '/api/v1/profile/zone-impact';
  private static readonly vacationsUrl = '/api/v1/profile/vacations';

  private readonly http = inject(HttpClient);

  getProfile() {
    return this.http.get<{ data: ProfileResponse }>(ProfileApiService.profileUrl);
  }

  previewZoneImpact(geographicZone: string) {
    return this.http.post<{ data: ZoneImpactResponse }>(ProfileApiService.zoneImpactUrl, { geographicZone });
  }

  updateProfile(payload: FormData) {
    return this.http.put<{ data: ProfileResponse }>(ProfileApiService.profileUrl, payload);
  }

  listVacations() {
    return this.http.get<{ data: VacationPeriod[] }>(ProfileApiService.vacationsUrl);
  }

  createVacation(payload: Omit<VacationPeriod, 'id'>) {
    return this.http.post<{ data: VacationPeriod }>(ProfileApiService.vacationsUrl, payload);
  }

  updateVacation(vacationId: string, payload: Omit<VacationPeriod, 'id'>) {
    return this.http.put<{ data: VacationPeriod }>(`${ProfileApiService.vacationsUrl}/${vacationId}`, payload);
  }

  deleteVacation(vacationId: string) {
    return this.http.delete<{ data: { message: string } }>(`${ProfileApiService.vacationsUrl}/${vacationId}`);
  }

  deleteAccount(confirmPassword: string) {
    return this.http.delete<{ data: { message: string } }>(ProfileApiService.profileUrl, {
      body: { confirmPassword }
    });
  }

  logout() {
    return this.http.post<{ data: { message: string } }>('/api/v1/auth/logout', {}, { withCredentials: true });
  }
}
