import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ProductService } from '../../services/product';
import { MaterialService } from '../../services/material';
import { ProductMaterialService } from '../../services/product-material';
import { AuthService } from '../../services/auth';
import { Product } from '../../models/product';
import { Material } from '../../models/material';
import { ProductMaterial } from '../../models/product-material';

@Component({
  selector: 'app-products',
  imports: [CommonModule, FormsModule],
  templateUrl: './products.html',
  styleUrl: './products.css',
})
export class Products implements OnInit {
  products: Product[] = [];
  materials: Material[] = [];
  loading = false;
  errorMessage = '';

  showForm = false;

  newProduct: Product = {
    productCode: '',
    productName: '',
    description: '',
    unitPrice: 0,
    active: true,
  };

  // BOM Management Modal state
  selectedBomProduct: Product | null = null;
  bomItems: ProductMaterial[] = [];
  bomLoading = false;
  bomSubmitting = false;
  bomErrorMessage = '';
  bomSuccessMessage = '';
  newBomMaterialId = 0;
  newBomQuantity = 1;

  constructor(
    private productService: ProductService,
    private materialService: MaterialService,
    private productMaterialService: ProductMaterialService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  get canManageProducts(): boolean {
    return this.authService.hasAnyRole('ADMIN');
  }

  ngOnInit(): void {
    this.loadProducts();
    this.loadMaterials();
  }

  loadProducts(): void {
    this.loading = true;

    this.productService.getProducts().subscribe({
      next: (data) => {
        this.products = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Product API error:', error);
        this.errorMessage = 'Unable to load products.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadMaterials(): void {
    this.materialService.getMaterials().subscribe({
      next: (data) => {
        this.materials = data;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Material API error:', error);
      },
    });
  }

  openAddProductForm(): void {
    this.newProduct = {
      productCode: '',
      productName: '',
      description: '',
      unitPrice: 0,
      active: true,
    };

    this.errorMessage = '';
    this.showForm = true;
    this.cdr.detectChanges();
  }

  closeAddProductForm(): void {
    this.showForm = false;
    this.errorMessage = '';
    this.cdr.detectChanges();
  }

  saveProduct(): void {
    if (
      !this.newProduct.productCode.trim() ||
      !this.newProduct.productName.trim() ||
      !this.newProduct.description.trim() ||
      this.newProduct.unitPrice <= 0
    ) {
      this.errorMessage = 'Please fill in all product details correctly.';
      return;
    }

    this.productService.createProduct(this.newProduct).subscribe({
      next: () => {
        this.showForm = false;
        this.loadProducts();
      },
      error: (error) => {
        console.error('Product creation error:', error);
        this.errorMessage = 'Unable to create product.';
        this.cdr.detectChanges();
      },
    });
  }

  deleteProduct(id: number | undefined): void {
    if (id === undefined) {
      return;
    }

    if (!confirm('Are you sure you want to delete this product?')) {
      return;
    }

    this.productService.deleteProduct(id).subscribe({
      next: () => {
        this.loadProducts();
      },
      error: () => {
        this.errorMessage = 'Unable to delete product.';
        this.cdr.detectChanges();
      },
    });
  }

  // BOM Management
  get availableMaterialsForBom(): Material[] {
    const mappedIds = new Set(
      this.bomItems.map((item) => item.material?.materialId)
    );
    return this.materials.filter((m) => !mappedIds.has(m.materialId));
  }

  openBomModal(product: Product): void {
    if (!product.productId) {
      return;
    }
    this.selectedBomProduct = product;
    this.bomErrorMessage = '';
    this.bomSuccessMessage = '';
    this.newBomMaterialId = 0;
    this.newBomQuantity = 1;
    this.loadBomForProduct(product.productId);
  }

  closeBomModal(): void {
    this.selectedBomProduct = null;
    this.bomItems = [];
    this.bomErrorMessage = '';
    this.bomSuccessMessage = '';
    this.cdr.detectChanges();
  }

  loadBomForProduct(productId: number): void {
    this.bomLoading = true;
    this.cdr.detectChanges();

    this.productMaterialService.getByProductId(productId).subscribe({
      next: (items) => {
        this.bomItems = items;
        this.bomLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.bomLoading = false;
        this.bomErrorMessage = this.extractErrorMessage(
          err,
          'Unable to load product Bill of Materials.'
        );
        this.cdr.detectChanges();
      },
    });
  }

  addBomMaterial(): void {
    if (!this.selectedBomProduct?.productId) {
      return;
    }
    this.bomErrorMessage = '';
    this.bomSuccessMessage = '';

    if (!this.newBomMaterialId || this.newBomMaterialId <= 0) {
      this.bomErrorMessage = 'Please select a raw material.';
      return;
    }
    if (!this.newBomQuantity || this.newBomQuantity <= 0) {
      this.bomErrorMessage = 'Required quantity must be greater than zero.';
      return;
    }

    this.bomSubmitting = true;
    this.cdr.detectChanges();

    const productId = this.selectedBomProduct.productId;
    this.productMaterialService
      .createProductMaterial({
        productId,
        materialId: Number(this.newBomMaterialId),
        quantityRequired: Number(this.newBomQuantity),
      })
      .subscribe({
        next: () => {
          this.bomSubmitting = false;
          this.newBomMaterialId = 0;
          this.newBomQuantity = 1;
          this.bomSuccessMessage = 'Material added to product BOM.';
          this.loadBomForProduct(productId);
        },
        error: (err) => {
          this.bomSubmitting = false;
          this.bomErrorMessage = this.extractErrorMessage(
            err,
            'Unable to add material to BOM.'
          );
          this.cdr.detectChanges();
        },
      });
  }

  updateBomQuantity(item: ProductMaterial): void {
    if (!item.productMaterialId || !this.selectedBomProduct?.productId) {
      return;
    }
    this.bomErrorMessage = '';
    this.bomSuccessMessage = '';

    if (!item.quantityRequired || item.quantityRequired <= 0) {
      this.bomErrorMessage = 'Quantity required must be greater than zero.';
      return;
    }

    this.bomSubmitting = true;
    const productId = this.selectedBomProduct.productId;
    this.productMaterialService
      .updateProductMaterial(item.productMaterialId, Number(item.quantityRequired))
      .subscribe({
        next: () => {
          this.bomSubmitting = false;
          this.bomSuccessMessage = `Updated quantity for ${item.material.materialName}.`;
          this.loadBomForProduct(productId);
        },
        error: (err) => {
          this.bomSubmitting = false;
          this.bomErrorMessage = this.extractErrorMessage(
            err,
            'Unable to update BOM quantity.'
          );
          this.cdr.detectChanges();
        },
      });
  }

  removeBomMaterial(item: ProductMaterial): void {
    if (!item.productMaterialId || !this.selectedBomProduct?.productId) {
      return;
    }
    if (!confirm(`Remove ${item.material.materialName} from this product BOM?`)) {
      return;
    }

    const productId = this.selectedBomProduct.productId;
    this.bomErrorMessage = '';
    this.bomSuccessMessage = '';

    this.productMaterialService
      .deleteProductMaterial(item.productMaterialId)
      .subscribe({
        next: () => {
          this.bomSuccessMessage = `Removed ${item.material.materialName} from BOM.`;
          this.loadBomForProduct(productId);
        },
        error: (err) => {
          this.bomErrorMessage = this.extractErrorMessage(
            err,
            'Unable to remove material from BOM.'
          );
          this.cdr.detectChanges();
        },
      });
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
