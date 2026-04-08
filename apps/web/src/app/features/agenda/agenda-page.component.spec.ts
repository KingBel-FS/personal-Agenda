import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AgendaPageComponent } from './agenda-page.component';
import { authInterceptor } from '../../core/auth.interceptor';
import { AuthService } from '../../core/auth.service';

function buildRange(view: 'week' | 'month', days: Array<Record<string, unknown>>) {
  return {
    view,
    anchorDate: '2026-04-15',
    rangeStart: view === 'week' ? '2026-04-13' : '2026-03-30',
    rangeEnd: view === 'week' ? '2026-04-19' : '2026-05-03',
    days
  };
}

function day(date: string, overrides: Record<string, unknown> = {}) {
  return {
    date,
    currentMonth: true,
    past: false,
    today: false,
    beforeAccountCreation: false,
    dayCategory: 'WORKDAY',
    totalCount: 2,
    plannedCount: 1,
    doneCount: 1,
    missedCount: 0,
    skippedCount: 0,
    statusTone: 'mixed',
    icons: ['📚'],
    ...overrides
  };
}

function daily(date: string, overrides: Record<string, unknown> = {}) {
  return {
    date,
    dayCategory: 'WORKDAY',
    activeCount: 1,
    skippedCount: 0,
    doneCount: 1,
    missedCount: 0,
    totalCount: 2,
    progressPercent: 50,
    streak: { currentStreak: 2, longestStreak: 3, streakActive: true, badges: [] },
    newBadges: [],
    occurrences: [
      {
        id: 'occ-1',
        taskDefinitionId: 'task-1',
        title: 'Lecture',
        description: '25 minutes',
        icon: '📚',
        occurrenceTime: '08:00',
        status: 'planned',
        dayCategory: 'WORKDAY',
        recurring: false,
        slotOrder: null,
        totalSlotsPerDay: 1
      }
    ],
    ...overrides
  };
}

describe('AgendaPageComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgendaPageComponent],
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    TestBed.inject(AuthService).setAccessToken('test-token');
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    TestBed.inject(AuthService).clearAccessToken();
  });

  it('loads the month agenda and selected day detail on init', () => {
    const fixture = TestBed.createComponent(AgendaPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne((request) => request.url === '/api/v1/agenda/month').flush({
      data: buildRange('month', [day('2026-04-15', { today: true })])
    });
    httpTesting.expectOne((request) => request.url === '/api/v1/today/daily').flush({
      data: daily('2026-04-15')
    });

    const component = fixture.componentInstance as any;
    expect(component.viewMode()).toBe('month');
    expect(component.detail()?.occurrences.length).toBe(1);
  });

  it('switches to week view and reloads the agenda', () => {
    const fixture = TestBed.createComponent(AgendaPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne((request) => request.url === '/api/v1/agenda/month').flush({
      data: buildRange('month', [day('2026-04-15')])
    });
    httpTesting.expectOne((request) => request.url === '/api/v1/today/daily').flush({
      data: daily('2026-04-15')
    });

    const component = fixture.componentInstance as any;
    component.setViewMode('week');

    httpTesting.expectOne((request) => request.url === '/api/v1/agenda/week').flush({
      data: buildRange('week', [day('2026-04-15')])
    });
    httpTesting.expectOne((request) => request.url === '/api/v1/today/daily').flush({
      data: daily('2026-04-15')
    });

    expect(component.viewMode()).toBe('week');
  });

  it('marks past days as read only in the detail pane', () => {
    const fixture = TestBed.createComponent(AgendaPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne((request) => request.url === '/api/v1/agenda/month').flush({
      data: buildRange('month', [day('2026-04-15', { past: true })])
    });
    httpTesting.expectOne((request) => request.url === '/api/v1/today/daily').flush({
      data: daily('2026-04-15')
    });

    const component = fixture.componentInstance as any;
    expect(component.range()?.days[0].past).toBeTrue();
    expect(component.readonlyMessage()).toContain("seules l'heure ainsi que le statut d'exécution restent modifiables");
  });

  it('supports keyboard navigation between days', () => {
    const fixture = TestBed.createComponent(AgendaPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne((request) => request.url === '/api/v1/agenda/month').flush({
      data: buildRange('month', [day('2026-04-15'), day('2026-04-16')])
    });
    httpTesting.expectOne((request) => request.url === '/api/v1/today/daily').flush({
      data: daily('2026-04-15')
    });

    const component = fixture.componentInstance as any;
    component.onDayKeydown(new KeyboardEvent('keydown', { key: 'ArrowRight' }), day('2026-04-15'));

    httpTesting.expectOne((request) =>
      request.url === '/api/v1/today/daily' && request.params.get('date') === '2026-04-16'
    ).flush({
      data: daily('2026-04-16')
    });

    expect(component.selectedDate()).toBe('2026-04-16');
  });
});
