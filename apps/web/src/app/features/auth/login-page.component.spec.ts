import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LoginPageComponent } from './login-page.component';
import { AuthService } from '../../core/auth.service';
import { authInterceptor } from '../../core/auth.interceptor';

describe('LoginPageComponent', () => {
  let httpTesting: HttpTestingController;
  let router: Router;
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpTesting.verify();
    authService.clearAccessToken();
  });

  it('logs in and stores the access token', fakeAsync(() => {
    spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    const fixture = TestBed.createComponent(LoginPageComponent);
    const component = fixture.componentInstance as any;
    fixture.detectChanges();

    component.form.setValue({
      email: 'charlie@example.com',
      password: 'Password456'
    });

    component.submit();

    const req = httpTesting.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.email).toBe('charlie@example.com');

    req.flush({
      data: {
        accessToken: 'jwt-token',
        expiresInSeconds: 900
      }
    });
    tick();

    expect(authService.getAccessToken()).toBe('jwt-token');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/today');
  }));

  it('shows the API auth error when login fails', () => {
    const fixture = TestBed.createComponent(LoginPageComponent);
    const component = fixture.componentInstance as any;
    fixture.detectChanges();

    component.form.setValue({
      email: 'charlie@example.com',
      password: 'Badpass1'
    });

    component.submit();

    const req = httpTesting.expectOne('/api/v1/auth/login');
    req.flush(
      {
        error: {
          message: 'Invalid credentials'
        }
      },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(component.errorMessage()).toBe('Invalid credentials');
  });
});
