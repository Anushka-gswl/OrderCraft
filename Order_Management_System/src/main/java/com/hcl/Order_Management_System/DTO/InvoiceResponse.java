package com.hcl.Order_Management_System.DTO;

import com.hcl.Order_Management_System.Model.Invoice;
import com.hcl.Order_Management_System.Model.InvoiceItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class InvoiceResponse {

    private Long invoiceId;
    private String invoiceNumber;
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private LocalDateTime invoiceDate;
    private LocalDate dueDate;
    private String status;
    private Double subtotal;
    private Double taxRate;
    private Double taxAmount;
    private Double totalAmount;
    private String notes;
    private LocalDateTime createdAt;
    private List<InvoiceItemDto> items;

    public InvoiceResponse() {
    }

    public static class InvoiceItemDto {
        private Long invoiceItemId;
        private String productName;
        private String productCode;
        private Integer quantity;
        private Double unitPrice;
        private Double subtotal;

        public InvoiceItemDto() {
        }

        public static InvoiceItemDto from(InvoiceItem item) {
            InvoiceItemDto dto = new InvoiceItemDto();
            dto.setInvoiceItemId(item.getInvoiceItemId());
            dto.setProductName(item.getProductName());
            dto.setProductCode(item.getProductCode());
            dto.setQuantity(item.getQuantity());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setSubtotal(item.getSubtotal());
            return dto;
        }

        public Long getInvoiceItemId() { return invoiceItemId; }
        public void setInvoiceItemId(Long invoiceItemId) { this.invoiceItemId = invoiceItemId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

        public Double getSubtotal() { return subtotal; }
        public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    }

    public static InvoiceResponse from(Invoice invoice) {
        InvoiceResponse r = new InvoiceResponse();
        r.setInvoiceId(invoice.getInvoiceId());
        r.setInvoiceNumber(invoice.getInvoiceNumber());
        if (invoice.getCustomerOrder() != null) {
            r.setOrderId(invoice.getCustomerOrder().getOrderId());
            r.setOrderNumber(invoice.getCustomerOrder().getOrderNumber());
            if (invoice.getCustomerOrder().getCustomer() != null) {
                r.setCustomerId(invoice.getCustomerOrder().getCustomer().getCustomerId());
                r.setCustomerName(invoice.getCustomerOrder().getCustomer().getCustomerName());
            }
        }
        r.setInvoiceDate(invoice.getInvoiceDate());
        r.setDueDate(invoice.getDueDate());
        r.setStatus(invoice.getStatus());
        r.setSubtotal(invoice.getSubtotal());
        r.setTaxRate(invoice.getTaxRate());
        r.setTaxAmount(invoice.getTaxAmount());
        r.setTotalAmount(invoice.getTotalAmount());
        r.setNotes(invoice.getNotes());
        r.setCreatedAt(invoice.getCreatedAt());
        r.setItems(
                invoice.getItems() != null
                        ? invoice.getItems().stream().map(InvoiceItemDto::from).toList()
                        : List.of()
        );
        return r;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public LocalDateTime getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }

    public Double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(Double taxAmount) { this.taxAmount = taxAmount; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<InvoiceItemDto> getItems() { return items; }
    public void setItems(List<InvoiceItemDto> items) { this.items = items; }
}
