package com.hcl.Order_Management_System.DTO;

public class PurchaseOrderItemRequest {

    private Long materialId;
    private MaterialRef material;
    private Double quantity;

    public Long getEffectiveMaterialId() {
        if (materialId != null) {
            return materialId;
        }
        return material != null ? material.getMaterialId() : null;
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

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
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
