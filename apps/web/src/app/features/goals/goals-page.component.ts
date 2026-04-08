import { Component, HostListener, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  GoalsApiService,
  type GoalEligibleTaskItem,
  type GoalItem,
  type GoalPayload,
  type GoalProgressHistoryItem,
  type GoalProgressSnapshot
} from './goals-api.service';

type GoalPeriodDetail = {
  goalId: string;
  goalTitle: string;
  periodType: string;
  periodLabel: string;
  snapshot: GoalProgressSnapshot;
};

@Component({
  selector: 'app-goals-page',
  imports: [ReactiveFormsModule],
  templateUrl: './goals-page.component.html',
  styleUrl: './goals-page.component.scss'
})
export class GoalsPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly goalsApi = inject(GoalsApiService);

  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly deletingGoalId = signal<string | null>(null);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly goals = signal<GoalItem[]>([]);
  protected readonly inactiveGoals = signal<GoalItem[]>([]);
  protected readonly eligibleTasks = signal<GoalEligibleTaskItem[]>([]);
  protected readonly editingGoalId = signal<string | null>(null);
  protected readonly selectedPeriodDetail = signal<GoalPeriodDetail | null>(null);
  protected readonly accountCreatedAt = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    goalScope: ['GLOBAL' as 'GLOBAL' | 'TASK', Validators.required],
    periodType: ['WEEKLY' as 'WEEKLY' | 'MONTHLY', Validators.required],
    targetCount: [3, [Validators.required, Validators.min(1), Validators.max(1000)]],
    taskDefinitionId: [''],
    active: [true]
  });

  constructor() {
    this.loadGoals();
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.closePeriodDetail();
  }

  protected isTaskScope(): boolean {
    return this.form.controls.goalScope.value === 'TASK';
  }

  protected startCreate(): void {
    this.editingGoalId.set(null);
    this.errorMessage.set('');
    this.form.reset({
      goalScope: 'GLOBAL',
      periodType: 'WEEKLY',
      targetCount: 3,
      taskDefinitionId: '',
      active: true
    });
  }

  protected editGoal(goal: GoalItem): void {
    this.editingGoalId.set(goal.id);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.form.reset({
      goalScope: goal.goalScope === 'TASK' ? 'TASK' : 'GLOBAL',
      periodType: goal.periodType === 'MONTHLY' ? 'MONTHLY' : 'WEEKLY',
      targetCount: goal.targetCount,
      taskDefinitionId: goal.taskDefinitionId ?? '',
      active: goal.active
    });
  }

  protected cancelEdit(): void {
    this.startCreate();
  }

  protected submit(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.buildPayload();
    const requestPayload = this.editingGoalId()
      ? payload
      : {
          goalScope: payload.goalScope,
          periodType: payload.periodType,
          targetCount: payload.targetCount,
          taskDefinitionId: payload.taskDefinitionId
        };
    this.submitting.set(true);

    const request = this.editingGoalId()
      ? this.goalsApi.updateGoal(this.editingGoalId()!, payload)
      : this.goalsApi.createGoal(requestPayload);

    request.subscribe({
      next: () => {
        this.submitting.set(false);
        const successMessage = this.editingGoalId() ? 'Objectif mis à jour.' : 'Objectif créé.';
        this.startCreate();
        this.successMessage.set(successMessage);
        this.loadGoals();
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(err?.error?.error?.message ?? "Impossible d'enregistrer l'objectif.");
      }
    });
  }

  protected deleteGoal(goal: GoalItem): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    this.deletingGoalId.set(goal.id);
    this.goalsApi.deleteGoal(goal.id).subscribe({
      next: () => {
        this.deletingGoalId.set(null);
        this.successMessage.set('Objectif supprimé.');
        if (this.editingGoalId() === goal.id) {
          this.startCreate();
        }
        this.loadGoals();
      },
      error: (err) => {
        this.deletingGoalId.set(null);
        this.errorMessage.set(err?.error?.error?.message ?? "Impossible de supprimer l'objectif.");
      }
    });
  }

  protected reactivateGoal(goal: GoalItem): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    this.submitting.set(true);
    this.goalsApi.updateGoal(goal.id, {
      goalScope: goal.goalScope === 'TASK' ? 'TASK' : 'GLOBAL',
      periodType: goal.periodType === 'MONTHLY' ? 'MONTHLY' : 'WEEKLY',
      targetCount: goal.targetCount,
      taskDefinitionId: goal.taskDefinitionId,
      active: true
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.successMessage.set('Objectif réactivé.');
        this.loadGoals();
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(err?.error?.error?.message ?? "Impossible de réactiver l'objectif.");
      }
    });
  }

  protected scopeLabel(goal: GoalItem): string {
    return goal.goalScope === 'TASK' ? 'Par tâche' : 'Global';
  }

  protected periodLabel(value: string): string {
    return value === 'MONTHLY' ? 'Mensuel' : 'Hebdomadaire';
  }

  protected periodLabelLong(value: string): string {
    return value === 'MONTHLY' ? 'Mois' : 'Semaine';
  }

  protected achievedGoalCount(): number {
    return this.goals().filter((goal) => goal.currentProgress.goalMet).length;
  }

  protected totalRemainingCount(): number {
    return this.goals().reduce((total, goal) => total + goal.currentProgress.remainingCount, 0);
  }

  protected averageProgress(): number {
    const goals = this.goals();
    if (goals.length === 0) {
      return 0;
    }
    return Math.round(goals.reduce((total, goal) => total + goal.currentProgress.progressPercent, 0) / goals.length);
  }

  protected progressStateLabel(goal: GoalItem): string {
    if (goal.currentProgress.goalMet) {
      return 'Atteint';
    }
    if (goal.currentProgress.progressPercent >= 70) {
      return 'En bonne voie';
    }
    if (goal.currentProgress.progressPercent >= 35) {
      return 'À consolider';
    }
    return 'À relancer';
  }

  protected detailStateLabel(detail: GoalPeriodDetail | null): string {
    if (!detail) {
      return '';
    }
    if (detail.snapshot.goalMet) {
      return 'Objectif atteint';
    }
    if (detail.snapshot.progressPercent >= 70) {
      return 'Très proche';
    }
    if (detail.snapshot.progressPercent >= 35) {
      return 'En progression';
    }
    return 'À relancer';
  }

  protected historyLabel(item: { periodStart: string; periodEnd: string }, periodType: string): string {
    if (periodType === 'MONTHLY') {
      return new Date(item.periodStart).toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
    }
    const start = new Date(item.periodStart).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
    const end = new Date(item.periodEnd).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
    return `${start} → ${end}`;
  }

  protected isPeriodAvailable(item: { periodStart: string; periodEnd: string }): boolean {
    const accountCreatedAt = this.accountCreatedAt();
    if (!accountCreatedAt) {
      return true;
    }
    const accountDate = new Date(accountCreatedAt);
    accountDate.setHours(0, 0, 0, 0);
    const periodEnd = new Date(item.periodEnd);
    periodEnd.setHours(0, 0, 0, 0);
    return periodEnd >= accountDate;
  }

  protected visibleHistory(goal: GoalItem): GoalProgressHistoryItem[] {
    return goal.recentHistory.filter((item) => this.isPeriodAvailable(item));
  }

  protected openCurrentPeriod(goal: GoalItem): void {
    this.selectedPeriodDetail.set({
      goalId: goal.id,
      goalTitle: goal.taskTitle ? `${goal.taskIcon ?? ''} ${goal.taskTitle}`.trim() : 'Objectif global',
      periodType: goal.periodType,
      periodLabel: `${this.periodLabelLong(goal.periodType)} en cours`,
      snapshot: goal.currentProgress
    });
  }

  protected openHistoryPeriod(goal: GoalItem, item: GoalProgressHistoryItem): void {
    this.selectedPeriodDetail.set({
      goalId: goal.id,
      goalTitle: goal.taskTitle ? `${goal.taskIcon ?? ''} ${goal.taskTitle}`.trim() : 'Objectif global',
      periodType: goal.periodType,
      periodLabel: this.historyLabel(item, goal.periodType),
      snapshot: {
        periodStart: item.periodStart,
        periodEnd: item.periodEnd,
        completedCount: item.completedCount,
        targetCount: item.targetCount,
        remainingCount: Math.max(item.targetCount - item.completedCount, 0),
        progressPercent: item.progressPercent,
        goalMet: item.goalMet,
        status: item.goalMet ? 'ACHIEVED' : 'IN_PROGRESS'
      }
    });
  }

  protected closePeriodDetail(): void {
    this.selectedPeriodDetail.set(null);
  }

  protected stopModalPropagation(event: Event): void {
    event.stopPropagation();
  }

  private buildPayload(): GoalPayload {
    const raw = this.form.getRawValue();
    return {
      goalScope: raw.goalScope,
      periodType: raw.periodType,
      targetCount: Number(raw.targetCount),
      taskDefinitionId: raw.goalScope === 'TASK' ? raw.taskDefinitionId || null : null,
      active: raw.active
    };
  }

  private loadGoals(): void {
    this.loading.set(true);
    this.goalsApi.listGoals().subscribe({
      next: ({ data }) => {
        this.goals.set(data.goals);
        this.inactiveGoals.set(data.inactiveGoals ?? []);
        this.eligibleTasks.set(data.eligibleTasks);
        this.accountCreatedAt.set(data.accountCreatedAt ?? null);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error?.message ?? 'Impossible de charger les objectifs.');
        this.loading.set(false);
      }
    });
  }
}
