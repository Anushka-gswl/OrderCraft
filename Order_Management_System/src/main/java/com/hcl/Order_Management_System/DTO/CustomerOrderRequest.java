package com.hcl.Order_Management_System.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class CustomerOrderRequest {

    private String orderNumber;
    private Long customerId;
    private CustomerRef customer;
    private LocalDateTime orderDate;
    private String status;
    private Double totalAmount;
    private String idempotencyKey;
    private List<OrderItemRequest> items;

    public Long getEffectiveCustomerId() {
        if (customerId != null) {
            return customerId;
        }
        return customer != null ? customer.getCustomerId() : null;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public CustomerRef getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerRef customer) {
        this.customer = customer;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public static class CustomerRef {
        private Long customerId;

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }
    }
}
