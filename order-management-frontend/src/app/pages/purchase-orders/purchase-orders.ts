import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { PurchaseOrderService } from '../../services/purchase-order';
import { MaterialService } from '../../services/material';
import { OrderService } from '../../services/order';
import { InventoryService } from '../../services/inventory';
import { AuthService } from '../../services/auth';
import {
  PurchaseOrder,
  PurchaseOrderDetails,
  PurchaseOrderRequest,
} from '../../models/purchase-order';
import { Material } from '../../models/material';
import { CustomerOrder } from '../../models/order';
import { Inventory } from '../../models/inventory';

interface PoFormItemRow {
  materialId: number;
  quantity: number;
  unitCost: number;
  unit: string;
  subtotal: number;
}

@Component({
  imports: [CommonModule, FormsModule],
  selector: 'app-purchase-orders',
  styleUrl: './purchase-orders.css',
  templateUrl: './purchase-orders.html',
})
export class PurchaseOrders implements OnInit {
  purchaseOrders: PurchaseOrder[] = [];
  materials: Material[] = [];
  customerOrders: CustomerOrder[] = [];
  inventorySnapshot: Inventory[] = [];

  loading = false;
  submitting = false;
  receivingId: number | undefined = undefined;
  detailsLoading = false;
  shortageLoading = false;

  errorMessage = '';
  formErrorMessage = '';
  shortageErrorMessage = '';
  successMessage = '';

  searchTerm = '';
  statusFilter = 'ALL';

  showForm = false;
  showShortagePanel = false;
  editingPoId: number | undefined = undefined;

  selectedPoDetails: PurchaseOrderDetails | null = null;

  selectedOrderForShortageId = 0;
  shortagesPreview: string[] = [];

  poForm: {
    poNumber: string;
    supplierName: string;
    customerOrderId: number;
    items: PoFormItemRow[];
  } = {
    poNumber: '',
    supplierName: '',
    customerOrderId: 0,
    items: [],
  };

