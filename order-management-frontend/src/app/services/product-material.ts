import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  ProductMaterial,
  ProductMaterialRequest,
} from '../models/product-material';

@Injectable({
  providedIn: 'root',
})
export class ProductMaterialService {
  private apiUrl = 'http://localhost:8080/api/product-materials';

  constructor(private http: HttpClient) {}

  getAllProductMaterials(): Observable<ProductMaterial[]> {
    return this.http.get<ProductMaterial[]>(this.apiUrl);
  }

  getByProductId(productId: number): Observable<ProductMaterial[]> {
    return this.http.get<ProductMaterial[]>(`${this.apiUrl}/product/${productId}`);
  }

  createProductMaterial(
    payload: ProductMaterialRequest
  ): Observable<ProductMaterial> {
    return this.http.post<ProductMaterial>(this.apiUrl, payload);
  }

  updateProductMaterial(
    id: number,
    quantityRequired: number
  ): Observable<ProductMaterial> {
    return this.http.put<ProductMaterial>(`${this.apiUrl}/${id}`, {
      quantityRequired,
    });
  }

  deleteProductMaterial(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
