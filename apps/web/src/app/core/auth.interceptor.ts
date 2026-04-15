import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse, HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Observable, EMPTY, catchError, shareReplay, switchMap, throwError } from 'rxjs';

const PUBLIC_AUTH_PATHS = [
  '/api/v1/auth/login',
  '/api/v1/auth/register',
  '/api/v1/auth/activate',
  '/api/v1/auth/password-reset/request',
  '/api/v1/auth/password-reset/confirm',
  '/api/v1/auth/refresh'
];

let refresh$: Observable<string> | null = null;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const http = inject(HttpClient);

  if (PUBLIC_AUTH_PATHS.some((path) => req.url.includes(path))) {
    return next(req);
  }

  const attachToken = (token: string | null) =>
    token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(attachToken(authService.getAccessToken())).pipe(
    catchError((err) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
        return throwError(() => err);
      }

      if (!refresh$) {
        refresh$ = http
          .post<{ data: { accessToken: string } }>(
            '/api/v1/auth/refresh',
            null,
            { withCredentials: true }
          )
          .pipe(
            switchMap((res) => {
              authService.setAccessToken(res.data.accessToken);
              refresh$ = null;
              return [res.data.accessToken];
            }),
            catchError(() => {
              refresh$ = null;
              authService.clearAccessToken();
              router.navigateByUrl('/login');
              return EMPTY;
            }),
            shareReplay(1)
          );
      }

      return refresh$.pipe(
        switchMap((newToken) => next(attachToken(newToken)))
      );
    })
  );
};
