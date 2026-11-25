import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InventoryService } from '../../services/inventory';
import { Inventory as InventoryModel} from '../../models/inventory';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-inventory',
  imports: [FormsModule],
  templateUrl: './inventory.html',
  styleUrl: './inventory.css'
})
export class Inventory implements OnInit {

  inventory: InventoryModel[] = [];
  loading = false;
  errorMessage = '';

  showForm = false;
  editingId: number | undefined = undefined;

  newInventory: InventoryModel = {
    material: {
      materialId: 0,
      materialCode: '',
      materialName: '',
      unit: '',
      reorderLevel: 0,
      unitCost: 0
    },
    quantityAvailable: 0
  };

  constructor(
    private inventoryService: InventoryService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  get canManageInventory(): boolean {
    return this.authService.hasAnyRole('ADMIN', 'INVENTORY_MANAGER');
  }

  ngOnInit(): void {
    this.loadInventory();
  }

  loadInventory(): void {
    this.loading = true;

    this.inventoryService.getInventory().subscribe({
      next: (data) => {
        this.inventory = data;
        this.loading = false;

        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Inventory API error:', error);

        this.errorMessage = 'Unable to load inventory.';
        this.loading = false;

        this.cdr.detectChanges();
      }
    });
  }

  openAddInventoryForm(): void {
    this.editingId = undefined;

    this.newInventory = {
      material: {
        materialId: 0,
        materialCode: '',
        materialName: '',
        unit: '',
        reorderLevel: 0,
        unitCost: 0
      },
      quantityAvailable: 0
    };

    this.errorMessage = '';
    this.showForm = true;

    this.cdr.detectChanges();
  }

  openEditInventoryForm(item: InventoryModel): void {
    this.editingId = item.inventoryId;

    this.newInventory = {
      material: {
        materialId: item.material.materialId,
        materialCode: item.material.materialCode,
        materialName: item.material.materialName,
        unit: item.material.unit,
        reorderLevel: item.material.reorderLevel,
        unitCost: item.material.unitCost
      },
      quantityAvailable: item.quantityAvailable
    };

    this.errorMessage = '';
    this.showForm = true;

    this.cdr.detectChanges();
  }

  closeInventoryForm(): void {
    this.showForm = false;
    this.errorMessage = '';
    this.editingId = undefined;

    this.cdr.detectChanges();
  }

  saveInventory(): void {

    if (
      this.newInventory.material.materialId <= 0 ||
      this.newInventory.quantityAvailable < 0
    ) {
      this.errorMessage =
        'Please enter a valid material ID and quantity.';
      return;
    }

    if (this.editingId !== undefined) {

      this.inventoryService
        .updateInventory(this.editingId, this.newInventory)
        .subscribe({
          next: () => {
            this.closeInventoryForm();
            this.loadInventory();
          },
          error: (error) => {
            console.error('Inventory update error:', error);

            this.errorMessage = 'Unable to update inventory.';
            this.cdr.detectChanges();
          }
        });

    } else {

      this.inventoryService
        .createInventory(this.newInventory)
        .subscribe({
          next: () => {
            this.closeInventoryForm();
            this.loadInventory();
          },
          error: (error) => {
            console.error('Inventory creation error:', error);

            this.errorMessage = 'Unable to create inventory.';
            this.cdr.detectChanges();
          }
        });
    }
  }

  deleteInventory(id: number | undefined): void {

    if (id === undefined) {
      return;
    }

    if (!confirm('Are you sure you want to delete this inventory record?')) {
      return;
    }

    this.inventoryService.deleteInventory(id).subscribe({
      next: () => {
        this.loadInventory();
      },
      error: () => {
        this.errorMessage = 'Unable to delete inventory.';
        this.cdr.detectChanges();
      }
    });
  }
}