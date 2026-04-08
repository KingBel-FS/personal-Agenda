import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { GoalsPageComponent } from './goals-page.component';
import { authInterceptor } from '../../core/auth.interceptor';
import { AuthService } from '../../core/auth.service';

describe('GoalsPageComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoalsPageComponent],
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    TestBed.inject(AuthService).setAccessToken('test-token');
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    TestBed.inject(AuthService).clearAccessToken();
  });

  it('loads active and inactive goals on init', () => {
    const fixture = TestBed.createComponent(GoalsPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/goals').flush({
      data: {
        goals: [
          {
            id: 'goal-1',
            goalScope: 'GLOBAL',
            periodType: 'WEEKLY',
            targetCount: 4,
            active: true,
            taskDefinitionId: null,
            taskTitle: null,
            taskIcon: null,
            recurrenceType: null,
            currentProgress: {
              periodStart: '2026-03-23',
              periodEnd: '2026-03-29',
              completedCount: 2,
              targetCount: 4,
              remainingCount: 2,
              progressPercent: 50,
              goalMet: false,
              status: 'IN_PROGRESS'
            },
            recentHistory: [],
            createdAt: '2026-03-26T10:00:00Z',
            updatedAt: '2026-03-26T10:00:00Z'
          }
        ],
        inactiveGoals: [
          {
            id: 'goal-2',
            goalScope: 'GLOBAL',
            periodType: 'MONTHLY',
            targetCount: 8,
            active: false,
            taskDefinitionId: null,
            taskTitle: null,
            taskIcon: null,
            recurrenceType: null,
            currentProgress: {
              periodStart: '2026-03-01',
              periodEnd: '2026-03-31',
              completedCount: 1,
              targetCount: 8,
              remainingCount: 7,
              progressPercent: 12,
              goalMet: false,
              status: 'IN_PROGRESS'
            },
            recentHistory: [],
            createdAt: '2026-03-26T10:00:00Z',
            updatedAt: '2026-03-26T10:00:00Z'
          }
        ],
        eligibleTasks: [
          { taskDefinitionId: 'task-1', title: 'Lecture', icon: '📚', recurrenceType: 'WEEKLY' }
        ],
        accountCreatedAt: '2026-03-24T10:00:00Z'
      }
    });

    const component = fixture.componentInstance as any;
    expect(component.goals().length).toBe(1);
    expect(component.inactiveGoals().length).toBe(1);
    expect(component.eligibleTasks().length).toBe(1);
  });

  it('creates a goal from the form', () => {
    const fixture = TestBed.createComponent(GoalsPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/goals').flush({
      data: { goals: [], inactiveGoals: [], eligibleTasks: [], accountCreatedAt: '2026-03-24T10:00:00Z' }
    });

    const component = fixture.componentInstance as any;
    component.form.setValue({
      goalScope: 'GLOBAL',
      periodType: 'MONTHLY',
      targetCount: 8,
      taskDefinitionId: '',
      active: true
    });
    component.submit();

    const request = httpTesting.expectOne('/api/v1/goals');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      goalScope: 'GLOBAL',
      periodType: 'MONTHLY',
      targetCount: 8,
      taskDefinitionId: null
    });
    request.flush({
      data: {
        id: 'goal-2',
        goalScope: 'GLOBAL',
        periodType: 'MONTHLY',
        targetCount: 8,
        active: true,
        taskDefinitionId: null,
        taskTitle: null,
        taskIcon: null,
        recurrenceType: null,
        currentProgress: {
          periodStart: '2026-03-01',
          periodEnd: '2026-03-31',
          completedCount: 0,
          targetCount: 8,
          remainingCount: 8,
          progressPercent: 0,
          goalMet: false,
          status: 'IN_PROGRESS'
        },
        recentHistory: [],
        createdAt: '2026-03-26T10:00:00Z',
        updatedAt: '2026-03-26T10:00:00Z'
      }
    });

    httpTesting.expectOne('/api/v1/goals').flush({
      data: { goals: [], inactiveGoals: [], eligibleTasks: [], accountCreatedAt: '2026-03-24T10:00:00Z' }
    });

    expect(component.successMessage()).toContain('créé');
  });

  it('opens a detail modal when a history period is selected', () => {
    const fixture = TestBed.createComponent(GoalsPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/goals').flush({
      data: {
        goals: [
          {
            id: 'goal-1',
            goalScope: 'GLOBAL',
            periodType: 'WEEKLY',
            targetCount: 4,
            active: true,
            taskDefinitionId: null,
            taskTitle: null,
            taskIcon: null,
            recurrenceType: null,
            currentProgress: {
              periodStart: '2026-03-23',
              periodEnd: '2026-03-29',
              completedCount: 2,
              targetCount: 4,
              remainingCount: 2,
              progressPercent: 50,
              goalMet: false,
              status: 'IN_PROGRESS'
            },
            recentHistory: [
              {
                periodStart: '2026-03-16',
                periodEnd: '2026-03-22',
                completedCount: 4,
                targetCount: 4,
                progressPercent: 100,
                goalMet: true
              }
            ],
            createdAt: '2026-03-26T10:00:00Z',
            updatedAt: '2026-03-26T10:00:00Z'
          }
        ],
        inactiveGoals: [],
        eligibleTasks: [],
        accountCreatedAt: '2026-03-01T10:00:00Z'
      }
    });

    const component = fixture.componentInstance as any;
    component.openHistoryPeriod(component.goals()[0], component.goals()[0].recentHistory[0]);

    expect(component.selectedPeriodDetail()).not.toBeNull();
    expect(component.selectedPeriodDetail().snapshot.goalMet).toBeTrue();
  });

  it('filters out history periods that end before account creation', () => {
    const fixture = TestBed.createComponent(GoalsPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/goals').flush({
      data: {
        goals: [],
        inactiveGoals: [],
        eligibleTasks: [],
        accountCreatedAt: '2026-03-24T10:00:00Z'
      }
    });

    const component = fixture.componentInstance as any;
    expect(component.isPeriodAvailable({ periodStart: '2026-02-01', periodEnd: '2026-02-28' })).toBeFalse();
    expect(component.isPeriodAvailable({ periodStart: '2026-03-23', periodEnd: '2026-03-29' })).toBeTrue();
    expect(component.visibleHistory({
      recentHistory: [
        { periodStart: '2026-02-01', periodEnd: '2026-02-28', completedCount: 1, targetCount: 4, progressPercent: 25, goalMet: false },
        { periodStart: '2026-03-23', periodEnd: '2026-03-29', completedCount: 3, targetCount: 4, progressPercent: 75, goalMet: false }
      ]
    }).length).toBe(1);
  });
});
