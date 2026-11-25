export type UserRole =
  | 'ADMIN'
  | 'SALES_EXECUTIVE'
  | 'INVENTORY_MANAGER'
  | 'PROCUREMENT';

export interface AuthUser {
  userId: number;
  username: string;
  fullName: string;
  email: string;
  role: UserRole;
  active: boolean;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  user: AuthUser;
}

export interface SetupStatusResponse {
  setupRequired: boolean;
  totalUsers: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface SetupAdminRequest {
  username: string;
  password: string;
  fullName: string;
  email: string;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  fullName: string;
  email: string;
  role: UserRole;
}

export interface UpdateUserRequest {
  fullName?: string;
  email?: string;
  role?: UserRole;
  active?: boolean;
  password?: string;
}
