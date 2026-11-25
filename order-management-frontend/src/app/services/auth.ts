import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import {
  AuthResponse,
  AuthUser,
  LoginRequest,
  SetupAdminRequest,
  SetupStatusResponse,
  UserRole,
} from '../models/auth';

const TOKEN_STORAGE_KEY = 'ordercraft_auth_token';
const USER_STORAGE_KEY = 'ordercraft_auth_user';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';

  readonly currentUser = signal<AuthUser | null>(this.loadInitialUser());
  readonly isLoggedIn = computed(
    () => this.currentUser() !== null && this.getToken() !== null
  );

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  getSetupStatus(): Observable<SetupStatusResponse> {
    return this.http.get<SetupStatusResponse>(`${this.apiUrl}/setup-status`);
  }

  setupAdmin(payload: SetupAdminRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/setup-admin`, payload)
      .pipe(tap((res) => this.persistSession(res)));
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, payload)
      .pipe(tap((res) => this.persistSession(res)));
  }

  fetchCurrentUser(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${this.apiUrl}/me`).pipe(
      tap((user) => {
        this.currentUser.set(user);
        this.safeSetItem(USER_STORAGE_KEY, JSON.stringify(user));
      })
    );
  }

  logout(redirectToLogin = true): void {
    this.clearSession();
    if (redirectToLogin) {
      this.router.navigate(['/login']);
    }
  }

  getToken(): string | null {
    const token = this.safeGetItem(TOKEN_STORAGE_KEY);
    if (!token) {
      return null;
    }
    if (this.isTokenExpired(token)) {
      this.clearSession();
      return null;
    }
    return token;
  }

  hasAnyRole(...roles: UserRole[]): boolean {
    const user = this.currentUser();
    if (!user) {
      return false;
    }
    return roles.includes(user.role);
  }

  private persistSession(res: AuthResponse): void {
    this.safeSetItem(TOKEN_STORAGE_KEY, res.token);
    this.safeSetItem(USER_STORAGE_KEY, JSON.stringify(res.user));
    this.currentUser.set(res.user);
  }

  private clearSession(): void {
    this.safeRemoveItem(TOKEN_STORAGE_KEY);
    this.safeRemoveItem(USER_STORAGE_KEY);
    this.currentUser.set(null);
  }

  private loadInitialUser(): AuthUser | null {
    const token = this.safeGetItem(TOKEN_STORAGE_KEY);
    const rawUser = this.safeGetItem(USER_STORAGE_KEY);
    if (!token || !rawUser || this.isTokenExpired(token)) {
      this.safeRemoveItem(TOKEN_STORAGE_KEY);
      this.safeRemoveItem(USER_STORAGE_KEY);
      return null;
    }
    try {
      return JSON.parse(rawUser) as AuthUser;
    } catch {
      return null;
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return true;
      }
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64)) as { exp?: number };
      if (!payload.exp) {
        return true;
      }
      return Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }

  private safeGetItem(key: string): string | null {
    try {
      return typeof localStorage !== 'undefined' ? localStorage.getItem(key) : null;
    } catch {
      return null;
    }
  }

  private safeSetItem(key: string, value: string): void {
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(key, value);
      }
    } catch {}
  }

  private safeRemoveItem(key: string): void {
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.removeItem(key);
      }
    } catch {}
  }
}
