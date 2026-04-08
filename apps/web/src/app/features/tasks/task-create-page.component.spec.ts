import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { ComponentFixture } from '@angular/core/testing';
import { TaskCreatePageComponent } from './task-create-page.component';
import { authInterceptor } from '../../core/auth.interceptor';
import { AuthService } from '../../core/auth.service';

describe('TaskCreatePageComponent', () => {
  let fixture: ComponentFixture<TaskCreatePageComponent>;
  let component: any;
  let httpTesting: HttpTestingController;
  let router: Router;

  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const tomorrowStr = tomorrow.toISOString().split('T')[0];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskCreatePageComponent],
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    const authService = TestBed.inject(AuthService);
    authService.setAccessToken('fake-jwt');

    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);

    fixture = TestBed.createComponent(TaskCreatePageComponent);
    component = fixture.componentInstance as any;
    fixture.detectChanges();
  });

  afterEach(() => httpTesting.verify());

  it('starts on step 1', () => {
    expect(component.currentStep()).toBe(1);
  });

  it('advances to step 2 when step 1 is valid', () => {
    component.infoForm.setValue({ title: 'Meditation', icon: '🧘', description: '' });
    component.nextStep();
    expect(component.currentStep()).toBe(2);
  });

  it('does not advance from step 1 when title is empty', () => {
    component.infoForm.setValue({ title: '', icon: '🧘', description: '' });
    component.nextStep();
    expect(component.currentStep()).toBe(1);
  });

  it('rejects past date on step 3', () => {
    component.currentStep.set(3);
    component.planningForm.controls['WORKDAY'].setValue(true);

    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    component.planningForm.controls.startDate.setValue(yesterday.toISOString().split('T')[0]);

    component.nextStep();

    expect(component.errorMessage()).toContain('passé');
    expect(component.currentStep()).toBe(3);
  });

  it('calls preview endpoint when advancing from step 4 and moves to step 5', fakeAsync(() => {
    component.infoForm.setValue({ title: 'Meditation', icon: '🧘', description: '' });
    component.planningForm.controls['WORKDAY'].setValue(true);
    component.planningForm.controls.startDate.setValue(tomorrowStr);
    component.scheduleForm.setValue({ timeMode: 'FIXED', fixedTime: '08:00', wakeUpOffsetHours: 0, wakeUpOffsetMins: 30 });
    component.currentStep.set(4);

    component.nextStep();

    const req = httpTesting.expectOne('/api/v1/tasks/preview');
    req.flush({
      data: { occurrenceDate: tomorrowStr, occurrenceTime: '08:00', occurrenceLabel: 'Le 25/03/2026 a 08:00' }
    });

    tick();
    expect(component.currentStep()).toBe(5);
    expect(component.preview()?.occurrenceLabel).toBe('Le 25/03/2026 a 08:00');
  }));

  it('submits task and navigates to task management', fakeAsync(() => {
    const navigateSpy = spyOn(router, 'navigateByUrl');

    component.infoForm.setValue({ title: 'Meditation', icon: '🧘', description: '' });
    component.planningForm.controls['WORKDAY'].setValue(true);
    component.planningForm.controls.startDate.setValue(tomorrowStr);
    component.scheduleForm.setValue({ timeMode: 'FIXED', fixedTime: '08:00', wakeUpOffsetHours: 0, wakeUpOffsetMins: 30 });
    component.currentStep.set(5);
    component.preview.set({ occurrenceDate: tomorrowStr, occurrenceTime: '08:00', occurrenceLabel: 'Le 25/03/2026 a 08:00' });

    component.submit();

    const req = httpTesting.expectOne('/api/v1/tasks');
    req.flush({ data: { id: 'task-1', title: 'Meditation', icon: '🧘', taskType: 'ONE_TIME' } });

    tick();
    expect(navigateSpy).toHaveBeenCalledWith('/tasks/manage');
  }));

  it('shows error message when creation fails', fakeAsync(() => {
    component.infoForm.setValue({ title: 'Meditation', icon: '🧘', description: '' });
    component.planningForm.controls['WORKDAY'].setValue(true);
    component.planningForm.controls.startDate.setValue(tomorrowStr);
    component.scheduleForm.setValue({ timeMode: 'FIXED', fixedTime: '08:00', wakeUpOffsetHours: 0, wakeUpOffsetMins: 30 });
    component.currentStep.set(5);
    component.preview.set({ occurrenceDate: tomorrowStr, occurrenceTime: '08:00', occurrenceLabel: 'test' });

    component.submit();

    const req = httpTesting.expectOne('/api/v1/tasks');
    req.flush({ error: { code: 'BUSINESS_RULE_VIOLATION', message: 'Date invalide.' } }, { status: 400, statusText: 'Bad Request' });

    tick();
    expect(component.errorMessage()).toBeTruthy();
  }));
});
