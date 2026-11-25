export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface InvoiceItem {
  invoiceItemId?: number;
  productName: string;
  productCode: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Invoice {
  invoiceId?: number;
  invoiceNumber: string;
  orderId: number;
  orderNumber: string;
  customerId: number;
  customerName: string;
  invoiceDate: string;
  dueDate?: string;
  status: InvoiceStatus;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  totalAmount: number;
  notes?: string;
  createdAt: string;
  items: InvoiceItem[];
}

export interface InvoiceRequest {
  taxRate?: number;
  dueDate?: string;
  notes?: string;
}
