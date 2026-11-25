import { Material } from './material';
import { Product } from './product';

export interface ProductMaterial {
  productMaterialId?: number;
  product: Product;
  material: Material;
  quantityRequired: number;
}

export interface ProductMaterialRequest {
  productId: number;
  materialId: number;
  quantityRequired: number;
}
