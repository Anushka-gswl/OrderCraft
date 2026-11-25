package com.hcl.Order_Management_System.DTO;

public class OrderItemRequest {

    private Long productId;
    private ProductRef product;
    private Integer quantity;

    public Long getEffectiveProductId() {
        if (productId != null) {
            return productId;
        }
        return product != null ? product.getProductId() : null;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
}
