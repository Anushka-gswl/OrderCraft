import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Invoice, InvoiceRequest } from '../models/invoice';

@Injectable({
  providedIn: 'root',
})
export class InvoiceService {
  private apiUrl = 'http://localhost:8080/api/invoices';

  constructor(private http: HttpClient) {}

  getInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(this.apiUrl);
  }

  getInvoiceById(id: number): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.apiUrl}/${id}`);
  }

  getInvoiceByOrderId(orderId: number): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.apiUrl}/order/${orderId}`);
  }

  generateInvoice(orderId: number, request: InvoiceRequest): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.apiUrl}?orderId=${orderId}`, request);
  }

  updateStatus(id: number, status: string): Observable<Invoice> {
    return this.http.put<Invoice>(`${this.apiUrl}/${id}/status`, { status });
  }

  deleteInvoice(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
