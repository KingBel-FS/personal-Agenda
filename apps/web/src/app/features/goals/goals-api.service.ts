import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface GoalProgressSnapshot {
  periodStart: string;
  periodEnd: string;
  completedCount: number;
  targetCount: number;
  remainingCount: number;
  progressPercent: number;
  goalMet: boolean;
  status: string;
}

export interface GoalProgressHistoryItem {
  periodStart: string;
  periodEnd: string;
  completedCount: number;
  targetCount: number;
  progressPercent: number;
  goalMet: boolean;
}

export interface GoalEligibleTaskItem {
  taskDefinitionId: string;
  title: string;
  icon: string;
  recurrenceType: string;
}

export interface GoalItem {
  id: string;
  goalScope: 'GLOBAL' | 'TASK' | string;
  periodType: 'WEEKLY' | 'MONTHLY' | string;
  targetCount: number;
  active: boolean;
  taskDefinitionId: string | null;
  taskTitle: string | null;
  taskIcon: string | null;
  recurrenceType: string | null;
  currentProgress: GoalProgressSnapshot;
  recentHistory: GoalProgressHistoryItem[];
  createdAt: string;
  updatedAt: string;
}

export interface GoalListResponse {
  goals: GoalItem[];
  inactiveGoals: GoalItem[];
  eligibleTasks: GoalEligibleTaskItem[];
  accountCreatedAt: string;
}

export interface GoalPayload {
  goalScope: 'GLOBAL' | 'TASK';
  periodType: 'WEEKLY' | 'MONTHLY';
  targetCount: number;
  taskDefinitionId?: string | null;
  active?: boolean;
}

@Injectable({ providedIn: 'root' })
export class GoalsApiService {
  private static readonly baseUrl = '/api/v1/goals';

  private readonly http = inject(HttpClient);

  listGoals() {
    return this.http.get<{ data: GoalListResponse }>(GoalsApiService.baseUrl);
  }

  createGoal(payload: GoalPayload) {
    return this.http.post<{ data: GoalItem }>(GoalsApiService.baseUrl, payload);
  }

  updateGoal(goalId: string, payload: GoalPayload) {
    return this.http.put<{ data: GoalItem }>(`${GoalsApiService.baseUrl}/${goalId}`, {
      ...payload,
      active: payload.active ?? true
    });
  }

  deleteGoal(goalId: string) {
    return this.http.delete<{ data: { message: string } }>(`${GoalsApiService.baseUrl}/${goalId}`);
  }
}
