import { CustomerOrder } from './order';
import { Material } from './material';

export interface PurchaseOrder {
  purchaseOrderId?: number;
  poNumber: string;
  supplierName: string;
  customerOrder?: CustomerOrder | null;
  orderDate?: string;
  status: string;
  totalAmount: number;
}

export interface PurchaseOrderItem {
  purchaseOrderItemId?: number;
  purchaseOrder?: PurchaseOrder;
  material: Material;
  quantity: number;
  unitCost: number;
  subtotal: number;
}

export interface PurchaseOrderDetails extends PurchaseOrder {
  items: PurchaseOrderItem[];
}

export interface PurchaseOrderItemRequest {
  materialId: number;
  quantity: number;
}

export interface PurchaseOrderRequest {
  poNumber?: string;
  supplierName: string;
  customerOrderId?: number | null;
  items: PurchaseOrderItemRequest[];
}
