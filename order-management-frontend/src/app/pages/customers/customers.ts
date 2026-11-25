import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CustomerService } from '../../services/customer';
import { Customer } from '../../models/customer';

@Component({
  selector: 'app-customers',
  imports: [FormsModule],
  templateUrl: './customers.html',
  styleUrl: './customers.css'
})
export class Customers implements OnInit {

  customers: Customer[] = [];
  loading = false;
  errorMessage = '';

  showForm = false;

  newCustomer: Customer = {
    customerName: '',
    email: '',
    phone: '',
    address: ''
  };

  constructor(
    private customerService: CustomerService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.loading = true;

    this.customerService.getCustomers().subscribe({
      next: (data) => {
        this.customers = data;
        this.loading = false;

        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Customer API error:', error);

        this.errorMessage = 'Unable to load customers.';
        this.loading = false;

        this.cdr.detectChanges();
      }
    });
  }

  openAddCustomerForm(): void {
    this.newCustomer = {
      customerName: '',
      email: '',
      phone: '',
      address: ''
    };

    this.errorMessage = '';
    this.showForm = true;

    this.cdr.detectChanges();
  }

  closeAddCustomerForm(): void {
    this.showForm = false;
    this.errorMessage = '';

    this.cdr.detectChanges();
  }

  saveCustomer(): void {

    if (
      !this.newCustomer.customerName.trim() ||
      !this.newCustomer.email.trim() ||
      !this.newCustomer.phone.trim() ||
      !this.newCustomer.address.trim()
    ) {
      this.errorMessage = 'Please fill in all customer details.';
      return;
    }

    this.customerService.createCustomer(this.newCustomer).subscribe({
      next: () => {
        this.showForm = false;
        this.loadCustomers();
      },
      error: (error) => {
        console.error('Customer creation error:', error);
        this.errorMessage = 'Unable to create customer.';
        this.cdr.detectChanges();
      }
    });
  }

  deleteCustomer(id: number | undefined): void {

    if (id === undefined) {
      return;
    }

    if (!confirm('Are you sure you want to delete this customer?')) {
      return;
    }

    this.customerService.deleteCustomer(id).subscribe({
      next: () => {
        this.loadCustomers();
      },
      error: () => {
        this.errorMessage = 'Unable to delete customer.';
        this.cdr.detectChanges();
      }
    });
  }
}