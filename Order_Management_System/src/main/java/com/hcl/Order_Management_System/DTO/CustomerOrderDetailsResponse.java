package com.hcl.Order_Management_System.DTO;

import com.hcl.Order_Management_System.Model.Customer;
import com.hcl.Order_Management_System.Model.CustomerOrder;
import com.hcl.Order_Management_System.Model.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public class CustomerOrderDetailsResponse {

    private Long orderId;
    private String orderNumber;
    private Customer customer;
    private LocalDateTime orderDate;
    private String status;
    private Double totalAmount;
    private List<OrderItem> items;

    public CustomerOrderDetailsResponse() {
    }

    public static CustomerOrderDetailsResponse from(CustomerOrder order, List<OrderItem> items) {
        CustomerOrderDetailsResponse response = new CustomerOrderDetailsResponse();
        response.setOrderId(order.getOrderId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCustomer(order.getCustomer());
        response.setOrderDate(order.getOrderDate());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setItems(items);
        return response;
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

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
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

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
