import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Dashboard } from './dashboard';
import { AuthService } from '../../services/auth';

describe('Dashboard Component', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let httpMock: HttpTestingController;
  let authServiceStub: {
    hasAnyRole: () => boolean;
    currentUser: () => { fullName: string; role: string };
  };

  beforeEach(async () => {
    authServiceStub = {
      hasAnyRole: () => true,
      currentUser: () => ({ fullName: 'Test Admin', role: 'ADMIN' }),
    };

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch and render live dashboard summary', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/dashboard/summary');
    expect(req.request.method).toBe('GET');

    req.flush({
      totalCustomers: 4,
      totalProducts: 6,
      activeProducts: 5,
      totalMaterials: 8,
      totalOrders: 10,
      newOrders: 2,
      inProgressOrders: 3,
      completedOrders: 4,
      cancelledOrders: 1,
      totalPurchaseOrders: 5,
      pendingPurchaseOrders: 1,
      receivedPurchaseOrders: 4,
      totalInvoices: 4,
      paidInvoices: 3,
      unpaidInvoices: 1,
      totalOrderRevenue: 12500,
      totalInvoicedAmount: 14750,
      paidInvoiceAmount: 11000,
      outstandingInvoiceAmount: 3750,
      totalPurchaseSpend: 4200,
      lowStockAlerts: [
        {
          materialId: 1,
          materialCode: 'MAT-01',
          materialName: 'Steel Rod',
          unit: 'kg',
          quantityAvailable: 5,
          reorderLevel: 20,
        },
      ],
      recentOrders: [],
      recentInvoices: [],
    });

    fixture.detectChanges();

    expect(component.summary).not.toBeNull();
    expect(component.summary?.totalOrders).toBe(10);
    expect(component.summary?.lowStockAlerts.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Steel Rod');
  });

  it('should display error message when summary request fails', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/dashboard/summary');
    req.flush(
      { message: 'Service unavailable' },
      { status: 503, statusText: 'Service Unavailable' }
    );

    expect(component.errorMessage).toBe('Service unavailable');
    expect(component.loading).toBe(false);
  });
});
