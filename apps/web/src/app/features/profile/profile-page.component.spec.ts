import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { ProfilePageComponent } from './profile-page.component';
import { AuthService } from '../../core/auth.service';
import { authInterceptor } from '../../core/auth.interceptor';

function profilePayload(overrides: Record<string, unknown> = {}) {
  return {
    userId: 'u1',
    pseudo: 'alice',
    email: 'alice@example.com',
    birthDate: '1995-05-12',
    geographicZone: 'METROPOLE',
    timezoneName: 'Europe/Paris',
    profilePhotoSignedUrl: 'signed-url',
    dayProfiles: [
      { dayCategory: 'WORKDAY', wakeUpTime: '07:00' },
      { dayCategory: 'VACATION', wakeUpTime: '09:00' },
      { dayCategory: 'WEEKEND_HOLIDAY', wakeUpTime: '08:30' }
    ],
    schedulingProfile: {
      geographicZone: 'METROPOLE',
      timezoneName: 'Europe/Paris',
      dayProfiles: []
    },
    holidaySyncStatus: {
      status: 'PENDING',
      lastSyncedYear: null,
      lastSyncedAt: null,
      nextRetryAt: '2026-03-24T10:15:00Z',
      lastError: 'API gouv indisponible',
      alertVisible: true
    },
    vacationPeriods: [
      {
        id: 'vac-1',
        label: 'Conges printemps',
        startDate: '2026-04-02',
        endDate: '2026-04-08'
      }
    ],
    ...overrides
  };
}