  constructor(
    private purchaseOrderService: PurchaseOrderService,
    private materialService: MaterialService,
    private orderService: OrderService,
    private inventoryService: InventoryService,
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
      purchaseOrders: this.purchaseOrderService.getPurchaseOrders(),
      materials: this.materialService.getMaterials(),
      customerOrders: this.orderService.getOrders(),
      inventory: this.inventoryService.getInventory(),
    }).subscribe({
      next: ({ purchaseOrders, materials, customerOrders, inventory }) => {
        this.purchaseOrders = purchaseOrders;
        this.materials = materials;
        this.customerOrders = customerOrders;
        this.inventorySnapshot = inventory;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Purchase orders load error:', err);
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to load purchase orders.'
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  refreshPurchaseOrdersAndInventory(): void {
    forkJoin({
      purchaseOrders: this.purchaseOrderService.getPurchaseOrders(),
      inventory: this.inventoryService.getInventory(),
    }).subscribe({
      next: ({ purchaseOrders, inventory }) => {
        this.purchaseOrders = purchaseOrders;
        this.inventorySnapshot = inventory;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Refresh error:', err);
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to refresh purchase orders.'
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  get filteredPurchaseOrders(): PurchaseOrder[] {
    const query = this.searchTerm.trim().toLowerCase();

    return this.purchaseOrders.filter((po) => {
      const matchesStatus =
        this.statusFilter === 'ALL' ||
        (po.status || '').toUpperCase() === this.statusFilter;

      if (!matchesStatus) {
        return false;
      }

      if (!query) {
        return true;
      }

      const poNumber = (po.poNumber || '').toLowerCase();
      const supplier = (po.supplierName || '').toLowerCase();
      const linkedOrder = (po.customerOrder?.orderNumber || '').toLowerCase();

      return (
        poNumber.includes(query) ||
        supplier.includes(query) ||
        linkedOrder.includes(query)
      );
    });
  }

  get createdCount(): number {
    return this.purchaseOrders.filter((p) => p.status === 'CREATED').length;
  }

  get receivedCount(): number {
    return this.purchaseOrders.filter((p) => p.status === 'RECEIVED').length;
  }

  get totalProcurementValue(): number {
    return this.purchaseOrders.reduce((sum, p) => sum + (p.totalAmount || 0), 0);
  }

  get lowStockCount(): number {
    return this.inventorySnapshot.filter(
      (i) => i.quantityAvailable <= i.material.reorderLevel
    ).length;
  }

  get estimatedFormTotal(): number {
    const sum = this.poForm.items.reduce((acc, row) => acc + (row.subtotal || 0), 0);
    return Math.round(sum * 100) / 100;
  }

  get shortageEligibleOrders(): CustomerOrder[] {
    return this.customerOrders.filter(
      (o) => (o.status || '').toUpperCase() === 'NEW'
    );
  }

  openCreatePoForm(): void {
    this.editingPoId = undefined;
    this.formErrorMessage = '';
    this.successMessage = '';
    this.showShortagePanel = false;
    this.selectedPoDetails = null;

    this.poForm = {
      poNumber: '',
      supplierName: '',
      customerOrderId: 0,
      items: [this.createEmptyItemRow()],
    };

    this.showForm = true;
    this.cdr.detectChanges();
  }

  openEditPoForm(po: PurchaseOrder): void {
    if (!po.purchaseOrderId || !this.canEdit(po)) {
      return;
    }

    this.formErrorMessage = '';
    this.successMessage = '';
    this.showShortagePanel = false;
    this.selectedPoDetails = null;
    this.detailsLoading = true;

    this.purchaseOrderService.getPurchaseOrderDetails(po.purchaseOrderId).subscribe({
      next: (details) => {
        this.editingPoId = details.purchaseOrderId;
        this.poForm = {
          poNumber: details.poNumber || '',
          supplierName: details.supplierName || '',
          customerOrderId: details.customerOrder?.orderId || 0,
          items:
            details.items && details.items.length > 0
              ? details.items.map((item) => {
                  const unitCost = item.material?.unitCost ?? item.unitCost ?? 0;
                  const quantity = item.quantity || 1;
                  return {
                    materialId: item.material?.materialId || 0,
                    quantity,
                    unitCost,
                    unit: item.material?.unit || '',
                    subtotal: Math.round(unitCost * quantity * 100) / 100,
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
          'Unable to load purchase order for editing.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  closePoForm(): void {
    this.showForm = false;
    this.editingPoId = undefined;
    this.formErrorMessage = '';
    this.cdr.detectChanges();
  }

  openShortageGenerator(): void {
    this.showForm = false;
    this.shortageErrorMessage = '';
    this.shortagesPreview = [];
    this.selectedOrderForShortageId = 0;
    this.showShortagePanel = true;
    this.cdr.detectChanges();
  }

  closeShortageGenerator(): void {
    this.showShortagePanel = false;
    this.shortageErrorMessage = '';
    this.shortagesPreview = [];
    this.cdr.detectChanges();
  }

  onShortageOrderSelected(): void {
    this.shortageErrorMessage = '';
    this.shortagesPreview = [];

    if (!this.selectedOrderForShortageId || this.selectedOrderForShortageId <= 0) {
      return;
    }

    this.shortageLoading = true;
    this.orderService.getShortages(this.selectedOrderForShortageId).subscribe({
      next: (shortages) => {
        this.shortagesPreview = shortages;
        this.shortageLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.shortageLoading = false;
        this.shortageErrorMessage = this.extractErrorMessage(
          err,
          'Unable to check shortages for the selected order.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  generatePoForShortages(): void {
    if (!this.selectedOrderForShortageId || this.selectedOrderForShortageId <= 0) {
      this.shortageErrorMessage = 'Please select a customer order first.';
      return;
    }

    this.submitting = true;
    this.shortageErrorMessage = '';

    this.orderService
      .createPurchaseOrderForShortages(this.selectedOrderForShortageId)
      .subscribe({
        next: (created) => {
          const po = created as PurchaseOrder;
          this.submitting = false;
          this.showShortagePanel = false;
          this.successMessage = `Shortage Purchase Order ${po.poNumber || ''} generated successfully.`;
          this.refreshPurchaseOrdersAndInventory();
        },
        error: (err) => {
          this.submitting = false;
          this.shortageErrorMessage = this.extractErrorMessage(
            err,
            'Unable to generate purchase order for shortages.'
          );
          this.cdr.detectChanges();
        },
      });
  }

  addItemRow(): void {
    this.poForm.items.push(this.createEmptyItemRow());
    this.cdr.detectChanges();
  }

  removeItemRow(index: number): void {
    if (this.poForm.items.length <= 1) {
      this.formErrorMessage = 'A purchase order must contain at least one material.';
      return;
    }
    this.poForm.items.splice(index, 1);
    this.formErrorMessage = '';
    this.cdr.detectChanges();
  }

  onMaterialChange(row: PoFormItemRow): void {
    const mat = this.materials.find((m) => m.materialId === row.materialId);
    row.unitCost = mat ? mat.unitCost : 0;
    row.unit = mat ? mat.unit : '';
    this.recalculateRow(row);
  }

  onQuantityChange(row: PoFormItemRow): void {
    this.recalculateRow(row);
  }

  savePurchaseOrder(): void {
    this.formErrorMessage = '';
    this.successMessage = '';

    if (!this.poForm.supplierName.trim()) {
      this.formErrorMessage = 'Please enter a supplier name.';
      return;
    }

    if (this.poForm.items.length === 0) {
      this.formErrorMessage = 'Please add at least one material item.';
      return;
    }

    const seenMaterialIds = new Set<number>();
    for (let i = 0; i < this.poForm.items.length; i++) {
      const row = this.poForm.items[i];
      if (!row.materialId || row.materialId <= 0) {
        this.formErrorMessage = `Please select a material for item row ${i + 1}.`;
        return;
      }
      if (seenMaterialIds.has(Number(row.materialId))) {
        this.formErrorMessage =
          'Duplicate materials are not allowed in the same purchase order. Adjust the quantity on the existing row.';
        return;
      }
      seenMaterialIds.add(Number(row.materialId));
      if (row.quantity === null || row.quantity === undefined || Number(row.quantity) <= 0) {
        this.formErrorMessage = `Quantity must be greater than zero for item row ${i + 1}.`;
        return;
      }
    }

    const payload: PurchaseOrderRequest = {
      supplierName: this.poForm.supplierName.trim(),
      customerOrderId:
        this.poForm.customerOrderId && this.poForm.customerOrderId > 0
          ? Number(this.poForm.customerOrderId)
          : null,
      items: this.poForm.items.map((row) => ({
        materialId: Number(row.materialId),
        quantity: Number(row.quantity),
      })),
    };

    if (this.poForm.poNumber.trim()) {
      payload.poNumber = this.poForm.poNumber.trim();
    }

    this.submitting = true;
    this.cdr.detectChanges();

    const request$ =
      this.editingPoId !== undefined
        ? this.purchaseOrderService.updatePurchaseOrder(this.editingPoId, payload)
        : this.purchaseOrderService.createPurchaseOrder(payload);

    request$.subscribe({
      next: (saved) => {
        const wasEdit = this.editingPoId !== undefined;
        this.submitting = false;
        this.showForm = false;
        this.editingPoId = undefined;
        this.successMessage = wasEdit
          ? `Purchase Order ${saved.poNumber} updated successfully.`
          : `Purchase Order ${saved.poNumber} created successfully.`;
        this.refreshPurchaseOrdersAndInventory();
      },
      error: (err) => {
        this.submitting = false;
        this.formErrorMessage = this.extractErrorMessage(
          err,
          'Unable to save purchase order.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  viewPoDetails(po: PurchaseOrder): void {
    if (!po.purchaseOrderId) {
      return;
    }

    this.errorMessage = '';
    this.detailsLoading = true;
    this.cdr.detectChanges();

    this.purchaseOrderService.getPurchaseOrderDetails(po.purchaseOrderId).subscribe({
      next: (details) => {
        this.selectedPoDetails = details;
        this.detailsLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.detailsLoading = false;
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to load purchase order details.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  closePoDetails(): void {
    this.selectedPoDetails = null;
    this.cdr.detectChanges();
  }

  receivePurchaseOrder(po: PurchaseOrder): void {
    if (!po.purchaseOrderId || !this.canReceive(po)) {
      return;
    }

    if (
      !confirm(
        `Receive Purchase Order ${po.poNumber}? Material inventory quantities will be updated automatically.`
      )
    ) {
      return;
    }

    this.receivingId = po.purchaseOrderId;
    this.errorMessage = '';
    this.successMessage = '';
    this.cdr.detectChanges();

    this.purchaseOrderService.receivePurchaseOrder(po.purchaseOrderId).subscribe({
      next: (updated) => {
        this.receivingId = undefined;
        if (
          this.selectedPoDetails &&
          this.selectedPoDetails.purchaseOrderId === po.purchaseOrderId
        ) {
          this.selectedPoDetails = {
            ...this.selectedPoDetails,
            status: updated.status,
          };
        }
        this.successMessage = `Purchase Order ${updated.poNumber} received and material inventory refreshed.`;
        this.refreshPurchaseOrdersAndInventory();
      },
      error: (err) => {
        this.receivingId = undefined;
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to receive purchase order.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  deletePurchaseOrder(po: PurchaseOrder): void {
    if (!po.purchaseOrderId || !this.canDelete(po)) {
      return;
    }

    if (!confirm(`Are you sure you want to delete Purchase Order ${po.poNumber}?`)) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.purchaseOrderService.deletePurchaseOrder(po.purchaseOrderId).subscribe({
      next: () => {
        if (this.selectedPoDetails?.purchaseOrderId === po.purchaseOrderId) {
          this.selectedPoDetails = null;
        }
        this.successMessage = `Purchase Order ${po.poNumber} deleted successfully.`;
        this.refreshPurchaseOrdersAndInventory();
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to delete purchase order.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  get canManagePurchaseOrders(): boolean {
    return this.authService.hasAnyRole('ADMIN', 'PROCUREMENT');
  }

  canEdit(po: PurchaseOrder): boolean {
    return (
      this.canManagePurchaseOrders &&
      (po.status || '').toUpperCase() === 'CREATED'
    );
  }

  canReceive(po: PurchaseOrder): boolean {
    return (
      this.authService.hasAnyRole('ADMIN', 'INVENTORY_MANAGER') &&
      (po.status || '').toUpperCase() === 'CREATED'
    );
  }

  canDelete(po: PurchaseOrder): boolean {
    const status = (po.status || '').toUpperCase();
    return (
      this.canManagePurchaseOrders &&
      (status === 'CREATED' || status === 'CANCELLED')
    );
  }

  getStatusClass(status: string): string {
    switch ((status || '').toUpperCase()) {
      case 'CREATED':
        return 'status-created';
      case 'RECEIVED':
        return 'status-received';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return 'status-default';
    }
  }

  private createEmptyItemRow(): PoFormItemRow {
    return {
      materialId: 0,
      quantity: 1,
      unitCost: 0,
      unit: '',
      subtotal: 0,
    };
  }

  private recalculateRow(row: PoFormItemRow): void {
    const qty = Number(row.quantity) > 0 ? Number(row.quantity) : 0;
    row.subtotal = Math.round(row.unitCost * qty * 100) / 100;
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
