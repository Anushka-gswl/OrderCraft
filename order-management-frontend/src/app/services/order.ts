import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CustomerOrder,
  CustomerOrderDetails,
  CustomerOrderRequest,
  OrderItem,
  OrderProcessingSummary,
} from '../models/order';

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private apiUrl = 'http://localhost:8080/api/orders';
  private orderItemsUrl = 'http://localhost:8080/api/order-items';

  constructor(private http: HttpClient) {}

  getOrders(): Observable<CustomerOrder[]> {
    return this.http.get<CustomerOrder[]>(this.apiUrl);
  }

  getOrderById(id: number): Observable<CustomerOrder> {
    return this.http.get<CustomerOrder>(`${this.apiUrl}/${id}`);
  }

  getOrderDetails(id: number): Observable<CustomerOrderDetails> {
    return this.http.get<CustomerOrderDetails>(`${this.apiUrl}/${id}/details`);
  }

  createOrder(
    payload: CustomerOrderRequest,
    idempotencyKey?: string
  ): Observable<CustomerOrder> {
    if (idempotencyKey) {
      return this.http.post<CustomerOrder>(this.apiUrl, payload, {
        headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }),
      });
    }
    return this.http.post<CustomerOrder>(this.apiUrl, payload);
  }

  updateOrder(id: number, payload: CustomerOrderRequest): Observable<CustomerOrder> {
    return this.http.put<CustomerOrder>(`${this.apiUrl}/${id}`, payload);
  }

  deleteOrder(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getOrderItemsByOrderId(orderId: number): Observable<OrderItem[]> {
    return this.http.get<OrderItem[]>(`${this.orderItemsUrl}/order/${orderId}`);
  }

  getShortages(orderId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/${orderId}/shortages`);
  }

  getProcessingSummary(orderId: number): Observable<OrderProcessingSummary> {
    return this.http.get<OrderProcessingSummary>(
      `${this.apiUrl}/${orderId}/processing-summary`
    );
  }

  createPurchaseOrderForShortages(orderId: number): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/${orderId}/create-purchase-order`, {});
  }

  startOrderProcessing(orderId: number): Observable<CustomerOrder> {
    return this.http.post<CustomerOrder>(
      `${this.apiUrl}/${orderId}/start-processing`,
      {}
    );
  }

  completeOrder(orderId: number): Observable<CustomerOrder> {
    return this.http.post<CustomerOrder>(`${this.apiUrl}/${orderId}/complete`, {});
  }

  cancelOrder(orderId: number): Observable<CustomerOrder> {
    return this.http.post<CustomerOrder>(`${this.apiUrl}/${orderId}/cancel`, {});
  }
}
