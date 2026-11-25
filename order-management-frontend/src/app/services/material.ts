import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Material } from '../models/material';

@Injectable({
  providedIn: 'root'
})
export class MaterialService {

  private apiUrl = 'http://localhost:8080/api/materials';

  constructor(private http: HttpClient) {}

  getMaterials(): Observable<Material[]> {
    return this.http.get<Material[]>(this.apiUrl);
  }

  createMaterial(material: Material): Observable<Material> {
    return this.http.post<Material>(this.apiUrl, material);
  }

  deleteMaterial(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}