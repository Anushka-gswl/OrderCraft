package com.hcl.Order_Management_System.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(nullable = false, length = 64)
    private String productCode;

    private Integer quantity;

    private Double unitPrice;

    private Double subtotal;

    public InvoiceItem() {
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getInvoiceItemId() { return invoiceItemId; }
    public void setInvoiceItemId(Long invoiceItemId) { this.invoiceItemId = invoiceItemId; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

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
