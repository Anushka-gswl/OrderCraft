import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';

import { App } from './app';
import { AuthService } from './services/auth';
import { AuthUser, UserRole } from './models/auth';

describe('App', () => {
  const adminUser: AuthUser = {
    userId: 1,
    username: 'sysadmin',
    fullName: 'System Admin',
    email: 'admin@ordercraft.local',
    role: 'ADMIN',
    active: true,
  };

  let authServiceMock: {
    currentUser: ReturnType<typeof signal<AuthUser | null>>;
    isLoggedIn: ReturnType<typeof signal<boolean>>;
    hasAnyRole: (...roles: UserRole[]) => boolean;
    logout: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceMock = {
      currentUser: signal<AuthUser | null>(adminUser),
      isLoggedIn: signal<boolean>(true),
      hasAnyRole: (...roles: UserRole[]) => {
        const user = authServiceMock.currentUser();
        return user ? roles.includes(user.role) : false;
      },
      logout: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title and role-filtered navigation for ADMIN', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('OrderCraft');
    expect(compiled.textContent).toContain('Users');
    expect(compiled.textContent).toContain('System Admin');
  });

  it('hides restricted navigation links for SALES_EXECUTIVE', async () => {
    authServiceMock.currentUser.set({ ...adminUser, role: 'SALES_EXECUTIVE' });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Customers');
    expect(compiled.textContent).toContain('Orders');
    expect(compiled.textContent).not.toContain('Purchase Orders');
    expect(compiled.textContent).not.toContain('Users');
  });
});
