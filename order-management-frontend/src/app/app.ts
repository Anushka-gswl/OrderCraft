import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './services/auth';
import { UserRole } from './models/auth';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('order-management-frontend');

  constructor(public authService: AuthService) {}

  canAccess(...roles: UserRole[]): boolean {
    return this.authService.hasAnyRole(...roles);
  }

  get userInitial(): string {
    const name = this.authService.currentUser()?.fullName || 'U';
    return name.trim().charAt(0).toUpperCase();
  }

  logout(): void {
    this.authService.logout(true);
  }
}
