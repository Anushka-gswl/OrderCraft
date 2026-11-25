package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.DashboardSummaryResponse;
import com.hcl.Order_Management_System.Model.*;
import com.hcl.Order_Management_System.Repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock ProductRepository productRepository;
    @Mock MaterialRepository materialRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock CustomerOrderRepository customerOrderRepository;
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock InvoiceRepository invoiceRepository;

    @InjectMocks DashboardService dashboardService;

    @Test
    void getSummary_emptyDatabase_returnsZeros() {
        when(customerRepository.count()).thenReturn(0L);
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(materialRepository.count()).thenReturn(0L);
        when(customerOrderRepository.findAllByOrderByOrderDateDesc()).thenReturn(Collections.emptyList());
        when(purchaseOrderRepository.findAll()).thenReturn(Collections.emptyList());
        when(invoiceRepository.findAllByOrderByInvoiceDateDesc()).thenReturn(Collections.emptyList());
        when(inventoryRepository.findAll()).thenReturn(Collections.emptyList());

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.getTotalOrders()).isZero();
        assertThat(summary.getTotalOrderRevenue()).isZero();
        assertThat(summary.getLowStockAlerts()).isEmpty();
        assertThat(summary.getRecentOrders()).isEmpty();
        assertThat(summary.getRecentInvoices()).isEmpty();
    }

    @Test
    void getSummary_aggregatesOrdersInvoicesPurchaseOrdersAndLowStock() {
        when(customerRepository.count()).thenReturn(2L);

        Product p1 = new Product();
        p1.setActive(true);
        Product p2 = new Product();
        p2.setActive(false);
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));
        when(materialRepository.count()).thenReturn(3L);

        Customer cust = new Customer();
        cust.setCustomerName("Acme");

        CustomerOrder o1 = new CustomerOrder();
        o1.setOrderId(1L);
        o1.setOrderNumber("ORD-1");
        o1.setCustomer(cust);
        o1.setStatus("COMPLETED");
        o1.setTotalAmount(1000.0);
        o1.setOrderDate(LocalDateTime.now());

        CustomerOrder o2 = new CustomerOrder();
        o2.setOrderId(2L);
        o2.setOrderNumber("ORD-2");
        o2.setCustomer(cust);
        o2.setStatus("CANCELLED");
        o2.setTotalAmount(500.0);
        o2.setOrderDate(LocalDateTime.now());

        when(customerOrderRepository.findAllByOrderByOrderDateDesc()).thenReturn(List.of(o1, o2));

        PurchaseOrder po1 = new PurchaseOrder();
        po1.setStatus("CREATED");
        po1.setTotalAmount(300.0);
        PurchaseOrder po2 = new PurchaseOrder();
        po2.setStatus("RECEIVED");
        po2.setTotalAmount(200.0);
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(po1, po2));

        Invoice inv1 = new Invoice();
        inv1.setInvoiceId(1L);
        inv1.setInvoiceNumber("INV-1");
        inv1.setCustomerOrder(o1);
        inv1.setStatus("PAID");
        inv1.setTotalAmount(1180.0);
        inv1.setInvoiceDate(LocalDateTime.now());

        Invoice inv2 = new Invoice();
        inv2.setInvoiceId(2L);
        inv2.setInvoiceNumber("INV-2");
        inv2.setCustomerOrder(o1);
        inv2.setStatus("SENT");
        inv2.setTotalAmount(400.0);
        inv2.setInvoiceDate(LocalDateTime.now());

        when(invoiceRepository.findAllByOrderByInvoiceDateDesc()).thenReturn(List.of(inv1, inv2));

        Material lowMat = new Material();
        lowMat.setMaterialId(1L);
        lowMat.setMaterialCode("MAT-1");
        lowMat.setMaterialName("Steel");
        lowMat.setUnit("kg");
        lowMat.setReorderLevel(50.0);

        Inventory lowInv = new Inventory();
        lowInv.setMaterial(lowMat);
        lowInv.setQuantityAvailable(25.0);

        Material okMat = new Material();
        okMat.setMaterialId(2L);
        okMat.setMaterialCode("MAT-2");
        okMat.setMaterialName("Copper");
        okMat.setUnit("kg");
        okMat.setReorderLevel(10.0);

        Inventory okInv = new Inventory();
        okInv.setMaterial(okMat);
        okInv.setQuantityAvailable(100.0);

        when(inventoryRepository.findAll()).thenReturn(List.of(lowInv, okInv));

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.getTotalCustomers()).isEqualTo(2L);
        assertThat(summary.getTotalProducts()).isEqualTo(2L);
        assertThat(summary.getActiveProducts()).isEqualTo(1L);
        assertThat(summary.getTotalOrders()).isEqualTo(2L);
        assertThat(summary.getCompletedOrders()).isEqualTo(1L);
        assertThat(summary.getCancelledOrders()).isEqualTo(1L);
        assertThat(summary.getTotalOrderRevenue()).isEqualTo(1000.0);

        assertThat(summary.getTotalPurchaseOrders()).isEqualTo(2L);
        assertThat(summary.getPendingPurchaseOrders()).isEqualTo(1L);
        assertThat(summary.getReceivedPurchaseOrders()).isEqualTo(1L);
        assertThat(summary.getTotalPurchaseSpend()).isEqualTo(500.0);

        assertThat(summary.getTotalInvoices()).isEqualTo(2L);
        assertThat(summary.getPaidInvoices()).isEqualTo(1L);
        assertThat(summary.getUnpaidInvoices()).isEqualTo(1L);
        assertThat(summary.getTotalInvoicedAmount()).isEqualTo(1580.0);
        assertThat(summary.getPaidInvoiceAmount()).isEqualTo(1180.0);
        assertThat(summary.getOutstandingInvoiceAmount()).isEqualTo(400.0);

        assertThat(summary.getLowStockAlerts()).hasSize(1);
        assertThat(summary.getLowStockAlerts().get(0).getMaterialCode()).isEqualTo("MAT-1");
        assertThat(summary.getRecentOrders()).hasSize(2);
        assertThat(summary.getRecentInvoices()).hasSize(2);
    }
}
