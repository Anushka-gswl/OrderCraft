import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Customers } from './customers';

describe('Customers Component', () => {
  let component: Customers;
  let fixture: ComponentFixture<Customers>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Customers],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Customers);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load customers on init', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/customers');
    req.flush([
      {
        customerId: 1,
        customerName: 'Acme Corp',
        email: 'acme@example.com',
        phone: '1234567890',
        address: '123 Main St',
      },
    ]);

    expect(component.customers.length).toBe(1);
    expect(component.customers[0].customerName).toBe('Acme Corp');
  });

  it('should validate required fields before saving customer', () => {
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/customers').flush([]);

    component.openAddCustomerForm();
    component.saveCustomer();

    expect(component.errorMessage).toBe('Please fill in all customer details.');
  });

  it('should create a new customer', () => {
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/customers').flush([]);

    component.openAddCustomerForm();
    component.newCustomer = {
      customerName: 'Beta Ltd',
      email: 'beta@example.com',
      phone: '9876543210',
      address: '456 Oak Ave',
    };
    component.saveCustomer();

    const postReq = httpMock.expectOne('http://localhost:8080/api/customers');
    expect(postReq.request.method).toBe('POST');
    postReq.flush({ customerId: 2, ...component.newCustomer });

    httpMock.expectOne('http://localhost:8080/api/customers').flush([
      { customerId: 2, ...component.newCustomer },
    ]);

    expect(component.showForm).toBe(false);
    expect(component.customers.length).toBe(1);
  });
});
