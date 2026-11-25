package com.hcl.Order_Management_System.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class DashboardSummaryResponse {

    private long totalCustomers;
    private long totalProducts;
    private long activeProducts;
    private long totalMaterials;

    private long totalOrders;
    private long newOrders;
    private long inProgressOrders;
    private long completedOrders;
    private long cancelledOrders;

    private long totalPurchaseOrders;
    private long pendingPurchaseOrders;
    private long receivedPurchaseOrders;

    private long totalInvoices;
    private long paidInvoices;
    private long unpaidInvoices;

    private double totalOrderRevenue;
    private double totalInvoicedAmount;
    private double paidInvoiceAmount;
    private double outstandingInvoiceAmount;
    private double totalPurchaseSpend;

    private List<LowStockAlertDto> lowStockAlerts;
    private List<RecentOrderDto> recentOrders;
    private List<RecentInvoiceDto> recentInvoices;

    public DashboardSummaryResponse() {
    }

    public static class LowStockAlertDto {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String unit;
        private Double quantityAvailable;
        private Double reorderLevel;

        public LowStockAlertDto() {
        }

        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long materialId) { this.materialId = materialId; }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public Double getQuantityAvailable() { return quantityAvailable; }
        public void setQuantityAvailable(Double quantityAvailable) { this.quantityAvailable = quantityAvailable; }

        public Double getReorderLevel() { return reorderLevel; }
        public void setReorderLevel(Double reorderLevel) { this.reorderLevel = reorderLevel; }
    }

    public static class RecentOrderDto {
        private Long orderId;
        private String orderNumber;
        private String customerName;
        private String status;
        private Double totalAmount;
        private LocalDateTime orderDate;

        public RecentOrderDto() {
        }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

        public LocalDateTime getOrderDate() { return orderDate; }
        public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    }

    public static class RecentInvoiceDto {
        private Long invoiceId;
        private String invoiceNumber;
        private String orderNumber;
        private String customerName;
        private String status;
        private Double totalAmount;
        private LocalDateTime invoiceDate;

        public RecentInvoiceDto() {
        }

        public Long getInvoiceId() { return invoiceId; }
        public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

        public String getInvoiceNumber() { return invoiceNumber; }
        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

        public LocalDateTime getInvoiceDate() { return invoiceDate; }
        public void setInvoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; }
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }

    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }

    public long getActiveProducts() { return activeProducts; }
    public void setActiveProducts(long activeProducts) { this.activeProducts = activeProducts; }

    public long getTotalMaterials() { return totalMaterials; }
    public void setTotalMaterials(long totalMaterials) { this.totalMaterials = totalMaterials; }

    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

    public long getNewOrders() { return newOrders; }
    public void setNewOrders(long newOrders) { this.newOrders = newOrders; }

    public long getInProgressOrders() { return inProgressOrders; }
    public void setInProgressOrders(long inProgressOrders) { this.inProgressOrders = inProgressOrders; }

    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }

    public long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public long getTotalPurchaseOrders() { return totalPurchaseOrders; }
    public void setTotalPurchaseOrders(long totalPurchaseOrders) { this.totalPurchaseOrders = totalPurchaseOrders; }

    public long getPendingPurchaseOrders() { return pendingPurchaseOrders; }
    public void setPendingPurchaseOrders(long pendingPurchaseOrders) { this.pendingPurchaseOrders = pendingPurchaseOrders; }

    public long getReceivedPurchaseOrders() { return receivedPurchaseOrders; }
    public void setReceivedPurchaseOrders(long receivedPurchaseOrders) { this.receivedPurchaseOrders = receivedPurchaseOrders; }

    public long getTotalInvoices() { return totalInvoices; }
    public void setTotalInvoices(long totalInvoices) { this.totalInvoices = totalInvoices; }

    public long getPaidInvoices() { return paidInvoices; }
    public void setPaidInvoices(long paidInvoices) { this.paidInvoices = paidInvoices; }

    public long getUnpaidInvoices() { return unpaidInvoices; }
    public void setUnpaidInvoices(long unpaidInvoices) { this.unpaidInvoices = unpaidInvoices; }

    public double getTotalOrderRevenue() { return totalOrderRevenue; }
    public void setTotalOrderRevenue(double totalOrderRevenue) { this.totalOrderRevenue = totalOrderRevenue; }

    public double getTotalInvoicedAmount() { return totalInvoicedAmount; }
    public void setTotalInvoicedAmount(double totalInvoicedAmount) { this.totalInvoicedAmount = totalInvoicedAmount; }

    public double getPaidInvoiceAmount() { return paidInvoiceAmount; }
    public void setPaidInvoiceAmount(double paidInvoiceAmount) { this.paidInvoiceAmount = paidInvoiceAmount; }

    public double getOutstandingInvoiceAmount() { return outstandingInvoiceAmount; }
    public void setOutstandingInvoiceAmount(double outstandingInvoiceAmount) { this.outstandingInvoiceAmount = outstandingInvoiceAmount; }

    public double getTotalPurchaseSpend() { return totalPurchaseSpend; }
    public void setTotalPurchaseSpend(double totalPurchaseSpend) { this.totalPurchaseSpend = totalPurchaseSpend; }

    public List<LowStockAlertDto> getLowStockAlerts() { return lowStockAlerts; }
    public void setLowStockAlerts(List<LowStockAlertDto> lowStockAlerts) { this.lowStockAlerts = lowStockAlerts; }

    public List<RecentOrderDto> getRecentOrders() { return recentOrders; }
    public void setRecentOrders(List<RecentOrderDto> recentOrders) { this.recentOrders = recentOrders; }

    public List<RecentInvoiceDto> getRecentInvoices() { return recentInvoices; }
    public void setRecentInvoices(List<RecentInvoiceDto> recentInvoices) { this.recentInvoices = recentInvoices; }
}
