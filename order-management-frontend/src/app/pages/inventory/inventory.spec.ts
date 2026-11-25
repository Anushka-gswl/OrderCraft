import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Inventory } from './inventory';
import { AuthService } from '../../services/auth';

describe('Inventory Component', () => {
  let component: Inventory;
  let fixture: ComponentFixture<Inventory>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Inventory],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { hasAnyRole: () => true } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Inventory);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load inventory records on init', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/inventory');
    req.flush([
      {
        inventoryId: 1,
        material: {
          materialId: 1,
          materialCode: 'MAT-01',
          materialName: 'Steel',
          unit: 'kg',
          reorderLevel: 20,
          unitCost: 100,
        },
        quantityAvailable: 75,
      },
    ]);

    expect(component.inventory.length).toBe(1);
    expect(component.inventory[0].quantityAvailable).toBe(75);
  });

  it('should validate materialId and quantity before saving', () => {
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/inventory').flush([]);

    component.openAddInventoryForm();
    component.saveInventory();

    expect(component.errorMessage).toContain('valid material ID');
  });

  it('should update existing inventory record', () => {
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/inventory').flush([]);

    component.openEditInventoryForm({
      inventoryId: 5,
      material: {
        materialId: 1,
        materialCode: 'MAT-01',
        materialName: 'Steel',
        unit: 'kg',
        reorderLevel: 20,
        unitCost: 100,
      },
      quantityAvailable: 50,
    });
    component.newInventory.quantityAvailable = 80;
    component.saveInventory();

    const putReq = httpMock.expectOne('http://localhost:8080/api/inventory/5');
    expect(putReq.request.method).toBe('PUT');
    putReq.flush({ ...component.newInventory, inventoryId: 5 });

    httpMock.expectOne('http://localhost:8080/api/inventory').flush([]);

    expect(component.showForm).toBe(false);
    expect(component.editingId).toBeUndefined();
  });
});
