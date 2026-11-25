import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Login } from './login';
import { AuthService } from '../../services/auth';

describe('Login', () => {
  let authServiceMock: {
    getSetupStatus: ReturnType<typeof vi.fn>;
    login: ReturnType<typeof vi.fn>;
    setupAdmin: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceMock = {
      getSetupStatus: vi.fn().mockReturnValue(of({ setupRequired: false, totalUsers: 1 })),
      login: vi.fn(),
      setupAdmin: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([{ path: 'dashboard', component: Login }]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();
  });

  it('renders login form when setup is not required and validates empty inputs', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    expect(comp.setupRequired).toBe(false);
    comp.submitLogin();
    expect(comp.errorMessage).toContain('username and password');
    expect(authServiceMock.login).not.toHaveBeenCalled();
  });

  it('shows initial admin setup mode when no users exist', () => {
    authServiceMock.getSetupStatus.mockReturnValue(of({ setupRequired: true, totalUsers: 0 }));

    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    expect(comp.setupRequired).toBe(true);
    comp.setupForm = {
      fullName: 'Admin',
      username: 'admin',
      email: 'admin@ordercraft.local',
      password: 'Password123',
      confirmPassword: 'Password123',
    };
    authServiceMock.setupAdmin.mockReturnValue(of({ token: 't', tokenType: 'Bearer', expiresInMs: 1000, user: {} }));

    comp.submitSetupAdmin();
    expect(authServiceMock.setupAdmin).toHaveBeenCalled();
  });

  it('displays backend error message on invalid credentials', () => {
    authServiceMock.login.mockReturnValue(
      throwError(() => ({ error: { message: 'Invalid username or password.' } }))
    );

    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.loginForm.username = 'admin';
    comp.loginForm.password = 'wrong';
    comp.submitLogin();

    expect(comp.errorMessage).toBe('Invalid username or password.');
  });
});
