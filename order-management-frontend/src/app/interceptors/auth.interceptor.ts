import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth';

const PUBLIC_AUTH_URLS = [
  '/api/auth/login',
  '/api/auth/setup-status',
  '/api/auth/setup-admin',
];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  const isPublicAuth = PUBLIC_AUTH_URLS.some((path) => req.url.includes(path));
  const token = authService.getToken();

  const outgoingReq =
    !isPublicAuth && token
      ? req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        })
      : req;

  return next(outgoingReq).pipe(
    catchError((err) => {
      if (err?.status === 401 && !isPublicAuth) {
        authService.logout(true);
      }
      return throwError(() => err);
    })
  );
};
