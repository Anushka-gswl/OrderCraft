package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.DashboardSummaryResponse;
import com.hcl.Order_Management_System.DTO.DashboardSummaryResponse.LowStockAlertDto;
import com.hcl.Order_Management_System.DTO.DashboardSummaryResponse.RecentInvoiceDto;
import com.hcl.Order_Management_System.DTO.DashboardSummaryResponse.RecentOrderDto;
import com.hcl.Order_Management_System.Model.CustomerOrder;
import com.hcl.Order_Management_System.Model.Inventory;
import com.hcl.Order_Management_System.Model.Invoice;
import com.hcl.Order_Management_System.Model.Material;
import com.hcl.Order_Management_System.Model.Product;
import com.hcl.Order_Management_System.Model.PurchaseOrder;
import com.hcl.Order_Management_System.Repository.CustomerOrderRepository;
import com.hcl.Order_Management_System.Repository.CustomerRepository;
import com.hcl.Order_Management_System.Repository.InventoryRepository;
import com.hcl.Order_Management_System.Repository.InvoiceRepository;
import com.hcl.Order_Management_System.Repository.MaterialRepository;
import com.hcl.Order_Management_System.Repository.ProductRepository;
import com.hcl.Order_Management_System.Repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InvoiceRepository invoiceRepository;

    public DashboardService(
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            MaterialRepository materialRepository,
            InventoryRepository inventoryRepository,
            CustomerOrderRepository customerOrderRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            InvoiceRepository invoiceRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.materialRepository = materialRepository;
        this.inventoryRepository = inventoryRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        DashboardSummaryResponse summary = new DashboardSummaryResponse();

        // Customers, Products, Materials
        summary.setTotalCustomers(customerRepository.count());

        List<Product> products = productRepository.findAll();
        summary.setTotalProducts(products.size());
        summary.setActiveProducts(products.stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActive()))
                .count());

        summary.setTotalMaterials(materialRepository.count());

        // Customer Orders
        List<CustomerOrder> orders = customerOrderRepository.findAllByOrderByOrderDateDesc();
        summary.setTotalOrders(orders.size());

        long newCount = 0;
        long inProgressCount = 0;
        long completedCount = 0;
        long cancelledCount = 0;
        BigDecimal orderRevenue = BigDecimal.ZERO;

        for (CustomerOrder order : orders) {
            String status = order.getStatus() != null ? order.getStatus().toUpperCase() : "";
            switch (status) {
                case "NEW" -> newCount++;
                case "IN_PROGRESS" -> inProgressCount++;
                case "COMPLETED" -> completedCount++;
                case "CANCELLED" -> cancelledCount++;
                default -> { }
            }
            if (!"CANCELLED".equals(status) && order.getTotalAmount() != null) {
                orderRevenue = orderRevenue.add(BigDecimal.valueOf(order.getTotalAmount()));
            }
        }

        summary.setNewOrders(newCount);
        summary.setInProgressOrders(inProgressCount);
        summary.setCompletedOrders(completedCount);
        summary.setCancelledOrders(cancelledCount);
        summary.setTotalOrderRevenue(round(orderRevenue));

        summary.setRecentOrders(orders.stream().limit(5).map(o -> {
            RecentOrderDto dto = new RecentOrderDto();
            dto.setOrderId(o.getOrderId());
            dto.setOrderNumber(o.getOrderNumber());
            dto.setCustomerName(o.getCustomer() != null ? o.getCustomer().getCustomerName() : "—");
            dto.setStatus(o.getStatus());
            dto.setTotalAmount(o.getTotalAmount() != null ? o.getTotalAmount() : 0.0);
            dto.setOrderDate(o.getOrderDate());
            return dto;
        }).toList());

        // Purchase Orders
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll();
        summary.setTotalPurchaseOrders(purchaseOrders.size());
        long pendingPo = 0;
        long receivedPo = 0;
        BigDecimal poSpend = BigDecimal.ZERO;

        for (PurchaseOrder po : purchaseOrders) {
            String status = po.getStatus() != null ? po.getStatus().toUpperCase() : "";
            if ("CREATED".equals(status)) {
                pendingPo++;
            } else if ("RECEIVED".equals(status)) {
                receivedPo++;
            }
            if (!"CANCELLED".equals(status) && po.getTotalAmount() != null) {
                poSpend = poSpend.add(BigDecimal.valueOf(po.getTotalAmount()));
            }
        }
        summary.setPendingPurchaseOrders(pendingPo);
        summary.setReceivedPurchaseOrders(receivedPo);
        summary.setTotalPurchaseSpend(round(poSpend));

        // Invoices
        List<Invoice> invoices = invoiceRepository.findAllByOrderByInvoiceDateDesc();
        summary.setTotalInvoices(invoices.size());
        long paidInv = 0;
        long unpaidInv = 0;
        BigDecimal invoicedAmt = BigDecimal.ZERO;
        BigDecimal paidAmt = BigDecimal.ZERO;
        BigDecimal outstandingAmt = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            String status = inv.getStatus() != null ? inv.getStatus().toUpperCase() : "";
            BigDecimal amt = inv.getTotalAmount() != null
                    ? BigDecimal.valueOf(inv.getTotalAmount())
                    : BigDecimal.ZERO;

            if (!"CANCELLED".equals(status)) {
                invoicedAmt = invoicedAmt.add(amt);
            }
            if ("PAID".equals(status)) {
                paidInv++;
                paidAmt = paidAmt.add(amt);
            } else if ("DRAFT".equals(status) || "SENT".equals(status) || "OVERDUE".equals(status)) {
                unpaidInv++;
                outstandingAmt = outstandingAmt.add(amt);
            }
        }

        summary.setPaidInvoices(paidInv);
        summary.setUnpaidInvoices(unpaidInv);
        summary.setTotalInvoicedAmount(round(invoicedAmt));
        summary.setPaidInvoiceAmount(round(paidAmt));
        summary.setOutstandingInvoiceAmount(round(outstandingAmt));

        summary.setRecentInvoices(invoices.stream().limit(5).map(inv -> {
            RecentInvoiceDto dto = new RecentInvoiceDto();
            dto.setInvoiceId(inv.getInvoiceId());
            dto.setInvoiceNumber(inv.getInvoiceNumber());
            if (inv.getCustomerOrder() != null) {
                dto.setOrderNumber(inv.getCustomerOrder().getOrderNumber());
                if (inv.getCustomerOrder().getCustomer() != null) {
                    dto.setCustomerName(inv.getCustomerOrder().getCustomer().getCustomerName());
                }
            }
            dto.setStatus(inv.getStatus());
            dto.setTotalAmount(inv.getTotalAmount() != null ? inv.getTotalAmount() : 0.0);
            dto.setInvoiceDate(inv.getInvoiceDate());
            return dto;
        }).toList());

        // Low Stock Alerts
        List<Inventory> inventories = inventoryRepository.findAll();
        List<LowStockAlertDto> alerts = new ArrayList<>();
        for (Inventory inv : inventories) {
            Material m = inv.getMaterial();
            if (m == null) {
                continue;
            }
            double available = inv.getQuantityAvailable() != null ? inv.getQuantityAvailable() : 0.0;
            double reorder = m.getReorderLevel() != null ? m.getReorderLevel() : 0.0;
            if (available <= reorder) {
                LowStockAlertDto alert = new LowStockAlertDto();
                alert.setMaterialId(m.getMaterialId());
                alert.setMaterialCode(m.getMaterialCode());
                alert.setMaterialName(m.getMaterialName());
                alert.setUnit(m.getUnit());
                alert.setQuantityAvailable(available);
                alert.setReorderLevel(reorder);
                alerts.add(alert);
            }
        }
        summary.setLowStockAlerts(alerts);

        return summary;
    }

    private double round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
