import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { tap } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';

const PUBLIC_AUTH_PATHS = [
  '/api/v1/auth/login',
  '/api/v1/auth/register',
  '/api/v1/auth/activate',
  '/api/v1/auth/password-reset/request',
  '/api/v1/auth/password-reset/confirm'
];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (PUBLIC_AUTH_PATHS.some((path) => req.url.includes(path))) {
    return next(req);
  }

  const token = authService.getAccessToken();
  const request = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(request).pipe(
    tap({
      error: (err) => {
        if (err instanceof HttpErrorResponse && err.status === 401) {
          authService.clearAccessToken();
          router.navigateByUrl('/login');
        }
      }
    })
  );
};
