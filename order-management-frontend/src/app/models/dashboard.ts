export interface LowStockAlert {
  materialId: number;
  materialCode: string;
  materialName: string;
  unit: string;
  quantityAvailable: number;
  reorderLevel: number;
}

export interface RecentOrder {
  orderId: number;
  orderNumber: string;
  customerName: string;
  status: string;
  totalAmount: number;
  orderDate: string;
}

export interface RecentInvoice {
  invoiceId: number;
  invoiceNumber: string;
  orderNumber: string;
  customerName: string;
  status: string;
  totalAmount: number;
  invoiceDate: string;
}

export interface DashboardSummary {
  totalCustomers: number;
  totalProducts: number;
  activeProducts: number;
  totalMaterials: number;

  totalOrders: number;
  newOrders: number;
  inProgressOrders: number;
  completedOrders: number;
  cancelledOrders: number;

  totalPurchaseOrders: number;
  pendingPurchaseOrders: number;
  receivedPurchaseOrders: number;

  totalInvoices: number;
  paidInvoices: number;
  unpaidInvoices: number;

  totalOrderRevenue: number;
  totalInvoicedAmount: number;
  paidInvoiceAmount: number;
  outstandingInvoiceAmount: number;
  totalPurchaseSpend: number;

  lowStockAlerts: LowStockAlert[];
  recentOrders: RecentOrder[];
  recentInvoices: RecentInvoice[];
}
