package com.hcl.Order_Management_System.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class PurchaseOrderRequest {

    private String poNumber;
    private String supplierName;
    private Long customerOrderId;
    private CustomerOrderRef customerOrder;
    private LocalDateTime orderDate;
    private String status;
    private Double totalAmount;
    private List<PurchaseOrderItemRequest> items;

    public Long getEffectiveCustomerOrderId() {
        if (customerOrderId != null) {
            return customerOrderId;
        }
        return customerOrder != null ? customerOrder.getOrderId() : null;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getCustomerOrderId() {
        return customerOrderId;
    }

    public void setCustomerOrderId(Long customerOrderId) {
        this.customerOrderId = customerOrderId;
    }

    public CustomerOrderRef getCustomerOrder() {
        return customerOrder;
    }

    public void setCustomerOrder(CustomerOrderRef customerOrder) {
        this.customerOrder = customerOrder;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<PurchaseOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItemRequest> items) {
        this.items = items;
    }

    public static class CustomerOrderRef {
        private Long orderId;

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }
    }
}
