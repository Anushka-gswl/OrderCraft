import { Customer } from './customer';
import { Product } from './product';
import { PurchaseOrder } from './purchase-order';

export interface CustomerOrder {
  orderId?: number;
  orderNumber: string;
  customer: Customer;
  orderDate?: string;
  status: string;
  totalAmount: number;
}

export interface OrderItem {
  orderItemId?: number;
  order?: CustomerOrder;
  product: Product;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface CustomerOrderDetails extends CustomerOrder {
  items: OrderItem[];
}

export interface MaterialRequirementItem {
  materialId: number;
  materialCode: string;
  materialName: string;
  unit: string;
  requiredQuantity: number;
  availableQuantity: number;
  shortageQuantity: number;
  unitCost: number;
  status: string;
}

export interface OrderProcessingSummary {
  orderId: number;
  orderNumber: string;
  status: string;
  hasOrderItems: boolean;
  sufficientInventory: boolean;
  activeCreatedPoExists: boolean;
  canGeneratePurchaseOrder: boolean;
  canStartProcessing: boolean;
  canComplete: boolean;
  canCancel: boolean;
  productsWithoutBom: string[];
  requirements: MaterialRequirementItem[];
  linkedPurchaseOrders: PurchaseOrder[];
}

export interface OrderItemRequest {
  productId: number;
  quantity: number;
}

export interface CustomerOrderRequest {
  orderNumber?: string;
  customerId: number;
  idempotencyKey?: string;
  items: OrderItemRequest[];
}
