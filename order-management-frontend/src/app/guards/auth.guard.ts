import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth';
import { UserRole } from '../models/auth';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};

export const guestOnlyGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return router.createUrlTree(['/dashboard']);
  }
  return true;
};

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }

  const allowedRoles = (route.data?.['roles'] as UserRole[] | undefined) || [];
  if (allowedRoles.length === 0 || authService.hasAnyRole(...allowedRoles)) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
