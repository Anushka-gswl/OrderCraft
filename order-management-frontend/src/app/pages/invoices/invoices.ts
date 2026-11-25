import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { InvoiceService } from '../../services/invoice';
import { OrderService } from '../../services/order';
import { AuthService } from '../../services/auth';
import { Invoice, InvoiceRequest, InvoiceStatus } from '../../models/invoice';
import { CustomerOrder } from '../../models/order';

@Component({
  selector: 'app-invoices',
  imports: [CommonModule, FormsModule],
  templateUrl: './invoices.html',
  styleUrl: './invoices.css',
})
export class Invoices implements OnInit {
  invoices: Invoice[] = [];
  completedOrders: CustomerOrder[] = [];

  loading = false;
  submitting = false;
  actionLoading = false;

  errorMessage = '';
  formErrorMessage = '';
  successMessage = '';

  searchTerm = '';
  statusFilter = 'ALL';

  showGenerateForm = false;
  selectedInvoice: Invoice | null = null;

  generateForm: {
    orderId: number;
    taxRate: number;
    dueDate: string;
    notes: string;
  } = {
    orderId: 0,
    taxRate: 18,
    dueDate: '',
    notes: '',
  };

  constructor(
    private invoiceService: InvoiceService,
    private orderService: OrderService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      invoices: this.invoiceService.getInvoices(),
      orders: this.orderService.getOrders(),
    }).subscribe({
      next: ({ invoices, orders }) => {
        this.invoices = invoices;
        const invoicedOrderIds = new Set(invoices.map((i) => i.orderId));
        this.completedOrders = orders.filter(
          (o) =>
            (o.status || '').toUpperCase() === 'COMPLETED' &&
            o.orderId !== undefined &&
            !invoicedOrderIds.has(o.orderId)
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to load invoices.'
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  get filteredInvoices(): Invoice[] {
    const term = this.searchTerm.trim().toLowerCase();
    return this.invoices.filter((inv) => {
      const matchesStatus =
        this.statusFilter === 'ALL' ||
        (inv.status || '').toUpperCase() === this.statusFilter;
      if (!matchesStatus) {
        return false;
      }
      if (!term) {
        return true;
      }
      return (
        (inv.invoiceNumber || '').toLowerCase().includes(term) ||
        (inv.orderNumber || '').toLowerCase().includes(term) ||
        (inv.customerName || '').toLowerCase().includes(term)
      );
    });
  }

  openGenerateForm(): void {
    this.formErrorMessage = '';
    this.successMessage = '';
    this.generateForm = {
      orderId:
        this.completedOrders.length > 0 && this.completedOrders[0].orderId
          ? this.completedOrders[0].orderId
          : 0,
      taxRate: 18,
      dueDate: '',
      notes: '',
    };
    this.showGenerateForm = true;
    this.cdr.detectChanges();
  }

  closeGenerateForm(): void {
    this.showGenerateForm = false;
    this.formErrorMessage = '';
    this.cdr.detectChanges();
  }

  submitGenerateInvoice(): void {
    this.formErrorMessage = '';
    this.successMessage = '';

    if (!this.generateForm.orderId || this.generateForm.orderId <= 0) {
      this.formErrorMessage = 'Please select a completed order.';
      return;
    }

    if (
      this.generateForm.taxRate === null ||
      this.generateForm.taxRate === undefined ||
      this.generateForm.taxRate < 0
    ) {
      this.formErrorMessage = 'Tax rate must be 0 or greater.';
      return;
    }

    const payload: InvoiceRequest = {
      taxRate: Number(this.generateForm.taxRate),
      dueDate: this.generateForm.dueDate ? this.generateForm.dueDate : undefined,
      notes: this.generateForm.notes.trim() || undefined,
    };

    this.submitting = true;
    this.invoiceService
      .generateInvoice(Number(this.generateForm.orderId), payload)
      .subscribe({
        next: (created) => {
          this.submitting = false;
          this.showGenerateForm = false;
          this.successMessage = `Invoice ${created.invoiceNumber} generated successfully.`;
          this.loadData();
        },
        error: (err) => {
          this.submitting = false;
          this.formErrorMessage = this.extractErrorMessage(
            err,
            'Failed to generate invoice.'
          );
          this.cdr.detectChanges();
        },
      });
  }

  viewInvoice(inv: Invoice): void {
    this.selectedInvoice = inv;
    this.cdr.detectChanges();
  }

  closeInvoiceModal(): void {
    this.selectedInvoice = null;
    this.cdr.detectChanges();
  }

  printInvoice(): void {
    window.print();
  }

  updateStatus(inv: Invoice, status: InvoiceStatus): void {
    if (!inv.invoiceId) {
      return;
    }
    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.invoiceService.updateStatus(inv.invoiceId, status).subscribe({
      next: (updated) => {
        this.actionLoading = false;
        if (this.selectedInvoice?.invoiceId === updated.invoiceId) {
          this.selectedInvoice = updated;
        }
        this.successMessage = `Invoice ${updated.invoiceNumber} marked as ${updated.status}.`;
        this.loadData();
      },
      error: (err) => {
        this.actionLoading = false;
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to update invoice status.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  deleteInvoice(inv: Invoice): void {
    if (!inv.invoiceId) {
      return;
    }
    if (
      !confirm(
        `Are you sure you want to delete draft invoice ${inv.invoiceNumber}?`
      )
    ) {
      return;
    }
    this.errorMessage = '';
    this.successMessage = '';

    this.invoiceService.deleteInvoice(inv.invoiceId).subscribe({
      next: () => {
        if (this.selectedInvoice?.invoiceId === inv.invoiceId) {
          this.selectedInvoice = null;
        }
        this.successMessage = `Invoice ${inv.invoiceNumber} deleted.`;
        this.loadData();
      },
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(
          err,
          'Unable to delete invoice.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  getStatusClass(status: string): string {
    switch ((status || '').toUpperCase()) {
      case 'DRAFT':
        return 'status-draft';
      case 'SENT':
        return 'status-sent';
      case 'PAID':
        return 'status-paid';
      case 'OVERDUE':
        return 'status-overdue';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return 'status-draft';
    }
  }

  get canManageInvoices(): boolean {
    return this.authService.hasAnyRole('ADMIN', 'SALES_EXECUTIVE');
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
