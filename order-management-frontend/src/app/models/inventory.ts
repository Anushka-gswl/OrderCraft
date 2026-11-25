export interface Inventory {
  inventoryId?: number;
  material: {
    materialId: number;
    materialCode: string;
    materialName: string;
    unit: string;
    reorderLevel: number;
    unitCost: number;
  };
  quantityAvailable: number;
}