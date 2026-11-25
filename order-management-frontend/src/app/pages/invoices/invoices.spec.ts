import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Invoices } from './invoices';
import { AuthService } from '../../services/auth';

describe('Invoices Component', () => {
  let component: Invoices;
  let fixture: ComponentFixture<Invoices>;
  let httpMock: HttpTestingController;
  let authServiceStub: { hasAnyRole: () => boolean };

  beforeEach(async () => {
    authServiceStub = {
      hasAnyRole: () => true,
    };

    await TestBed.configureTestingModule({
      imports: [Invoices],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Invoices);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create and load invoices and completed orders', () => {
    fixture.detectChanges();

    const invReq = httpMock.expectOne('http://localhost:8080/api/invoices');
    invReq.flush([
      {
        invoiceId: 1,
        invoiceNumber: 'INV-2026-0001',
        orderId: 10,
        orderNumber: 'ORD-0010',
        customerId: 1,
        customerName: 'Acme Corp',
        invoiceDate: '2026-11-25T10:00:00',
        status: 'DRAFT',
        subtotal: 1000,
        taxRate: 18,
        taxAmount: 180,
        totalAmount: 1180,
        createdAt: '2026-11-25T10:00:00',
        items: [],
      },
    ]);

    const ordReq = httpMock.expectOne('http://localhost:8080/api/orders');
    ordReq.flush([
      {
        orderId: 10,
        orderNumber: 'ORD-0010',
        customer: { customerId: 1, customerName: 'Acme Corp' },
        status: 'COMPLETED',
        totalAmount: 1000,
      },
      {
        orderId: 11,
        orderNumber: 'ORD-0011',
        customer: { customerId: 1, customerName: 'Acme Corp' },
        status: 'COMPLETED',
        totalAmount: 500,
      },
    ]);

    expect(component.invoices.length).toBe(1);
    expect(component.completedOrders.length).toBe(1);
    expect(component.completedOrders[0].orderId).toBe(11);
  });

  it('should validate orderId before generating invoice', () => {
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/invoices').flush([]);
    httpMock.expectOne('http://localhost:8080/api/orders').flush([]);

    component.openGenerateForm();
    component.generateForm.orderId = 0;
    component.submitGenerateInvoice();

    expect(component.formErrorMessage).toContain('select a completed order');
  });

  it('should generate an invoice for a completed order', () => {
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/invoices').flush([]);
    httpMock.expectOne('http://localhost:8080/api/orders').flush([
      {
        orderId: 11,
        orderNumber: 'ORD-0011',
        customer: { customerId: 1, customerName: 'Acme Corp' },
        status: 'COMPLETED',
        totalAmount: 500,
      },
    ]);

    component.openGenerateForm();
    component.generateForm.orderId = 11;
    component.generateForm.taxRate = 18;
    component.submitGenerateInvoice();

    const postReq = httpMock.expectOne('http://localhost:8080/api/invoices?orderId=11');
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body.taxRate).toBe(18);

    postReq.flush({
      invoiceId: 2,
      invoiceNumber: 'INV-2026-0002',
      orderId: 11,
      orderNumber: 'ORD-0011',
      customerId: 1,
      customerName: 'Acme Corp',
      invoiceDate: '2026-11-25T10:05:00',
      status: 'DRAFT',
      subtotal: 500,
      taxRate: 18,
      taxAmount: 90,
      totalAmount: 590,
      createdAt: '2026-11-25T10:05:00',
      items: [],
    });

    // After creation, loadData() runs again
    httpMock.expectOne('http://localhost:8080/api/invoices').flush([]);
    httpMock.expectOne('http://localhost:8080/api/orders').flush([]);

    expect(component.successMessage).toContain('INV-2026-0002');
  });
});
