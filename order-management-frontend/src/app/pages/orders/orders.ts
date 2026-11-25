import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { OrderService } from '../../services/order';
import { CustomerService } from '../../services/customer';
import { ProductService } from '../../services/product';
import { InvoiceService } from '../../services/invoice';
import { AuthService } from '../../services/auth';
import {
  CustomerOrder,
  CustomerOrderDetails,
  CustomerOrderRequest,
  OrderProcessingSummary,
} from '../../models/order';
import { Customer } from '../../models/customer';
import { Product } from '../../models/product';
import { Invoice } from '../../models/invoice';

interface OrderFormItemRow {
  productId: number;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

@Component({
  imports: [CommonModule, FormsModule],
  selector: 'app-orders',
  styleUrl: './orders.css',
  templateUrl: './orders.html',
})
export class Orders implements OnInit {
  orders: CustomerOrder[] = [];
  customers: Customer[] = [];
  products: Product[] = [];

  loading = false;
  submitting = false;
  detailsLoading = false;

  errorMessage = '';
  formErrorMessage = '';
  successMessage = '';

  searchTerm = '';
  statusFilter = 'ALL';

  showForm = false;
  editingOrderId: number | undefined = undefined;
  pendingIdempotencyKey: string | undefined = undefined;
  private editingOriginalProducts = new Map<number, Product>();

  selectedOrderDetails: CustomerOrderDetails | null = null;
  workflowSummary: OrderProcessingSummary | null = null;
  workflowInvoice: Invoice | null = null;
  workflowLoading = false;
  workflowActionLoading = false;
  workflowErrorMessage = '';
  workflowSuccessMessage = '';

  orderForm: {
    orderNumber: string;
    customerId: number;
    items: OrderFormItemRow[];
  } = {
    orderNumber: '',
    customerId: 0,
    items: [],
  };

  constructor(
    private orderService: OrderService,
    private customerService: CustomerService,
    private productService: ProductService,
    private invoiceService: InvoiceService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadInitialData();
  }

