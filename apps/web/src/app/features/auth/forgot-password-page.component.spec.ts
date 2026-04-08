import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ForgotPasswordPageComponent } from './forgot-password-page.component';

describe('ForgotPasswordPageComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPasswordPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests a password reset email', () => {
    const fixture = TestBed.createComponent(ForgotPasswordPageComponent);
    const component = fixture.componentInstance as any;
    fixture.detectChanges();

    component.form.setValue({ email: 'charlie@example.com' });
    component.submit();

    const req = httpTesting.expectOne('/api/v1/auth/password-reset/request');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.email).toBe('charlie@example.com');
    req.flush({ data: { message: 'If the account exists, a reset email has been sent.' } });

    expect(component.successMessage()).toContain('reset email');
  });
});
