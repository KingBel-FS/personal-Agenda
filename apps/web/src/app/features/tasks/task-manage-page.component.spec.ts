import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { authInterceptor } from '../../core/auth.interceptor';
import { TaskManagePageComponent } from './task-manage-page.component';

describe('TaskManagePageComponent', () => {
  let httpTesting: HttpTestingController;
  let todayIso: string;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskManagePageComponent],
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    TestBed.inject(AuthService).setAccessToken('fake-jwt');
    httpTesting = TestBed.inject(HttpTestingController);
    const now = new Date();
    todayIso = `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}-${`${now.getDate()}`.padStart(2, '0')}`;
  });

  afterEach(() => httpTesting.verify());

  function futureOccurrence() {
    return {
      id: 'occ-1',
      taskDefinitionId: 'def-1',
      taskRuleId: 'rule-1',
      title: 'Sport',
      icon: '🏃',
      description: null,
      taskType: 'RECURRING',
      timeMode: 'WAKE_UP_OFFSET',
      fixedTime: null,
      wakeUpOffsetMinutes: 90,
      dayCategories: ['WORKDAY', 'VACATION'],
      recurrenceType: 'WEEKLY',
      daysOfWeek: [1, 3, 5],
      dayOfMonth: null,
      endDate: '2026-06-30',
      occurrenceDate: '2026-03-28',
      occurrenceTime: '08:00:00',
      status: 'planned',
      dayCategory: 'WORKDAY',
      pastLocked: false,
      recurring: true,
      futureScopeAvailable: true,
      slotOrder: null,
      totalSlotsPerDay: 1,
      timeSlots: [] as Array<{ timeMode: string; fixedTime: string | null; afterPreviousMinutes: number | null }>
    };
  }

  function pagePayload(items = [futureOccurrence()]) {
    return {
      data: {
        items,
        page: 0,
        size: 10,
        totalElements: items.length,
        totalPages: 1
      }
    };
  }

  it('loads future occurrences on init with default backend pagination', () => {
    const fixture = TestBed.createComponent(TaskManagePageComponent);
    fixture.detectChanges();

    const req = httpTesting.expectOne((request) => request.url === '/api/v1/tasks/occurrences');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('occurrenceDateFrom')).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    req.flush(pagePayload([futureOccurrence(), { ...futureOccurrence(), id: 'occ-2', occurrenceDate: '2026-03-30' }]));

    const component = fixture.componentInstance as any;
    expect(component.occurrences().length).toBe(2);
  });

  it('sends filters for search recurring end date and fixed time', () => {
    const fixture = TestBed.createComponent(TaskManagePageComponent);
    fixture.detectChanges();
    httpTesting.expectOne((request) =>
      request.url === '/api/v1/tasks/occurrences' && request.params.get('occurrenceDateFrom') === todayIso
    ).flush(pagePayload());

    const component = fixture.componentInstance as any;
    component.filterForm.patchValue({
      search: 'Sport',
      taskKind: 'RECURRING_AFTER_DATE',
      selectedDate: '2026-06-01',
      timeMode: 'FIXED',
      fixedTime: '08:30',
      occurrenceDateFrom: '2026-03-01',
      occurrenceDateTo: '2026-12-31'
    });

    component.applyFilters();

    const req = httpTesting.expectOne((request) => request.url === '/api/v1/tasks/occurrences');
    expect(req.request.params.get('search')).toBe('Sport');
    expect(req.request.params.get('taskKind')).toBe('RECURRING_AFTER_DATE');
    expect(req.request.params.get('selectedDate')).toBe('2026-06-01');
    expect(req.request.params.get('timeMode')).toBe('FIXED');
    expect(req.request.params.get('fixedTime')).toBe('08:30');
    expect(req.request.params.get('occurrenceDateFrom')).toBe('2026-03-01');
    expect(req.request.params.get('occurrenceDateTo')).toBe('2026-12-31');
    req.flush(pagePayload());
  });

  it('opens the scope sheet for a future occurrence and keeps recurring settings', () => {
    const fixture = TestBed.createComponent(TaskManagePageComponent);
    fixture.detectChanges();
    httpTesting.expectOne((request) =>
      request.url === '/api/v1/tasks/occurrences' && request.params.get('occurrenceDateFrom') === todayIso
    ).flush(
      pagePayload([futureOccurrence(), { ...futureOccurrence(), id: 'occ-2', occurrenceDate: '2026-03-30' }])
    );

    const component = fixture.componentInstance as any;
    component.openScopeSheet('edit', component.occurrences()[0]);

    expect(component.scopeSheetOpen()).toBeTrue();
    expect(component.editForm.controls.timeMode.value).toBe('WAKE_UP_OFFSET');
    expect(component.editForm.controls.wakeUpOffsetHours.value).toBe(1);
    expect(component.editForm.controls.wakeUpOffsetMins.value).toBe(30);
    expect(component.editForm.controls.endDate.value).toBe('2026-06-30');
  });

  it('sends a scoped edit payload with recurring settings', fakeAsync(() => {
    const fixture = TestBed.createComponent(TaskManagePageComponent);
    fixture.detectChanges();
    httpTesting.expectOne((request) =>
      request.url === '/api/v1/tasks/occurrences' && request.params.get('occurrenceDateFrom') === todayIso
    ).flush(
      pagePayload([futureOccurrence(), { ...futureOccurrence(), id: 'occ-2', occurrenceDate: '2026-03-30' }])
    );

    const component = fixture.componentInstance as any;
    component.openScopeSheet('edit', component.occurrences()[0]);
    component.selectedScope.set('THIS_AND_FOLLOWING');
    component.editForm.patchValue({
      title: 'Sport matin',
      icon: '🔥',
      WORKDAY: false,
      VACATION: true,
      WEEKEND_HOLIDAY: true,
      recurrenceType: 'WEEKLY',
      dow1: false,
      dow2: true,
      dow3: false,
      dow4: true,
      dow5: false,
      dow6: true,
      dow7: false,
      endDate: '2026-07-31'
    });

    component.confirmMutation();

    const req = httpTesting.expectOne('/api/v1/tasks/occurrences/occ-1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.scope).toBe('THIS_AND_FOLLOWING');
    expect(req.request.body.dayCategories).toEqual(['VACATION', 'WEEKEND_HOLIDAY']);
    expect(req.request.body.daysOfWeek).toEqual([2, 4, 6]);
    expect(req.request.body.endDate).toBe('2026-07-31');
    req.flush({ data: { ...futureOccurrence(), title: 'Sport matin', icon: '🔥', occurrenceTime: '09:30:00' } });

    httpTesting.expectOne((request) =>
      request.url === '/api/v1/tasks/occurrences' && request.params.get('occurrenceDateFrom') === todayIso
    ).flush(
      pagePayload([{ ...futureOccurrence(), title: 'Sport matin', icon: '🔥', occurrenceTime: '09:30:00' }])
    );

    tick();
    expect(component.successMessage()).toContain('suivantes');
  }));

  it('loads existing slots into timeSlotsArray when opening scope sheet', () => {
    const fixture = TestBed.createComponent(TaskManagePageComponent);
    fixture.detectChanges();

    const occWithSlots = {
      ...futureOccurrence(),
      timeSlots: [
        { timeMode: 'EVERY_N_MINUTES', fixedTime: null, afterPreviousMinutes: 90 },
        { timeMode: 'FIXED', fixedTime: '14:30:00', afterPreviousMinutes: null }
      ]
    };
    httpTesting.expectOne((request) =>
      request.url === '/api/v1/tasks/occurrences' && request.params.get('occurrenceDateFrom') === todayIso
    ).flush(pagePayload([occWithSlots]));

    const component = fixture.componentInstance as any;
    component.openScopeSheet('edit', component.occurrences()[0]);

    expect(component.slots.length).toBe(2);

    const slot0 = component.slots.at(0);
    expect(slot0.controls.timeMode.value).toBe('EVERY_N_MINUTES');
    expect(slot0.controls.intervalHours.value).toBe(1);
    expect(slot0.controls.intervalMins.value).toBe(30);

    const slot1 = component.slots.at(1);
    expect(slot1.controls.timeMode.value).toBe('FIXED');
    expect(slot1.controls.fixedTime.value).toBe('14:30');
  });

  it('sends timeSlots in THIS_AND_FOLLOWING payload', fakeAsync(() => {
    const fixture = TestBed.createComponent(TaskManagePageComponent);
    fixture.detectChanges();
    httpTesting.expectOne((request) =>
      request.url === '/api/v1/tasks/occurrences' && request.params.get('occurrenceDateFrom') === todayIso
    ).flush(pagePayload());

    const component = fixture.componentInstance as any;
    component.openScopeSheet('edit', component.occurrences()[0]);
    component.selectedScope.set('THIS_AND_FOLLOWING');

    component.addTimeSlot();
    component.slots.at(0).patchValue({ timeMode: 'FIXED', fixedTime: '09:00' });

    component.editForm.patchValue({ WORKDAY: true, dow1: true, endDate: '2026-12-31' });

    component.confirmMutation();

    const req = httpTesting.expectOne('/api/v1/tasks/occurrences/occ-1');
    expect(req.request.body.timeSlots).toEqual([
      { timeMode: 'FIXED', fixedTime: '09:00', afterPreviousMinutes: null }
    ]);
    req.flush({ data: futureOccurrence() });

    httpTesting.expectOne((request) => request.url === '/api/v1/tasks/occurrences').flush(pagePayload());
    tick();
  }));

  it('shows a french toast for business rule violations', () => {
    const fixture = TestBed.createComponent(TaskManagePageComponent);
    fixture.detectChanges();
    httpTesting.expectOne((request) =>
      request.url === '/api/v1/tasks/occurrences' && request.params.get('occurrenceDateFrom') === todayIso
    ).flush(pagePayload());

    const component = fixture.componentInstance as any;
    component.openScopeSheet('delete', component.occurrences()[0]);
    component.confirmMutation();

    const req = httpTesting.expectOne('/api/v1/tasks/occurrences/occ-1');
    req.flush(
      { error: { code: 'BUSINESS_RULE_VIOLATION', message: 'Cette occurrence ne peut pas être supprimée.' } },
      { status: 400, statusText: 'Bad Request' }
    );

    expect(component.toastMessage()).toContain('Action impossible');
    expect(component.toastMessage()).toContain('Cette occurrence ne peut pas être supprimée.');
  });
});
