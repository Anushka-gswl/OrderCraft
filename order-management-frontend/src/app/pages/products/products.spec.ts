import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { Products } from './products';
import { ProductService } from '../../services/product';
import { MaterialService } from '../../services/material';
import { ProductMaterialService } from '../../services/product-material';
import { AuthService } from '../../services/auth';
import { Product } from '../../models/product';
import { Material } from '../../models/material';

describe('Products (with BOM management)', () => {
  const mockProducts: Product[] = [
    {
      productId: 1,
      productCode: 'PROD-001',
      productName: 'Steel Frame',
      description: 'Frame',
      unitPrice: 1000,
      active: true,
    },
  ];

  const mockMaterials: Material[] = [
    {
      materialId: 10,
      materialCode: 'MAT-001',
      materialName: 'Steel Rod',
      unit: 'KG',
      reorderLevel: 5,
      unitCost: 50,
    },
  ];

  let bomServiceMock: {
    getByProductId: ReturnType<typeof vi.fn>;
    createProductMaterial: ReturnType<typeof vi.fn>;
    updateProductMaterial: ReturnType<typeof vi.fn>;
    deleteProductMaterial: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    bomServiceMock = {
      getByProductId: vi.fn().mockReturnValue(of([])),
      createProductMaterial: vi.fn().mockReturnValue(
        of({
          productMaterialId: 99,
          product: mockProducts[0],
          material: mockMaterials[0],
          quantityRequired: 4,
        })
      ),
      updateProductMaterial: vi.fn(),
      deleteProductMaterial: vi.fn().mockReturnValue(of(void 0)),
    };

    await TestBed.configureTestingModule({
      imports: [Products],
      providers: [
        {
          provide: ProductService,
          useValue: {
            getProducts: vi.fn().mockReturnValue(of(mockProducts)),
            createProduct: vi.fn(),
            deleteProduct: vi.fn(),
          },
        },
        {
          provide: MaterialService,
          useValue: { getMaterials: vi.fn().mockReturnValue(of(mockMaterials)) },
        },
        { provide: ProductMaterialService, useValue: bomServiceMock },
        { provide: AuthService, useValue: { hasAnyRole: () => true } },
      ],
    }).compileComponents();
  });

  it('loads BOM and adds a material mapping to a product', () => {
    const fixture = TestBed.createComponent(Products);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openBomModal(mockProducts[0]);
    expect(bomServiceMock.getByProductId).toHaveBeenCalledWith(1);

    comp.newBomMaterialId = 10;
    comp.newBomQuantity = 4;
    comp.addBomMaterial();

    expect(bomServiceMock.createProductMaterial).toHaveBeenCalledWith({
      productId: 1,
      materialId: 10,
      quantityRequired: 4,
    });
    expect(comp.bomSuccessMessage).toContain('Material added');
  });
});
