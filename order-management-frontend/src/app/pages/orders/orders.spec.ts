import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Orders } from './orders';
import { OrderService } from '../../services/order';
import { CustomerService } from '../../services/customer';
import { ProductService } from '../../services/product';
import { AuthService } from '../../services/auth';
import { Customer } from '../../models/customer';
import { Product } from '../../models/product';
import { CustomerOrder, CustomerOrderDetails } from '../../models/order';

describe('Orders', () => {
  const mockCustomers: Customer[] = [
    {
      customerId: 1,
      customerName: 'ABC Manufacturing',
      email: 'abc@example.com',
      phone: '9999999999',
      address: 'Pune',
    },
  ];

  const mockProducts: Product[] = [
    {
      productId: 10,
      productCode: 'PROD-001',
      productName: 'Steel Frame',
      description: 'Heavy duty frame',
      unitPrice: 1200,
      active: true,
    },
    {
      productId: 20,
      productCode: 'PROD-002',
      productName: 'Control Unit',
      description: 'Digital controller',
      unitPrice: 500,
      active: true,
    },
  ];

  const mockOrders: CustomerOrder[] = [
    {
      orderId: 100,
      orderNumber: 'ORD-001',
      customer: mockCustomers[0],
      orderDate: '2026-11-25T10:00:00',
      status: 'NEW',
      totalAmount: 2900,
    },
  ];

  let orderServiceMock: {
    getOrders: ReturnType<typeof vi.fn>;
    getOrderDetails: ReturnType<typeof vi.fn>;
    createOrder: ReturnType<typeof vi.fn>;
    updateOrder: ReturnType<typeof vi.fn>;
    deleteOrder: ReturnType<typeof vi.fn>;
    getProcessingSummary: ReturnType<typeof vi.fn>;
    createPurchaseOrderForShortages: ReturnType<typeof vi.fn>;
    startOrderProcessing: ReturnType<typeof vi.fn>;
    completeOrder: ReturnType<typeof vi.fn>;
    cancelOrder: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    orderServiceMock = {
      getOrders: vi.fn().mockReturnValue(of(mockOrders)),
      getOrderDetails: vi.fn(),
      createOrder: vi.fn(),
      updateOrder: vi.fn(),
      deleteOrder: vi.fn().mockReturnValue(of(void 0)),
      getProcessingSummary: vi.fn(),
      createPurchaseOrderForShortages: vi.fn(),
      startOrderProcessing: vi.fn(),
      completeOrder: vi.fn(),
      cancelOrder: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [Orders],
      providers: [
        { provide: OrderService, useValue: orderServiceMock },
        {
          provide: CustomerService,
          useValue: { getCustomers: vi.fn().mockReturnValue(of(mockCustomers)) },
        },
        {
          provide: ProductService,
          useValue: { getProducts: vi.fn().mockReturnValue(of(mockProducts)) },
        },
        {
          provide: AuthService,
          useValue: { hasAnyRole: () => true },
        },
      ],
    }).compileComponents();
  });

  it('loads orders, customers, and products on init', () => {
    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    expect(comp.orders.length).toBe(1);
    expect(comp.customers.length).toBe(1);
    expect(comp.products.length).toBe(2);
  });

  it('calculates row subtotals and form total automatically when products and quantities change', () => {
    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openCreateOrderForm();
    comp.orderForm.customerId = 1;
    comp.orderForm.items[0].productId = 10;
    comp.onProductChange(comp.orderForm.items[0]);
    comp.orderForm.items[0].quantity = 2;
    comp.onQuantityChange(comp.orderForm.items[0]);

    comp.addItemRow();
    comp.orderForm.items[1].productId = 20;
    comp.onProductChange(comp.orderForm.items[1]);
    comp.orderForm.items[1].quantity = 3;
    comp.onQuantityChange(comp.orderForm.items[1]);

    expect(comp.orderForm.items[0].subtotal).toBe(2400);
    expect(comp.orderForm.items[1].subtotal).toBe(1500);
    expect(comp.estimatedFormTotal).toBe(3900);
  });

  it('validates missing customer and invalid quantities before calling createOrder', () => {
    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openCreateOrderForm();
    comp.saveOrder();
    expect(comp.formErrorMessage).toContain('customer');
    expect(orderServiceMock.createOrder).not.toHaveBeenCalled();

    comp.orderForm.customerId = 1;
    comp.orderForm.items[0].productId = 10;
    comp.orderForm.items[0].quantity = 0;
    comp.saveOrder();
    expect(comp.formErrorMessage).toContain('positive whole number');
    expect(orderServiceMock.createOrder).not.toHaveBeenCalled();
  });

  it('creates an order with multiple products and submits only customerId and product quantities', () => {
    orderServiceMock.createOrder.mockReturnValue(
      of({
        orderId: 101,
        orderNumber: 'ORD-101',
        customer: mockCustomers[0],
        status: 'NEW',
        totalAmount: 3900,
      })
    );

    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openCreateOrderForm();
    comp.orderForm.customerId = 1;
    comp.orderForm.items[0].productId = 10;
    comp.orderForm.items[0].quantity = 2;
    comp.addItemRow();
    comp.orderForm.items[1].productId = 20;
    comp.orderForm.items[1].quantity = 3;

    comp.saveOrder();

    expect(orderServiceMock.createOrder).toHaveBeenCalledWith(
      {
        customerId: 1,
        items: [
          { productId: 10, quantity: 2 },
          { productId: 20, quantity: 3 },
        ],
      },
      expect.any(String)
    );
    expect(comp.successMessage).toContain('ORD-101');
  });

  it('rejects duplicate products in the same order form', () => {
    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openCreateOrderForm();
    comp.orderForm.customerId = 1;
    comp.orderForm.items[0].productId = 10;
    comp.orderForm.items[0].quantity = 1;
    comp.addItemRow();
    comp.orderForm.items[1].productId = 10;
    comp.orderForm.items[1].quantity = 2;

    comp.saveOrder();

    expect(comp.formErrorMessage).toContain('Duplicate products');
    expect(orderServiceMock.createOrder).not.toHaveBeenCalled();
  });

  it('keeps inactive product visible with label when editing an existing NEW order but excludes it from new rows', () => {
    const inactiveProduct: Product = {
      productId: 30,
      productCode: 'PROD-003',
      productName: 'Legacy Valve',
      description: 'Discontinued',
      unitPrice: 250,
      active: false,
    };

    const detailsWithInactive: CustomerOrderDetails = {
      orderId: 100,
      orderNumber: 'ORD-001',
      customer: mockCustomers[0],
      status: 'NEW',
      totalAmount: 250,
      items: [
        {
          orderItemId: 5,
          product: inactiveProduct,
          quantity: 1,
          unitPrice: 250,
          subtotal: 250,
        },
      ],
    };
    orderServiceMock.getOrderDetails.mockReturnValue(of(detailsWithInactive));

    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    comp.products = [...mockProducts, inactiveProduct];

    comp.openEditOrderForm(mockOrders[0]);
    fixture.detectChanges();

    const existingRowOptions = comp.getSelectableProducts(comp.orderForm.items[0]);
    expect(existingRowOptions.some((p) => p.productId === 30)).toBe(true);

    comp.addItemRow();
    const newRowOptions = comp.getSelectableProducts(comp.orderForm.items[1]);
    expect(newRowOptions.some((p) => p.productId === 30)).toBe(false);
    expect(comp.isInactiveProduct(inactiveProduct)).toBe(true);
  });

  it('reuses the same idempotency key on retry after failure', () => {
    orderServiceMock.createOrder
      .mockReturnValueOnce(throwError(() => ({ error: { message: 'Temporary failure' } })))
      .mockReturnValueOnce(
        of({
          orderId: 102,
          orderNumber: 'ORD-102',
          customer: mockCustomers[0],
          status: 'NEW',
          totalAmount: 1200,
        })
      );

    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openCreateOrderForm();
    const firstKey = comp.pendingIdempotencyKey;
    comp.orderForm.customerId = 1;
    comp.orderForm.items[0].productId = 10;
    comp.orderForm.items[0].quantity = 1;

    comp.saveOrder();
    expect(comp.formErrorMessage).toBe('Temporary failure');
    expect(comp.pendingIdempotencyKey).toBe(firstKey);

    comp.saveOrder();
    expect(orderServiceMock.createOrder.mock.calls[0][1]).toBe(firstKey);
    expect(orderServiceMock.createOrder.mock.calls[1][1]).toBe(firstKey);
    expect(comp.pendingIdempotencyKey).toBeUndefined();
  });

  it('loads order details and supports editing a NEW order', () => {
    const details: CustomerOrderDetails = {
      orderId: 100,
      orderNumber: 'ORD-001',
      customer: mockCustomers[0],
      status: 'NEW',
      totalAmount: 2900,
      items: [
        {
          orderItemId: 1,
          product: mockProducts[0],
          quantity: 2,
          unitPrice: 1200,
          subtotal: 2400,
        },
        {
          orderItemId: 2,
          product: mockProducts[1],
          quantity: 1,
          unitPrice: 500,
          subtotal: 500,
        },
      ],
    };
    orderServiceMock.getOrderDetails.mockReturnValue(of(details));
    orderServiceMock.updateOrder.mockReturnValue(
      of({ ...mockOrders[0], totalAmount: 3400 })
    );

    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.viewOrderDetails(mockOrders[0]);
    expect(comp.selectedOrderDetails?.items.length).toBe(2);

    comp.openEditOrderForm(mockOrders[0]);
    expect(comp.editingOrderId).toBe(100);
    expect(comp.orderForm.items.length).toBe(2);
    comp.orderForm.items[1].quantity = 2;
    comp.saveOrder();

    expect(orderServiceMock.updateOrder).toHaveBeenCalledWith(100, {
      orderNumber: 'ORD-001',
      customerId: 1,
      items: [
        { productId: 10, quantity: 2 },
        { productId: 20, quantity: 2 },
      ],
    });
  });

  it('surfaces backend error message when order creation fails', () => {
    orderServiceMock.createOrder.mockReturnValue(
      throwError(() => ({ error: { message: 'Customer not found with ID: 999' } }))
    );

    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openCreateOrderForm();
    comp.orderForm.customerId = 999;
    comp.orderForm.items[0].productId = 10;
    comp.orderForm.items[0].quantity = 1;
    comp.saveOrder();

    expect(comp.formErrorMessage).toBe('Customer not found with ID: 999');
  });

  it('opens order processing workflow and transitions NEW -> IN_PROGRESS -> COMPLETED', () => {
    orderServiceMock.getProcessingSummary
      .mockReturnValueOnce(
        of({
          orderId: 100,
          orderNumber: 'ORD-001',
          status: 'NEW',
          hasOrderItems: true,
          sufficientInventory: true,
          activeCreatedPoExists: false,
          canGeneratePurchaseOrder: false,
          canStartProcessing: true,
          canComplete: false,
          canCancel: true,
          productsWithoutBom: [],
          requirements: [],
          linkedPurchaseOrders: [],
        })
      )
      .mockReturnValue(
        of({
          orderId: 100,
          orderNumber: 'ORD-001',
          status: 'IN_PROGRESS',
          hasOrderItems: true,
          sufficientInventory: true,
          activeCreatedPoExists: false,
          canGeneratePurchaseOrder: false,
          canStartProcessing: false,
          canComplete: true,
          canCancel: false,
          productsWithoutBom: [],
          requirements: [],
          linkedPurchaseOrders: [],
        })
      );
    orderServiceMock.startOrderProcessing.mockReturnValue(
      of({ ...mockOrders[0], status: 'IN_PROGRESS' })
    );
    orderServiceMock.completeOrder.mockReturnValue(
      of({ ...mockOrders[0], status: 'COMPLETED' })
    );

    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openWorkflowModal(mockOrders[0]);
    expect(comp.workflowSummary?.canStartProcessing).toBe(true);

    comp.startProcessingFromWorkflow();
    expect(orderServiceMock.startOrderProcessing).toHaveBeenCalledWith(100);
    expect(comp.workflowSuccessMessage).toContain('IN_PROGRESS');

    comp.completeOrderFromWorkflow();
    expect(orderServiceMock.completeOrder).toHaveBeenCalledWith(100);
    expect(comp.workflowSuccessMessage).toContain('COMPLETED');
  });
});
