import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface TaskResponse {
  id: string;
  title: string;
  icon: string;
  description: string | null;
  taskType: string;
  startDate: string;
  dayCategories: string[];
  timeMode: string;
  fixedTime: string | null;
  wakeUpOffsetMinutes: number | null;
  recurrenceType: string | null;
  daysOfWeek: number[] | null;
  dayOfMonth: number | null;
  endDate: string | null;
  endTime: string | null;
  photoUrl: string | null;
}

export interface TaskPreviewRequest {
  startDate: string;
  dayCategories: string[];
  timeMode: string;
  fixedTime?: string | null;
  wakeUpOffsetMinutes?: number | null;
  recurrenceType?: string | null;
  daysOfWeek?: number[] | null;
  dayOfMonth?: number | null;
}

export interface TaskPreviewResponse {
  occurrenceDate: string;
  occurrenceTime: string;
  occurrenceLabel: string;
}

export interface TimeSlotSummary {
  timeMode: string;
  fixedTime: string | null;
  afterPreviousMinutes: number | null;
}

export interface TaskOccurrence {
  id: string;
  taskDefinitionId: string;
  taskRuleId: string;
  title: string;
  icon: string;
  description: string | null;
  photoUrl?: string | null;
  taskType: string;
  timeMode: 'FIXED' | 'WAKE_UP_OFFSET';
  fixedTime: string | null;
  wakeUpOffsetMinutes: number | null;
  dayCategories: string[];
  recurrenceType: 'WEEKLY' | 'MONTHLY' | null;
  daysOfWeek: number[] | null;
  dayOfMonth: number | null;
  endDate: string | null;
  endTime: string | null;
  occurrenceDate: string;
  occurrenceTime: string;
  status: string;
  dayCategory: string;
  pastLocked: boolean;
  recurring: boolean;
  futureScopeAvailable: boolean;
  slotOrder: number | null;
  totalSlotsPerDay: number;
  timeSlots: TimeSlotSummary[];
}

export interface TaskOccurrenceListQuery {
  page: number;
  size: number;
  search?: string;
  taskKind?: 'ALL' | 'ONE_TIME' | 'RECURRING_AFTER_DATE';
  selectedDate?: string;
  occurrenceDateFrom?: string;
  occurrenceDateTo?: string;
  timeMode?: 'ALL' | 'FIXED' | 'WAKE_UP_OFFSET';
  fixedTime?: string;
  wakeUpOffsetMinutes?: number;
}

export interface TaskOccurrencePage {
  items: TaskOccurrence[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UpdateTaskOccurrenceRequest {
  scope: 'THIS_OCCURRENCE' | 'THIS_AND_FOLLOWING';
  title: string;
  icon: string;
  description: string | null;
  timeMode: 'FIXED' | 'WAKE_UP_OFFSET';
  fixedTime: string | null;
  wakeUpOffsetMinutes: number | null;
  dayCategories: string[] | null;
  recurrenceType: 'WEEKLY' | 'MONTHLY' | null;
  daysOfWeek: number[] | null;
  dayOfMonth: number | null;
  endDate: string | null;
  endTime: string | null;
  timeSlots?: Array<{ timeMode: string; fixedTime?: string | null; afterPreviousMinutes?: number | null }> | null;
}

export interface DeleteTaskOccurrenceRequest {
  scope: 'THIS_OCCURRENCE' | 'THIS_AND_FOLLOWING';
}

@Injectable({ providedIn: 'root' })
export class TaskApiService {
  private static readonly tasksUrl = '/api/v1/tasks';

  private readonly http = inject(HttpClient);

  createTask(payload: FormData) {
    return this.http.post<{ data: TaskResponse }>(TaskApiService.tasksUrl, payload);
  }

  previewNextOccurrence(payload: TaskPreviewRequest) {
    return this.http.post<{ data: TaskPreviewResponse }>(`${TaskApiService.tasksUrl}/preview`, payload);
  }

  listOccurrences(query: TaskOccurrenceListQuery) {
    const params: Record<string, string | number> = {
      page: query.page,
      size: query.size
    };

    if (query.search) {
      params['search'] = query.search;
    }
    if (query.taskKind && query.taskKind !== 'ALL') {
      params['taskKind'] = query.taskKind;
    }
    if (query.selectedDate) {
      params['selectedDate'] = query.selectedDate;
    }
    if (query.occurrenceDateFrom) {
      params['occurrenceDateFrom'] = query.occurrenceDateFrom;
    }
    if (query.occurrenceDateTo) {
      params['occurrenceDateTo'] = query.occurrenceDateTo;
    }
    if (query.timeMode && query.timeMode !== 'ALL') {
      params['timeMode'] = query.timeMode;
    }
    if (query.fixedTime) {
      params['fixedTime'] = query.fixedTime;
    }
    if (query.wakeUpOffsetMinutes !== undefined && query.wakeUpOffsetMinutes !== null) {
      params['wakeUpOffsetMinutes'] = query.wakeUpOffsetMinutes;
    }

    return this.http.get<{ data: TaskOccurrencePage }>(`${TaskApiService.tasksUrl}/occurrences`, { params });
  }

  updateOccurrence(occurrenceId: string, payload: UpdateTaskOccurrenceRequest) {
    return this.http.put<{ data: TaskOccurrence }>(`${TaskApiService.tasksUrl}/occurrences/${occurrenceId}`, payload);
  }

  updateOccurrencePhoto(occurrenceId: string, scope: 'THIS_OCCURRENCE' | 'THIS_AND_FOLLOWING', photo: File) {
    const payload = new FormData();
    payload.set('scope', scope);
    payload.set('photo', photo);
    return this.http.post<{ data: TaskOccurrence }>(
      `${TaskApiService.tasksUrl}/occurrences/${occurrenceId}/photo`,
      payload
    );
  }

  deleteOccurrence(occurrenceId: string, payload: DeleteTaskOccurrenceRequest) {
    return this.http.request<{ data: { message: string } }>('DELETE', `${TaskApiService.tasksUrl}/occurrences/${occurrenceId}`, {
      body: payload
    });
  }
}
