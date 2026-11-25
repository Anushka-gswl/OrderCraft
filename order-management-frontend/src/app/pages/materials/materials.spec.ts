import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Materials } from './materials';
import { AuthService } from '../../services/auth';

describe('Materials Component', () => {
  let component: Materials;
  let fixture: ComponentFixture<Materials>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Materials],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { hasAnyRole: () => true } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Materials);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load materials on init', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/materials');
    req.flush([
      {
        materialId: 1,
        materialCode: 'MAT-01',
        materialName: 'Steel Sheet',
        unit: 'kg',
        reorderLevel: 50,
        unitCost: 120,
      },
    ]);

    expect(component.materials.length).toBe(1);
    expect(component.materials[0].materialCode).toBe('MAT-01');
  });

  it('should validate material form before saving', () => {
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/materials').flush([]);

    component.openAddMaterialForm();
    component.saveMaterial();

    expect(component.errorMessage).toContain('Please fill in all material details');
  });

  it('should create a new material', () => {
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/materials').flush([]);

    component.openAddMaterialForm();
    component.newMaterial = {
      materialCode: 'MAT-02',
      materialName: 'Copper Wire',
      unit: 'm',
      reorderLevel: 100,
      unitCost: 45,
    };
    component.saveMaterial();

    const postReq = httpMock.expectOne('http://localhost:8080/api/materials');
    expect(postReq.request.method).toBe('POST');
    postReq.flush({ materialId: 2, ...component.newMaterial });

    httpMock.expectOne('http://localhost:8080/api/materials').flush([
      { materialId: 2, ...component.newMaterial },
    ]);

    expect(component.showForm).toBe(false);
    expect(component.materials.length).toBe(1);
  });
});
