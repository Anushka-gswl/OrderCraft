import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  AuthUser,
  CreateUserRequest,
  UpdateUserRequest,
} from '../models/auth';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private apiUrl = 'http://localhost:8080/api/users';

  constructor(private http: HttpClient) {}

  getUsers(): Observable<AuthUser[]> {
    return this.http.get<AuthUser[]>(this.apiUrl);
  }

  createUser(payload: CreateUserRequest): Observable<AuthUser> {
    return this.http.post<AuthUser>(this.apiUrl, payload);
  }

  updateUser(id: number, payload: UpdateUserRequest): Observable<AuthUser> {
    return this.http.put<AuthUser>(`${this.apiUrl}/${id}`, payload);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
