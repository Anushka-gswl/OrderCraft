import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  PurchaseOrder,
  PurchaseOrderDetails,
  PurchaseOrderItem,
  PurchaseOrderRequest,
} from '../models/purchase-order';

@Injectable({
  providedIn: 'root',
})
export class PurchaseOrderService {
  private apiUrl = 'http://localhost:8080/api/purchase-orders';
  private itemsUrl = 'http://localhost:8080/api/purchase-order-items';

  constructor(private http: HttpClient) {}

  getPurchaseOrders(): Observable<PurchaseOrder[]> {
    return this.http.get<PurchaseOrder[]>(this.apiUrl);
  }

  getPurchaseOrderById(id: number): Observable<PurchaseOrder> {
    return this.http.get<PurchaseOrder>(`${this.apiUrl}/${id}`);
  }

  getPurchaseOrderDetails(id: number): Observable<PurchaseOrderDetails> {
    return this.http.get<PurchaseOrderDetails>(`${this.apiUrl}/${id}/details`);
  }

  createPurchaseOrder(payload: PurchaseOrderRequest): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(this.apiUrl, payload);
  }

  updatePurchaseOrder(
    id: number,
    payload: PurchaseOrderRequest
  ): Observable<PurchaseOrder> {
    return this.http.put<PurchaseOrder>(`${this.apiUrl}/${id}`, payload);
  }

  receivePurchaseOrder(id: number): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.apiUrl}/${id}/receive`, {});
  }

  deletePurchaseOrder(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getItemsByPurchaseOrderId(
    purchaseOrderId: number
  ): Observable<PurchaseOrderItem[]> {
    return this.http.get<PurchaseOrderItem[]>(
      `${this.itemsUrl}/purchase-order/${purchaseOrderId}`
    );
  }
}
