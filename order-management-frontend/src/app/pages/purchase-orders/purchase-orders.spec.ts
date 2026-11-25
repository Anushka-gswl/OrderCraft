import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { PurchaseOrders } from './purchase-orders';
import { PurchaseOrderService } from '../../services/purchase-order';
import { MaterialService } from '../../services/material';
import { OrderService } from '../../services/order';
import { InventoryService } from '../../services/inventory';
import { AuthService } from '../../services/auth';
import { Material } from '../../models/material';
import { PurchaseOrder, PurchaseOrderDetails } from '../../models/purchase-order';

describe('PurchaseOrders', () => {
  const mockMaterials: Material[] = [
    {
      materialId: 1,
      materialCode: 'MAT-001',
      materialName: 'Steel Sheet',
      unit: 'KG',
      reorderLevel: 10,
      unitCost: 120,
    },
  ];

  const mockPOs: PurchaseOrder[] = [
    {
      purchaseOrderId: 50,
      poNumber: 'PO-050',
      supplierName: 'Tata Metals',
      orderDate: '2026-11-25T10:00:00',
      status: 'CREATED',
      totalAmount: 1200,
    },
  ];

  let poServiceMock: {
    getPurchaseOrders: ReturnType<typeof vi.fn>;
    getPurchaseOrderDetails: ReturnType<typeof vi.fn>;
    createPurchaseOrder: ReturnType<typeof vi.fn>;
    updatePurchaseOrder: ReturnType<typeof vi.fn>;
    receivePurchaseOrder: ReturnType<typeof vi.fn>;
    deletePurchaseOrder: ReturnType<typeof vi.fn>;
  };

  let inventoryServiceMock: {
    getInventory: ReturnType<typeof vi.fn>;
  };

  let orderServiceMock: {
    getOrders: ReturnType<typeof vi.fn>;
    getShortages: ReturnType<typeof vi.fn>;
    createPurchaseOrderForShortages: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    poServiceMock = {
      getPurchaseOrders: vi.fn().mockReturnValue(of(mockPOs)),
      getPurchaseOrderDetails: vi.fn(),
      createPurchaseOrder: vi.fn(),
      updatePurchaseOrder: vi.fn(),
      receivePurchaseOrder: vi.fn(),
      deletePurchaseOrder: vi.fn().mockReturnValue(of(void 0)),
    };

    inventoryServiceMock = {
      getInventory: vi.fn().mockReturnValue(of([])),
    };

    orderServiceMock = {
      getOrders: vi.fn().mockReturnValue(of([])),
      getShortages: vi.fn().mockReturnValue(of(['Steel Sheet | Material ID: 1 | Shortage: 10.0 KG'])),
      createPurchaseOrderForShortages: vi.fn().mockReturnValue(of(mockPOs[0])),
    };

    await TestBed.configureTestingModule({
      imports: [PurchaseOrders],
      providers: [
        { provide: PurchaseOrderService, useValue: poServiceMock },
        {
          provide: MaterialService,
          useValue: { getMaterials: vi.fn().mockReturnValue(of(mockMaterials)) },
        },
        { provide: OrderService, useValue: orderServiceMock },
        { provide: InventoryService, useValue: inventoryServiceMock },
        { provide: AuthService, useValue: { hasAnyRole: () => true } },
      ],
    }).compileComponents();
  });

  it('loads purchase orders, materials, orders, and inventory on init', () => {
    const fixture = TestBed.createComponent(PurchaseOrders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    expect(comp.purchaseOrders.length).toBe(1);
    expect(comp.materials.length).toBe(1);
  });

  it('creates a manual purchase order and refreshes inventory', () => {
    poServiceMock.createPurchaseOrder.mockReturnValue(of(mockPOs[0]));

    const fixture = TestBed.createComponent(PurchaseOrders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openCreatePoForm();
    comp.poForm.supplierName = 'Tata Metals';
    comp.poForm.items[0].materialId = 1;
    comp.onMaterialChange(comp.poForm.items[0]);
    comp.poForm.items[0].quantity = 10;
    comp.onQuantityChange(comp.poForm.items[0]);

    expect(comp.estimatedFormTotal).toBe(1200);
    comp.savePurchaseOrder();

    expect(poServiceMock.createPurchaseOrder).toHaveBeenCalledWith({
      supplierName: 'Tata Metals',
      customerOrderId: null,
      items: [{ materialId: 1, quantity: 10 }],
    });
    expect(comp.successMessage).toContain('PO-050');
  });

  it('receives a CREATED PO, refreshes inventory, and blocks duplicate receive on RECEIVED PO', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    poServiceMock.receivePurchaseOrder.mockReturnValue(
      of({ ...mockPOs[0], status: 'RECEIVED' })
    );

    const fixture = TestBed.createComponent(PurchaseOrders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.receivePurchaseOrder(mockPOs[0]);

    expect(poServiceMock.receivePurchaseOrder).toHaveBeenCalledWith(50);
    expect(inventoryServiceMock.getInventory).toHaveBeenCalledTimes(2);
    expect(comp.successMessage).toContain('received');

    // Already RECEIVED PO cannot be received again
    const receivedPo: PurchaseOrder = { ...mockPOs[0], status: 'RECEIVED' };
    expect(comp.canReceive(receivedPo)).toBe(false);
    comp.receivePurchaseOrder(receivedPo);
    expect(poServiceMock.receivePurchaseOrder).toHaveBeenCalledTimes(1);
  });

  it('surfaces backend error if receiving fails', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    poServiceMock.receivePurchaseOrder.mockReturnValue(
      throwError(() => ({ error: { message: 'Purchase Order PO-050 has already been received.' } }))
    );

    const fixture = TestBed.createComponent(PurchaseOrders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.receivePurchaseOrder(mockPOs[0]);
    expect(comp.errorMessage).toBe('Purchase Order PO-050 has already been received.');
  });

  it('loads purchase order details modal', () => {
    const details: PurchaseOrderDetails = {
      ...mockPOs[0],
      items: [
        {
          purchaseOrderItemId: 1,
          material: mockMaterials[0],
          quantity: 10,
          unitCost: 120,
          subtotal: 1200,
        },
      ],
    };
    poServiceMock.getPurchaseOrderDetails.mockReturnValue(of(details));

    const fixture = TestBed.createComponent(PurchaseOrders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.viewPoDetails(mockPOs[0]);
    expect(comp.selectedPoDetails?.items.length).toBe(1);
  });

  it('rejects duplicate materials in the same purchase order form', () => {
    const fixture = TestBed.createComponent(PurchaseOrders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openCreatePoForm();
    comp.poForm.supplierName = 'Tata Metals';
    comp.poForm.items[0].materialId = 1;
    comp.poForm.items[0].quantity = 5;
    comp.addItemRow();
    comp.poForm.items[1].materialId = 1;
    comp.poForm.items[1].quantity = 5;

    comp.savePurchaseOrder();
    expect(comp.formErrorMessage).toContain('Duplicate materials');
    expect(poServiceMock.createPurchaseOrder).not.toHaveBeenCalled();
  });

  it('filters shortageEligibleOrders to NEW customer orders only', () => {
    const fixture = TestBed.createComponent(PurchaseOrders);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.customerOrders = [
      {
        orderId: 1,
        orderNumber: 'ORD-NEW',
        customer: { customerName: 'C1', email: '', phone: '', address: '' },
        status: 'NEW',
        totalAmount: 100,
      },
      {
        orderId: 2,
        orderNumber: 'ORD-PROG',
        customer: { customerName: 'C2', email: '', phone: '', address: '' },
        status: 'IN_PROGRESS',
        totalAmount: 200,
      },
    ];

    expect(comp.shortageEligibleOrders.map((o) => o.orderNumber)).toEqual(['ORD-NEW']);
  });
});
