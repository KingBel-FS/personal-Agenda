import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { StatsPageComponent } from './stats-page.component';
import { authInterceptor } from '../../core/auth.interceptor';
import { AuthService } from '../../core/auth.service';

describe('StatsPageComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatsPageComponent],
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    TestBed.inject(AuthService).setAccessToken('test-token');
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    TestBed.inject(AuthService).clearAccessToken();
  });

  it('loads the four temporal axes on init', () => {
    const fixture = TestBed.createComponent(StatsPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne((request) => request.url === '/api/v1/stats/dashboard').flush({
      data: dashboard()
    });

    const component = fixture.componentInstance as any;
    expect(component.periods().length).toBe(4);
    expect(component.periods()[0].periodType).toBe('DAILY');
  });

  it('navigates monthly periods with anchors', () => {
    const fixture = TestBed.createComponent(StatsPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne((request) => request.url === '/api/v1/stats/dashboard').flush({
      data: dashboard()
    });

    const component = fixture.componentInstance as any;
    component.previousPeriod(component.periods()[2]);

    httpTesting.expectOne((request) =>
      request.url === '/api/v1/stats/dashboard' &&
      !!request.params.get('monthlyAnchor')
    ).flush({
      data: dashboard()
    });

    expect(component.monthlyAnchor()).toBe('2026-02-27');
  });

  it('opens task detail with the selected period anchor', () => {
    const fixture = TestBed.createComponent(StatsPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne((request) => request.url === '/api/v1/stats/dashboard').flush({
      data: dashboard()
    });

    const component = fixture.componentInstance as any;
    component.openTaskDetail(component.periods()[1], component.periods()[1].taskBreakdown[0]);

    httpTesting.expectOne((request) =>
      request.url === '/api/v1/stats/tasks/task-1' &&
      request.params.get('periodType') === 'WEEKLY' &&
      !!request.params.get('anchorDate')
    ).flush({
      data: detail()
    });

    expect(component.selectedTaskDetail()?.title).toBe('Lecture');
  });
});

function snapshot(overrides: Record<string, unknown> = {}) {
  return {
    periodStart: '2026-03-23',
    periodEnd: '2026-03-29',
    totalCount: 4,
    doneCount: 3,
    missedCount: 1,
    skippedCount: 0,
    plannedCount: 0,
    taskCount: 1,
    completionRate: 75,
    ...overrides
  };
}

function history(label: string) {
  return {
    label,
    periodStart: '2026-03-23',
    periodEnd: '2026-03-29',
    totalCount: 4,
    doneCount: 3,
    missedCount: 1,
    skippedCount: 0,
    plannedCount: 0,
    completionRate: 75
  };
}

function period(periodType: string, label: string) {
  return {
    periodType,
    label,
    current: snapshot(),
    previous: snapshot({ periodStart: '2026-03-16', periodEnd: '2026-03-22', completionRate: 50 }),
    delta: { totalCountDelta: 2, doneCountDelta: 1, completionRateDelta: 25 },
    comparisonLabel: 'Comparaison',
    taskBreakdown: [
      {
        taskDefinitionId: 'task-1',
        title: 'Lecture',
        icon: '📚',
        totalCount: 4,
        doneCount: 3,
        missedCount: 1,
        skippedCount: 0,
        plannedCount: 0,
        completionRate: 75,
        doneCountDelta: 1,
        completionRateDelta: 25
      }
    ],
    history: [history('A'), history('B'), history('C')]
  };
}

function dashboard() {
  return {
    generatedAt: '2026-03-27T10:00:00Z',
    accountCreatedAt: '2026-03-20T10:00:00Z',
    daily: period('DAILY', 'Quotidien'),
    weekly: period('WEEKLY', 'Hebdomadaire'),
    monthly: period('MONTHLY', 'Mensuel'),
    yearly: period('YEARLY', 'Annuel')
  };
}

function detail() {
  return {
    taskDefinitionId: 'task-1',
    title: 'Lecture',
    icon: '📚',
    periodType: 'WEEKLY',
    label: 'Hebdomadaire',
    current: snapshot(),
    previous: snapshot({ periodStart: '2026-03-16', periodEnd: '2026-03-22', completionRate: 50 }),
    delta: { totalCountDelta: 2, doneCountDelta: 1, completionRateDelta: 25 },
    recentOccurrences: [{ occurrenceDate: '2026-03-26', occurrenceTime: '08:00', status: 'done', dayCategory: 'WORKDAY' }]
  };
}
