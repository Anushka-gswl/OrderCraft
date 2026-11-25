import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { UserService } from '../../services/user';
import { AuthService } from '../../services/auth';
import { AuthUser, UserRole } from '../../models/auth';

@Component({
  selector: 'app-users',
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html',
  styleUrl: './users.css',
})
export class Users implements OnInit {
  users: AuthUser[] = [];
  roles: UserRole[] = [
    'ADMIN',
    'SALES_EXECUTIVE',
    'INVENTORY_MANAGER',
    'PROCUREMENT',
  ];

  loading = false;
  submitting = false;
  showForm = false;
  editingUserId: number | undefined = undefined;

  errorMessage = '';
  formErrorMessage = '';
  successMessage = '';

  userForm: {
    username: string;
    fullName: string;
    email: string;
    password: string;
    role: UserRole;
    active: boolean;
  } = {
    username: '',
    fullName: '',
    email: '',
    password: '',
    role: 'SALES_EXECUTIVE',
    active: true,
  };

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.errorMessage = '';

    this.userService.getUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to load users.'
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openCreateUserForm(): void {
    this.editingUserId = undefined;
    this.formErrorMessage = '';
    this.successMessage = '';
    this.userForm = {
      username: '',
      fullName: '',
      email: '',
      password: '',
      role: 'SALES_EXECUTIVE',
      active: true,
    };
    this.showForm = true;
    this.cdr.detectChanges();
  }

  openEditUserForm(user: AuthUser): void {
    this.editingUserId = user.userId;
    this.formErrorMessage = '';
    this.successMessage = '';
    this.userForm = {
      username: user.username,
      fullName: user.fullName,
      email: user.email,
      password: '',
      role: user.role,
      active: user.active,
    };
    this.showForm = true;
    this.cdr.detectChanges();
  }

  closeUserForm(): void {
    this.showForm = false;
    this.editingUserId = undefined;
    this.formErrorMessage = '';
    this.cdr.detectChanges();
  }

  saveUser(): void {
    this.formErrorMessage = '';
    this.successMessage = '';

    if (
      !this.userForm.fullName.trim() ||
      !this.userForm.email.trim().includes('@')
    ) {
      this.formErrorMessage = 'Please provide a valid full name and email.';
      return;
    }

    if (this.editingUserId === undefined) {
      if (this.userForm.username.trim().length < 3) {
        this.formErrorMessage = 'Username must be at least 3 characters.';
        return;
      }
      if (this.userForm.password.length < 6) {
        this.formErrorMessage = 'Password must be at least 6 characters.';
        return;
      }

      this.submitting = true;
      this.userService
        .createUser({
          username: this.userForm.username.trim(),
          fullName: this.userForm.fullName.trim(),
          email: this.userForm.email.trim(),
          password: this.userForm.password,
          role: this.userForm.role,
        })
        .subscribe({
          next: (created) => {
            this.submitting = false;
            this.showForm = false;
            this.successMessage = `User ${created.username} created successfully.`;
            this.loadUsers();
          },
          error: (err) => {
            this.submitting = false;
            this.formErrorMessage = this.extractErrorMessage(
              err,
              'Unable to create user.'
            );
            this.cdr.detectChanges();
          },
        });
    } else {
      if (this.userForm.password && this.userForm.password.length < 6) {
        this.formErrorMessage =
          'New password must be at least 6 characters when provided.';
        return;
      }

      this.submitting = true;
      this.userService
        .updateUser(this.editingUserId, {
          fullName: this.userForm.fullName.trim(),
          email: this.userForm.email.trim(),
          role: this.userForm.role,
          active: this.userForm.active,
          ...(this.userForm.password ? { password: this.userForm.password } : {}),
        })
        .subscribe({
          next: (updated) => {
            this.submitting = false;
            this.showForm = false;
            this.editingUserId = undefined;
            this.successMessage = `User ${updated.username} updated successfully.`;
            this.loadUsers();
          },
          error: (err) => {
            this.submitting = false;
            this.formErrorMessage = this.extractErrorMessage(
              err,
              'Unable to update user.'
            );
            this.cdr.detectChanges();
          },
        });
    }
  }

  deleteUser(user: AuthUser): void {
    if (this.isCurrentUser(user)) {
      return;
    }
    if (!confirm(`Delete user ${user.username}?`)) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.userService.deleteUser(user.userId).subscribe({
      next: () => {
        this.successMessage = `User ${user.username} deleted.`;
        this.loadUsers();
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to delete user.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  isCurrentUser(user: AuthUser): boolean {
    return this.authService.currentUser()?.username === user.username;
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