describe('ProfilePageComponent', () => {
  let httpTesting: HttpTestingController;
  let router: Router;
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfilePageComponent],
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    authService = TestBed.inject(AuthService);
    authService.setAccessToken('test-token');
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpTesting.verify();
    authService.clearAccessToken();
  });

  it('loads the profile with the bearer token and sync alert data', () => {
    const fixture = TestBed.createComponent(ProfilePageComponent);
    fixture.detectChanges();

    const req = httpTesting.expectOne('/api/v1/profile');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    req.flush({ data: profilePayload() });

    const component = fixture.componentInstance as any;
    expect(component.form.getRawValue().pseudo).toBe('alice');
    expect(component.profilePhotoUrl()).toBe('signed-url');
    expect(component.syncAlert()?.status).toBe('PENDING');
    expect(component.vacationPeriods().length).toBe(1);
  });

  it('requests zone impact when the user selects another geographic zone', () => {
    const fixture = TestBed.createComponent(ProfilePageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/profile').flush({ data: profilePayload({ profilePhotoSignedUrl: null }) });

    const component = fixture.componentInstance as any;
    component.form.controls.geographicZone.setValue('ALSACE_LORRAINE');

    const req = httpTesting.expectOne('/api/v1/profile/zone-impact');
    expect(req.request.body.geographicZone).toBe('ALSACE_LORRAINE');
    req.flush({
      data: {
        currentZone: 'METROPOLE',
        targetZone: 'ALSACE_LORRAINE',
        holidayRulesWillChange: true,
        impactMessage: 'Impact zone'
      }
    });

    expect(component.zoneImpact()?.holidayRulesWillChange).toBeTrue();
  });

  it('submits a multipart profile update payload', () => {
    const fixture = TestBed.createComponent(ProfilePageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/profile').flush({ data: profilePayload({ profilePhotoSignedUrl: null }) });

    const component = fixture.componentInstance as any;
    component.form.patchValue({
      pseudo: 'alice-updated',
      geographicZone: 'METROPOLE',
      timezoneName: 'Europe/Paris',
      workdayWakeUpTime: '07:10',
      vacationWakeUpTime: '09:10',
      weekendHolidayWakeUpTime: '08:40'
    });
    const file = new File(['image'], 'avatar.png', { type: 'image/png' });
    component.onPhotoSelected({
      target: {
        files: {
          item: () => file
        }
      }
    } as unknown as Event);

    component.submit();

    const req = httpTesting.expectOne('/api/v1/profile');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect(req.request.body.get('dayProfiles[0].dayCategory')).toBe('WORKDAY');
    expect(req.request.body.get('dayProfiles[2].wakeUpTime')).toBe('08:40');
    expect(req.request.body.get('profilePhoto')).toBe(file);
    req.flush({ data: profilePayload({ pseudo: 'alice-updated' }) });

    expect(component.successMessage()).toContain('Profil mis à jour');
  });

  it('creates a vacation period and inserts it in date order', () => {
    const fixture = TestBed.createComponent(ProfilePageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/profile').flush({ data: profilePayload() });

    const component = fixture.componentInstance as any;
    component.vacationForm.setValue({
      label: 'Vacances ete',
      startDate: '2026-03-28',
      endDate: '2026-03-31'
    });

    component.saveVacation();

    const req = httpTesting.expectOne('/api/v1/profile/vacations');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      label: 'Vacances ete',
      startDate: '2026-03-28',
      endDate: '2026-03-31'
    });
    req.flush({
      data: {
        id: 'vac-2',
        label: 'Vacances ete',
        startDate: '2026-03-28',
        endDate: '2026-03-31'
      }
    });

    expect(component.vacationPeriods().map((item: any) => item.id)).toEqual(['vac-2', 'vac-1']);
    expect(component.successMessage()).toContain('ajoutée');
  });

  it('updates and deletes a vacation period', () => {
    const fixture = TestBed.createComponent(ProfilePageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/profile').flush({ data: profilePayload() });

    const component = fixture.componentInstance as any;
    component.editVacation(component.vacationPeriods()[0]);
    component.vacationForm.patchValue({ label: 'Conges modifies' });
    component.saveVacation();

    const updateReq = httpTesting.expectOne('/api/v1/profile/vacations/vac-1');
    expect(updateReq.request.method).toBe('PUT');
    updateReq.flush({
      data: {
        id: 'vac-1',
        label: 'Conges modifies',
        startDate: '2026-04-02',
        endDate: '2026-04-08'
      }
    });

    expect(component.vacationPeriods()[0].label).toBe('Conges modifies');

    component.deleteVacation(component.vacationPeriods()[0]);

    const deleteReq = httpTesting.expectOne('/api/v1/profile/vacations/vac-1');
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush({ data: { message: 'Vacation period deleted.' } });

    expect(component.vacationPeriods().length).toBe(0);
  });

  it('deletes the account, clears the token and redirects to login', fakeAsync(() => {
    spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    const fixture = TestBed.createComponent(ProfilePageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/profile').flush({ data: profilePayload({ profilePhotoSignedUrl: null }) });

    const component = fixture.componentInstance as any;
    component.deletionForm.setValue({ confirmPassword: 'secret123' });
    component.deleteAccount();

    const req = httpTesting.expectOne('/api/v1/profile');
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body).toEqual({ confirmPassword: 'secret123' });
    req.flush({ data: { message: 'Account deleted.' } });
    tick();

    expect(authService.getAccessToken()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  }));

  it('shows an error when account deletion fails due to wrong password', () => {
    const fixture = TestBed.createComponent(ProfilePageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/profile').flush({ data: profilePayload({ profilePhotoSignedUrl: null }) });

    const component = fixture.componentInstance as any;
    component.deletionForm.setValue({ confirmPassword: 'wrongpass' });
    component.deleteAccount();

    const req = httpTesting.expectOne('/api/v1/profile');
    req.flush(
      { error: { message: 'Invalid credentials' } },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(component.deletionErrorMessage()).toBe('Invalid credentials');
    expect(authService.getAccessToken()).toBeNull();
  });

  it('logs out, clears the token and redirects to login', fakeAsync(() => {
    spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    const fixture = TestBed.createComponent(ProfilePageComponent);
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/profile').flush({ data: profilePayload({ profilePhotoSignedUrl: null }) });

    const component = fixture.componentInstance as any;
    component.logout();

    const req = httpTesting.expectOne('/api/v1/auth/logout');
    expect(req.request.method).toBe('POST');
    req.flush({ data: { message: 'Logged out.' } });
    tick();

    expect(authService.getAccessToken()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  }));
});
