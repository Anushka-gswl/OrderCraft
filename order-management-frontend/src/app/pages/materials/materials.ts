import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MaterialService } from '../../services/material';
import { Material } from '../../models/material';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-materials',
  imports: [FormsModule],
  templateUrl: './materials.html',
  styleUrl: './materials.css'
})
export class Materials implements OnInit {

  materials: Material[] = [];
  loading = false;
  errorMessage = '';

  showForm = false;

  newMaterial: Material = {
    materialCode: '',
    materialName: '',
    unit: '',
    reorderLevel: 0,
    unitCost: 0
  };

  constructor(
    private materialService: MaterialService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  get canManageMaterials(): boolean {
    return this.authService.hasAnyRole('ADMIN', 'INVENTORY_MANAGER');
  }

  ngOnInit(): void {
    this.loadMaterials();
  }

  loadMaterials(): void {
    this.loading = true;

    this.materialService.getMaterials().subscribe({
      next: (data) => {
        this.materials = data;
        this.loading = false;

        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Material API error:', error);

        this.errorMessage = 'Unable to load materials.';
        this.loading = false;

        this.cdr.detectChanges();
      }
    });
  }

  openAddMaterialForm(): void {
    this.newMaterial = {
      materialCode: '',
      materialName: '',
      unit: '',
      reorderLevel: 0,
      unitCost: 0
    };

    this.errorMessage = '';
    this.showForm = true;

    this.cdr.detectChanges();
  }

  closeAddMaterialForm(): void {
    this.showForm = false;
    this.errorMessage = '';

    this.cdr.detectChanges();
  }

  saveMaterial(): void {

    if (
      !this.newMaterial.materialCode.trim() ||
      !this.newMaterial.materialName.trim() ||
      !this.newMaterial.unit.trim() ||
      this.newMaterial.reorderLevel < 0 ||
      this.newMaterial.unitCost <= 0
    ) {
      this.errorMessage =
        'Please fill in all material details correctly.';
      return;
    }

    this.materialService.createMaterial(this.newMaterial).subscribe({
      next: () => {
        this.showForm = false;
        this.loadMaterials();
      },
      error: (error) => {
        console.error('Material creation error:', error);

        this.errorMessage = 'Unable to create material.';
        this.cdr.detectChanges();
      }
    });
  }

  deleteMaterial(id: number | undefined): void {

    if (id === undefined) {
      return;
    }

    if (!confirm('Are you sure you want to delete this material?')) {
      return;
    }

    this.materialService.deleteMaterial(id).subscribe({
      next: () => {
        this.loadMaterials();
      },
      error: () => {
        this.errorMessage = 'Unable to delete material.';
        this.cdr.detectChanges();
      }
    });
  }
}