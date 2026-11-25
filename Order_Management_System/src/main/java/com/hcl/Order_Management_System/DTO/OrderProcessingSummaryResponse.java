package com.hcl.Order_Management_System.DTO;

import com.hcl.Order_Management_System.Model.PurchaseOrder;

import java.util.List;

public class OrderProcessingSummaryResponse {

    private Long orderId;
    private String orderNumber;
    private String status;
    private boolean hasOrderItems;
    private boolean sufficientInventory;
    private boolean activeCreatedPoExists;
    private boolean canGeneratePurchaseOrder;
    private boolean canStartProcessing;
    private boolean canComplete;
    private boolean canCancel;
    private List<String> productsWithoutBom;
    private List<MaterialRequirementItem> requirements;
    private List<PurchaseOrder> linkedPurchaseOrders;

    public static class MaterialRequirementItem {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String unit;
        private Double requiredQuantity;
        private Double availableQuantity;
        private Double shortageQuantity;
        private Double unitCost;
        private String status;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public String getMaterialCode() {
            return materialCode;
        }

        public void setMaterialCode(String materialCode) {
            this.materialCode = materialCode;
        }

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Double getRequiredQuantity() {
            return requiredQuantity;
        }

        public void setRequiredQuantity(Double requiredQuantity) {
            this.requiredQuantity = requiredQuantity;
        }

        public Double getAvailableQuantity() {
            return availableQuantity;
        }

        public void setAvailableQuantity(Double availableQuantity) {
            this.availableQuantity = availableQuantity;
        }

        public Double getShortageQuantity() {
            return shortageQuantity;
        }

        public void setShortageQuantity(Double shortageQuantity) {
            this.shortageQuantity = shortageQuantity;
        }

        public Double getUnitCost() {
            return unitCost;
        }

        public void setUnitCost(Double unitCost) {
            this.unitCost = unitCost;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHasOrderItems() {
        return hasOrderItems;
    }

    public void setHasOrderItems(boolean hasOrderItems) {
        this.hasOrderItems = hasOrderItems;
    }

    public boolean isSufficientInventory() {
        return sufficientInventory;
    }

    public void setSufficientInventory(boolean sufficientInventory) {
        this.sufficientInventory = sufficientInventory;
    }

    public boolean isActiveCreatedPoExists() {
        return activeCreatedPoExists;
    }

    public void setActiveCreatedPoExists(boolean activeCreatedPoExists) {
        this.activeCreatedPoExists = activeCreatedPoExists;
    }

    public boolean isCanGeneratePurchaseOrder() {
        return canGeneratePurchaseOrder;
    }

    public void setCanGeneratePurchaseOrder(boolean canGeneratePurchaseOrder) {
        this.canGeneratePurchaseOrder = canGeneratePurchaseOrder;
    }

    public boolean isCanStartProcessing() {
        return canStartProcessing;
    }

    public void setCanStartProcessing(boolean canStartProcessing) {
        this.canStartProcessing = canStartProcessing;
    }

    public boolean isCanComplete() {
        return canComplete;
    }

    public void setCanComplete(boolean canComplete) {
        this.canComplete = canComplete;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public void setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
    }

    public List<String> getProductsWithoutBom() {
        return productsWithoutBom;
    }

    public void setProductsWithoutBom(List<String> productsWithoutBom) {
        this.productsWithoutBom = productsWithoutBom;
    }

    public List<MaterialRequirementItem> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<MaterialRequirementItem> requirements) {
        this.requirements = requirements;
    }

    public List<PurchaseOrder> getLinkedPurchaseOrders() {
        return linkedPurchaseOrders;
    }

    public void setLinkedPurchaseOrders(List<PurchaseOrder> linkedPurchaseOrders) {
        this.linkedPurchaseOrders = linkedPurchaseOrders;
    }
}
