import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  loadingSetupStatus = true;
  setupRequired = false;
  submitting = false;
  errorMessage = '';

  loginForm = {
    username: '',
    password: '',
  };

  setupForm = {
    fullName: '',
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  };

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.authService.getSetupStatus().subscribe({
      next: (status) => {
        this.setupRequired = status.setupRequired;
        this.loadingSetupStatus = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.setupRequired = false;
        this.loadingSetupStatus = false;
        this.cdr.detectChanges();
      },
    });
  }

  submitLogin(): void {
    this.errorMessage = '';
    if (!this.loginForm.username.trim() || !this.loginForm.password) {
      this.errorMessage = 'Please enter both username and password.';
      return;
    }

    this.submitting = true;
    this.cdr.detectChanges();

    this.authService
      .login({
        username: this.loginForm.username.trim(),
        password: this.loginForm.password,
      })
      .subscribe({
        next: () => {
          this.submitting = false;
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.submitting = false;
          this.errorMessage = this.extractErrorMessage(
            err,
            'Invalid username or password.'
          );
          this.cdr.detectChanges();
        },
      });
  }

  submitSetupAdmin(): void {
    this.errorMessage = '';
    if (
      !this.setupForm.fullName.trim() ||
      this.setupForm.username.trim().length < 3 ||
      !this.setupForm.email.trim().includes('@')
    ) {
      this.errorMessage =
        'Please enter full name, a username (min 3 characters), and a valid email.';
      return;
    }
    if (this.setupForm.password.length < 6) {
      this.errorMessage = 'Password must be at least 6 characters long.';
      return;
    }
    if (this.setupForm.password !== this.setupForm.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    this.submitting = true;
    this.cdr.detectChanges();

    this.authService
      .setupAdmin({
        fullName: this.setupForm.fullName.trim(),
        username: this.setupForm.username.trim(),
        email: this.setupForm.email.trim(),
        password: this.setupForm.password,
      })
      .subscribe({
        next: () => {
          this.submitting = false;
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.submitting = false;
          this.errorMessage = this.extractErrorMessage(
            err,
            'Unable to create initial administrator account.'
          );
          this.cdr.detectChanges();
        },
      });
  }

  private extractErrorMessage(err: unknown, fallback: string): string {
    if (
      err &&
      typeof err === 'object' &&
      'error' in err &&
      err.error &&
      typeof err.error === 'object' &&
      'message' in err.error &&
      typeof err.error.message === 'string' &&
      err.error.message.trim()
    ) {
      return err.error.message;
    }
    return fallback;
  }
}
