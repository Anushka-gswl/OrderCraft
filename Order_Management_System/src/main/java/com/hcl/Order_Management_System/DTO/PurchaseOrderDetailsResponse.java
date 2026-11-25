package com.hcl.Order_Management_System.DTO;

import com.hcl.Order_Management_System.Model.CustomerOrder;
import com.hcl.Order_Management_System.Model.PurchaseOrder;
import com.hcl.Order_Management_System.Model.PurchaseOrderItem;

import java.time.LocalDateTime;
import java.util.List;

public class PurchaseOrderDetailsResponse {

    private Long purchaseOrderId;
    private String poNumber;
    private String supplierName;
    private CustomerOrder customerOrder;
    private LocalDateTime orderDate;
    private String status;
    private Double totalAmount;
    private List<PurchaseOrderItem> items;

    public PurchaseOrderDetailsResponse() {
    }

    public static PurchaseOrderDetailsResponse from(PurchaseOrder po, List<PurchaseOrderItem> items) {
        PurchaseOrderDetailsResponse response = new PurchaseOrderDetailsResponse();
        response.setPurchaseOrderId(po.getPurchaseOrderId());
        response.setPoNumber(po.getPoNumber());
        response.setSupplierName(po.getSupplierName());
        response.setCustomerOrder(po.getCustomerOrder());
        response.setOrderDate(po.getOrderDate());
        response.setStatus(po.getStatus());
        response.setTotalAmount(po.getTotalAmount());
        response.setItems(items);
        return response;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
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

    public CustomerOrder getCustomerOrder() {
        return customerOrder;
    }

    public void setCustomerOrder(CustomerOrder customerOrder) {
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

    public List<PurchaseOrderItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItem> items) {
        this.items = items;
    }
}
