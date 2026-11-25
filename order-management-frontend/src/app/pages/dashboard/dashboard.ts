import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { DashboardService } from '../../services/dashboard';
import { AuthService } from '../../services/auth';
import { DashboardSummary } from '../../models/dashboard';
import { UserRole } from '../../models/auth';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  summary: DashboardSummary | null = null;
  loading = false;
  errorMessage = '';

  constructor(
    private dashboardService: DashboardService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadSummary();
  }

  loadSummary(): void {
    this.loading = true;
    this.errorMessage = '';

    this.dashboardService.getSummary().subscribe({
      next: (data) => {
        this.summary = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to load dashboard analytics.'
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  canAccess(...roles: UserRole[]): boolean {
    return this.authService.hasAnyRole(...roles);
  }

  getStatusClass(status: string): string {
    switch ((status || '').toUpperCase()) {
      case 'NEW':
      case 'DRAFT':
        return 'badge-new';
      case 'IN_PROGRESS':
      case 'SENT':
        return 'badge-progress';
      case 'COMPLETED':
      case 'PAID':
        return 'badge-success';
      case 'OVERDUE':
        return 'badge-warning';
      case 'CANCELLED':
        return 'badge-danger';
      default:
        return 'badge-new';
    }
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
