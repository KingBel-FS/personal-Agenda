import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface NotificationItem {
  id: string;
  title: string;
  body: string;
  iconUrl: string | null;
  notificationType: string;
  relatedTaskId: string | null;
  status: string;
  createdAt: string;
}

export interface NotificationCenterResponse {
  notifications: NotificationItem[];
  totalCount: number;
  hasMore: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private static readonly baseUrl = '/api/v1/notifications';

  private readonly http = inject(HttpClient);

  getCenter(page = 0, size = 20) {
    return this.http.get<{ data: NotificationCenterResponse }>(
      `${NotificationApiService.baseUrl}/center`,
      { params: { page: page.toString(), size: size.toString() } }
    );
  }

  getUnviewedCount() {
    return this.http.get<{ data: { count: number } }>(
      `${NotificationApiService.baseUrl}/center/unviewed-count`
    );
  }

  markViewed(notificationId: string) {
    return this.http.post<{ data: { status: string } }>(
      `${NotificationApiService.baseUrl}/center/${notificationId}/mark-viewed`, {}
    );
  }

  dismiss(notificationId: string) {
    return this.http.post<{ data: { status: string } }>(
      `${NotificationApiService.baseUrl}/center/${notificationId}/dismiss`, {}
    );
  }
}
