package com.hcl.Order_Management_System.DTO;

public class ProductMaterialRequest {

    private Long productId;
    private ProductRef product;
    private Long materialId;
    private MaterialRef material;
    private Double quantityRequired;

    public Long getEffectiveProductId() {
        if (productId != null) {
            return productId;
        }
        return product != null ? product.getProductId() : null;
    }

    public Long getEffectiveMaterialId() {
        if (materialId != null) {
            return materialId;
        }
        return material != null ? material.getMaterialId() : null;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public ProductRef getProduct() {
        return product;
    }

    public void setProduct(ProductRef product) {
        this.product = product;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public MaterialRef getMaterial() {
        return material;
    }

    public void setMaterial(MaterialRef material) {
        this.material = material;
    }

    public Double getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(Double quantityRequired) {
        this.quantityRequired = quantityRequired;
    }

    public static class ProductRef {
        private Long productId;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }
    }

    public static class MaterialRef {
        private Long materialId;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
                this.materialId = materialId;
        }
    }
}
