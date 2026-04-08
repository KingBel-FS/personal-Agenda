import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ResetPasswordPageComponent } from './reset-password-page.component';

describe('ResetPasswordPageComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResetPasswordPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: of(convertToParamMap({ token: 'reset-token' }))
          }
        }
      ]
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('submits the reset token and new password', () => {
    const fixture = TestBed.createComponent(ResetPasswordPageComponent);
    const component = fixture.componentInstance as any;
    fixture.detectChanges();

    component.form.setValue({ newPassword: 'Password789' });
    component.submit();

    const req = httpTesting.expectOne('/api/v1/auth/password-reset/confirm');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.token).toBe('reset-token');
    req.flush({ data: { message: 'Password reset completed.' } });

    expect(component.successMessage()).toContain('Password reset completed');
  });
});