  loadInitialData(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      orders: this.orderService.getOrders(),
      customers: this.customerService.getCustomers(),
      products: this.productService.getProducts(),
    }).subscribe({
      next: ({ orders, customers, products }) => {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Orders load error:', err);
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to load customer orders.'
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadOrders(): void {
    this.loading = true;
    this.errorMessage = '';

    this.orderService.getOrders().subscribe({
      next: (data) => {
        this.orders = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Orders refresh error:', err);
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to load customer orders.'
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  get filteredOrders(): CustomerOrder[] {
    const query = this.searchTerm.trim().toLowerCase();

    return this.orders.filter((order) => {
      const matchesStatus =
        this.statusFilter === 'ALL' ||
        (order.status || '').toUpperCase() === this.statusFilter;

      if (!matchesStatus) {
        return false;
      }

      if (!query) {
        return true;
      }

      const orderNumber = (order.orderNumber || '').toLowerCase();
      const customerName = (order.customer?.customerName || '').toLowerCase();
      const customerEmail = (order.customer?.email || '').toLowerCase();

      return (
        orderNumber.includes(query) ||
        customerName.includes(query) ||
        customerEmail.includes(query)
      );
    });
  }

  get activeProducts(): Product[] {
    return this.products.filter((p) => p.active !== false);
  }

  getSelectableProducts(row: OrderFormItemRow): Product[] {
    const active = this.activeProducts;
    if (this.editingOrderId === undefined || !row.productId) {
      return active;
    }

    const alreadyActive = active.some((p) => p.productId === row.productId);
    if (alreadyActive) {
      return active;
    }

    const inactiveProduct =
      this.products.find((p) => p.productId === row.productId) ||
      this.editingOriginalProducts.get(row.productId);

    return inactiveProduct ? [...active, inactiveProduct] : active;
  }

  isInactiveProduct(product: Product): boolean {
    return product.active === false;
  }

  get newOrdersCount(): number {
    return this.orders.filter((o) => o.status === 'NEW').length;
  }

  get inProgressOrdersCount(): number {
    return this.orders.filter((o) => o.status === 'IN_PROGRESS').length;
  }

  get totalOrdersAmount(): number {
    return this.orders.reduce((sum, o) => sum + (o.totalAmount || 0), 0);
  }

  get estimatedFormTotal(): number {
    const sum = this.orderForm.items.reduce(
      (acc, item) => acc + (item.subtotal || 0),
      0
    );
    return Math.round(sum * 100) / 100;
  }

  openCreateOrderForm(): void {
    this.editingOrderId = undefined;
    this.editingOriginalProducts.clear();
    this.pendingIdempotencyKey = this.generateIdempotencyKey();
    this.formErrorMessage = '';
    this.successMessage = '';
    this.selectedOrderDetails = null;

    this.orderForm = {
      orderNumber: '',
      customerId: 0,
      items: [this.createEmptyItemRow()],
    };

    this.showForm = true;
    this.cdr.detectChanges();
  }

  openEditOrderForm(order: CustomerOrder): void {
    if (!order.orderId || order.status !== 'NEW') {
      return;
    }

    this.formErrorMessage = '';
    this.successMessage = '';
    this.selectedOrderDetails = null;
    this.pendingIdempotencyKey = undefined;
    this.editingOriginalProducts.clear();
    this.detailsLoading = true;

    this.orderService.getOrderDetails(order.orderId).subscribe({
      next: (details) => {
        this.editingOrderId = details.orderId;
        if (details.items) {
          for (const item of details.items) {
            if (item.product?.productId) {
              this.editingOriginalProducts.set(item.product.productId, item.product);
            }
          }
        }
        this.orderForm = {
          orderNumber: details.orderNumber || '',
          customerId: details.customer?.customerId || 0,
          items:
            details.items && details.items.length > 0
              ? details.items.map((item) => {
                  const unitPrice = item.product?.unitPrice ?? item.unitPrice ?? 0;
                  const quantity = item.quantity || 1;
                  return {
                    productId: item.product?.productId || 0,
                    quantity,
                    unitPrice,
                    subtotal: Math.round(unitPrice * quantity * 100) / 100,
                  };
                })
              : [this.createEmptyItemRow()],
        };
        this.detailsLoading = false;
        this.showForm = true;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.detailsLoading = false;
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to load order for editing.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  closeOrderForm(): void {
    this.showForm = false;
    this.editingOrderId = undefined;
    this.pendingIdempotencyKey = undefined;
    this.editingOriginalProducts.clear();
    this.formErrorMessage = '';
    this.cdr.detectChanges();
  }

  addItemRow(): void {
    this.orderForm.items.push(this.createEmptyItemRow());
    this.cdr.detectChanges();
  }

  removeItemRow(index: number): void {
    if (this.orderForm.items.length <= 1) {
      this.formErrorMessage = 'An order must contain at least one product item.';
      return;
    }
    this.orderForm.items.splice(index, 1);
    this.formErrorMessage = '';
    this.cdr.detectChanges();
  }

  onProductChange(row: OrderFormItemRow): void {
    const product = this.products.find((p) => p.productId === row.productId);
    row.unitPrice = product ? product.unitPrice : 0;
    this.recalculateRow(row);
  }

  onQuantityChange(row: OrderFormItemRow): void {
    this.recalculateRow(row);
  }

  saveOrder(): void {
    this.formErrorMessage = '';
    this.successMessage = '';

    if (!this.orderForm.customerId || this.orderForm.customerId <= 0) {
      this.formErrorMessage = 'Please select a customer.';
      return;
    }

    if (this.orderForm.items.length === 0) {
      this.formErrorMessage = 'Please add at least one product to the order.';
      return;
    }

    const seenProductIds = new Set<number>();
    for (let i = 0; i < this.orderForm.items.length; i++) {
      const row = this.orderForm.items[i];
      if (!row.productId || row.productId <= 0) {
        this.formErrorMessage = `Please select a product for item row ${i + 1}.`;
        return;
      }
      if (seenProductIds.has(Number(row.productId))) {
        this.formErrorMessage =
          'Duplicate products are not allowed in the same order. Adjust the quantity on the existing row.';
        return;
      }
      seenProductIds.add(Number(row.productId));
      if (
        row.quantity === null ||
        row.quantity === undefined ||
        !Number.isInteger(Number(row.quantity)) ||
        Number(row.quantity) <= 0
      ) {
        this.formErrorMessage = `Quantity must be a positive whole number for item row ${i + 1}.`;
        return;
      }
    }

    const payload: CustomerOrderRequest = {
      customerId: Number(this.orderForm.customerId),
      items: this.orderForm.items.map((row) => ({
        productId: Number(row.productId),
        quantity: Number(row.quantity),
      })),
    };

    if (this.orderForm.orderNumber.trim()) {
      payload.orderNumber = this.orderForm.orderNumber.trim();
    }

    this.submitting = true;
    this.cdr.detectChanges();

    if (this.editingOrderId === undefined && !this.pendingIdempotencyKey) {
      this.pendingIdempotencyKey = this.generateIdempotencyKey();
    }

    const request$ =
      this.editingOrderId !== undefined
        ? this.orderService.updateOrder(this.editingOrderId, payload)
        : this.orderService.createOrder(payload, this.pendingIdempotencyKey);

    request$.subscribe({
      next: (savedOrder) => {
        const wasEdit = this.editingOrderId !== undefined;
        this.submitting = false;
        this.showForm = false;
        this.editingOrderId = undefined;
        this.pendingIdempotencyKey = undefined;
        this.editingOriginalProducts.clear();
        this.successMessage = wasEdit
          ? `Order ${savedOrder.orderNumber} updated successfully.`
          : `Order ${savedOrder.orderNumber} created successfully.`;
        this.loadOrders();
      },
      error: (err) => {
        console.error('Order save error:', err);
        this.submitting = false;
        this.formErrorMessage = this.extractErrorMessage(
          err,
          'Unable to save customer order.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  viewOrderDetails(order: CustomerOrder): void {
    if (!order.orderId) {
      return;
    }

    this.errorMessage = '';
    this.detailsLoading = true;
    this.cdr.detectChanges();

    this.orderService.getOrderDetails(order.orderId).subscribe({
      next: (details) => {
        this.selectedOrderDetails = details;
        this.detailsLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Order details error:', err);
        this.detailsLoading = false;
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to load order details.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  closeOrderDetails(): void {
    this.selectedOrderDetails = null;
    this.cdr.detectChanges();
  }

  openWorkflowModal(order: CustomerOrder): void {
    if (!order.orderId) {
      return;
    }
    this.selectedOrderDetails = null;
    this.workflowErrorMessage = '';
    this.workflowSuccessMessage = '';
    this.loadWorkflowSummary(order.orderId);
  }

  closeWorkflowModal(): void {
    this.workflowSummary = null;
    this.workflowErrorMessage = '';
    this.workflowSuccessMessage = '';
    this.cdr.detectChanges();
  }

  loadWorkflowSummary(orderId: number): void {
    this.workflowLoading = true;
    this.cdr.detectChanges();

    this.orderService.getProcessingSummary(orderId).subscribe({
      next: (summary) => {
        this.workflowSummary = summary;
        this.workflowInvoice = null;
        if (
          (summary.status || '').toUpperCase() === 'COMPLETED' &&
          this.canManageOrders
        ) {
          this.invoiceService.getInvoiceByOrderId(orderId).subscribe({
            next: (inv) => {
              this.workflowInvoice = inv;
              this.cdr.detectChanges();
            },
            error: () => {
              this.workflowInvoice = null;
            },
          });
        }
        this.workflowLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.workflowLoading = false;
        this.workflowErrorMessage = this.extractErrorMessage(
          err,
          'Unable to load order processing workflow.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  generateShortagePoFromWorkflow(): void {
    if (!this.workflowSummary) {
      return;
    }
    const orderId = this.workflowSummary.orderId;
    this.workflowActionLoading = true;
    this.workflowErrorMessage = '';
    this.workflowSuccessMessage = '';

    this.orderService.createPurchaseOrderForShortages(orderId).subscribe({
      next: () => {
        this.workflowActionLoading = false;
        this.workflowSuccessMessage =
          'Shortage Purchase Order generated! Receive it in Purchase Orders, then click Recheck Inventory.';
        this.loadWorkflowSummary(orderId);
        this.loadOrders();
      },
      error: (err) => {
        this.workflowActionLoading = false;
        this.workflowErrorMessage = this.extractErrorMessage(
          err,
          'Unable to generate purchase order for shortages.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  startProcessingFromWorkflow(): void {
    if (!this.workflowSummary) {
      return;
    }
    const orderId = this.workflowSummary.orderId;
    this.workflowActionLoading = true;
    this.workflowErrorMessage = '';
    this.workflowSuccessMessage = '';

    this.orderService.startOrderProcessing(orderId).subscribe({
      next: (updated) => {
        this.workflowActionLoading = false;
        this.workflowSuccessMessage = `Order ${updated.orderNumber} is now IN_PROGRESS and raw materials have been deducted from inventory.`;
        this.loadWorkflowSummary(orderId);
        this.loadOrders();
      },
      error: (err) => {
        this.workflowActionLoading = false;
        this.workflowErrorMessage = this.extractErrorMessage(
          err,
          'Unable to start order processing.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  completeOrderFromWorkflow(): void {
    if (!this.workflowSummary) {
      return;
    }
    const orderId = this.workflowSummary.orderId;
    this.workflowActionLoading = true;
    this.workflowErrorMessage = '';
    this.workflowSuccessMessage = '';

    this.orderService.completeOrder(orderId).subscribe({
      next: (updated) => {
        this.workflowActionLoading = false;
        this.workflowSuccessMessage = `Order ${updated.orderNumber} marked as COMPLETED.`;
        this.loadWorkflowSummary(orderId);
        this.loadOrders();
      },
      error: (err) => {
        this.workflowActionLoading = false;
        this.workflowErrorMessage = this.extractErrorMessage(
          err,
          'Unable to complete order.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  cancelOrderFromWorkflow(): void {
    if (!this.workflowSummary) {
      return;
    }
    const orderId = this.workflowSummary.orderId;
    if (!confirm(`Cancel order ${this.workflowSummary.orderNumber}?`)) {
      return;
    }
    this.workflowActionLoading = true;
    this.workflowErrorMessage = '';
    this.workflowSuccessMessage = '';

    this.orderService.cancelOrder(orderId).subscribe({
      next: (updated) => {
        this.workflowActionLoading = false;
        this.workflowSuccessMessage = `Order ${updated.orderNumber} has been CANCELLED.`;
        this.loadWorkflowSummary(orderId);
        this.loadOrders();
      },
      error: (err) => {
        this.workflowActionLoading = false;
        this.workflowErrorMessage = this.extractErrorMessage(
          err,
          'Unable to cancel order.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  generateInvoiceFromWorkflow(): void {
    if (!this.workflowSummary) {
      return;
    }
    const orderId = this.workflowSummary.orderId;
    this.workflowActionLoading = true;
    this.workflowErrorMessage = '';
    this.workflowSuccessMessage = '';

    this.invoiceService.generateInvoice(orderId, { taxRate: 18 }).subscribe({
      next: (inv) => {
        this.workflowActionLoading = false;
        this.workflowInvoice = inv;
        this.workflowSuccessMessage = `Invoice ${inv.invoiceNumber} generated successfully!`;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.workflowActionLoading = false;
        this.workflowErrorMessage = this.extractErrorMessage(
          err,
          'Unable to generate invoice.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  getWorkflowStepState(step: number): 'done' | 'active' | 'pending' | 'cancelled' {
    if (!this.workflowSummary) {
      return 'pending';
    }
    const status = (this.workflowSummary.status || '').toUpperCase();
    if (status === 'CANCELLED') {
      return step === 1 ? 'done' : 'cancelled';
    }
    if (status === 'COMPLETED') {
      return 'done';
    }
    if (status === 'IN_PROGRESS') {
      if (step <= 3) return 'done';
      if (step === 4) return 'active';
      return 'pending';
    }
    // Status is NEW
    if (step === 1) return 'done';
    if (step === 2) {
      return this.workflowSummary.sufficientInventory ? 'done' : 'active';
    }
    if (step === 3) {
      if (this.workflowSummary.sufficientInventory) return 'done';
      if (this.workflowSummary.activeCreatedPoExists) return 'active';
      return 'pending';
    }
    return 'pending';
  }

  deleteOrder(order: CustomerOrder): void {
    if (!order.orderId) {
      return;
    }

    if (
      !confirm(
        `Are you sure you want to delete order ${order.orderNumber}? This will also remove its line items.`
      )
    ) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.orderService.deleteOrder(order.orderId).subscribe({
      next: () => {
        if (this.selectedOrderDetails?.orderId === order.orderId) {
          this.selectedOrderDetails = null;
        }
        this.successMessage = `Order ${order.orderNumber} deleted successfully.`;
        this.loadOrders();
      },
      error: (err) => {
        console.error('Order delete error:', err);
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to delete order.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  canEdit(order: CustomerOrder): boolean {
    return (
      this.authService.hasAnyRole('ADMIN', 'SALES_EXECUTIVE') &&
      (order.status || '').toUpperCase() === 'NEW'
    );
  }

  canDelete(order: CustomerOrder): boolean {
    const status = (order.status || '').toUpperCase();
    return (
      this.authService.hasAnyRole('ADMIN', 'SALES_EXECUTIVE') &&
      (status === 'NEW' || status === 'CANCELLED')
    );
  }

  get canManageOrders(): boolean {
    return this.authService.hasAnyRole('ADMIN', 'SALES_EXECUTIVE');
  }

  get canGeneratePo(): boolean {
    return this.authService.hasAnyRole('ADMIN', 'PROCUREMENT', 'INVENTORY_MANAGER');
  }

  get canProcessOrders(): boolean {
    return this.authService.hasAnyRole('ADMIN', 'INVENTORY_MANAGER');
  }

  getStatusClass(status: string): string {
    switch ((status || '').toUpperCase()) {
      case 'NEW':
        return 'status-new';
      case 'IN_PROGRESS':
        return 'status-in-progress';
      case 'COMPLETED':
      case 'DELIVERED':
        return 'status-completed';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return 'status-default';
    }
  }

  private createEmptyItemRow(): OrderFormItemRow {
    return {
      productId: 0,
      quantity: 1,
      unitPrice: 0,
      subtotal: 0,
    };
  }

  private recalculateRow(row: OrderFormItemRow): void {
    const qty = Number(row.quantity) > 0 ? Number(row.quantity) : 0;
    row.subtotal = Math.round(row.unitPrice * qty * 100) / 100;
  }

  private generateIdempotencyKey(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `order-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  }

  private extractErrorMessage(err: unknown, fallback: string): string {
    if (
      err &&
      typeof err === 'object' &&
      'error' in err &&
      err.error &&
      typeof err.error === 'object' &&
      'message' in err.error &&
      typeof err.error.message === 'string' &&
      err.error.message.trim()
    ) {
      return err.error.message;
    }
    return fallback;
  }
}
