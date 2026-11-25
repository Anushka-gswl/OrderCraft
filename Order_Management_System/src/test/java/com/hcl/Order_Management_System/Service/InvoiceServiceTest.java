package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.InvoiceRequest;
import com.hcl.Order_Management_System.DTO.InvoiceResponse;
import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ConflictException;
import com.hcl.Order_Management_System.Exception.ResourceNotFoundException;
import com.hcl.Order_Management_System.Model.*;
import com.hcl.Order_Management_System.Repository.CustomerOrderRepository;
import com.hcl.Order_Management_System.Repository.InvoiceRepository;
import com.hcl.Order_Management_System.Repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock CustomerOrderRepository customerOrderRepository;
    @Mock OrderItemRepository orderItemRepository;

    @InjectMocks InvoiceService invoiceService;

    private CustomerOrder completedOrder;
    private OrderItem sampleOrderItem;
    private Product product;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setCustomerName("Test Customer");

        completedOrder = new CustomerOrder();
        completedOrder.setOrderId(10L);
        completedOrder.setOrderNumber("ORD-TEST-001");
        completedOrder.setStatus("COMPLETED");
        completedOrder.setTotalAmount(500.0);
        completedOrder.setCustomer(customer);

        product = new Product();
        product.setProductId(5L);
        product.setProductName("Widget A");
        product.setProductCode("WGT-A");
        product.setUnitPrice(100.0);
        product.setActive(true);

        sampleOrderItem = new OrderItem();
        sampleOrderItem.setOrderItemId(1L);
        sampleOrderItem.setOrder(completedOrder);
        sampleOrderItem.setProduct(product);
        sampleOrderItem.setQuantity(5);
        sampleOrderItem.setUnitPrice(100.0);
        sampleOrderItem.setSubtotal(500.0);
    }

    // ── generateInvoice ────────────────────────────────────────────────────────

    @Test
    void generateInvoice_success_noTax() {
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(completedOrder));
        when(invoiceRepository.existsByCustomerOrderOrderId(10L)).thenReturn(false);
        when(orderItemRepository.findByOrderOrderId(10L)).thenReturn(List.of(sampleOrderItem));
        when(invoiceRepository.findByInvoiceNumber(anyString())).thenReturn(Optional.empty());

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(captor.capture())).thenAnswer(inv -> {
            Invoice saved = captor.getValue();
            saved.setInvoiceId(1L);
            return saved;
        });

        InvoiceRequest req = new InvoiceRequest();
        req.setTaxRate(0.0);

        InvoiceResponse resp = invoiceService.generateInvoice(10L, req);

        assertThat(resp.getSubtotal()).isEqualTo(500.0);
        assertThat(resp.getTaxRate()).isEqualTo(0.0);
        assertThat(resp.getTaxAmount()).isEqualTo(0.0);
        assertThat(resp.getTotalAmount()).isEqualTo(500.0);
        assertThat(resp.getStatus()).isEqualTo("DRAFT");
        assertThat(resp.getItems()).hasSize(1);
        assertThat(resp.getItems().get(0).getProductName()).isEqualTo("Widget A");
    }

    @Test
    void generateInvoice_success_withTax() {
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(completedOrder));
        when(invoiceRepository.existsByCustomerOrderOrderId(10L)).thenReturn(false);
        when(orderItemRepository.findByOrderOrderId(10L)).thenReturn(List.of(sampleOrderItem));
        when(invoiceRepository.findByInvoiceNumber(anyString())).thenReturn(Optional.empty());

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(captor.capture())).thenAnswer(inv -> {
            Invoice saved = captor.getValue();
            saved.setInvoiceId(2L);
            return saved;
        });

        InvoiceRequest req = new InvoiceRequest();
        req.setTaxRate(18.0);

        InvoiceResponse resp = invoiceService.generateInvoice(10L, req);

        assertThat(resp.getSubtotal()).isEqualTo(500.0);
        assertThat(resp.getTaxRate()).isEqualTo(18.0);
        assertThat(resp.getTaxAmount()).isEqualTo(90.0);  // 18% of 500
        assertThat(resp.getTotalAmount()).isEqualTo(590.0);
    }

    @Test
    void generateInvoice_orderNotFound_throws404() {
        when(customerOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.generateInvoice(99L, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void generateInvoice_orderNotCompleted_throws400() {
        completedOrder.setStatus("NEW");
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(completedOrder));

        assertThatThrownBy(() -> invoiceService.generateInvoice(10L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void generateInvoice_duplicateInvoice_throws409() {
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(completedOrder));
        when(invoiceRepository.existsByCustomerOrderOrderId(10L)).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.generateInvoice(10L, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void generateInvoice_noOrderItems_throws400() {
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(completedOrder));
        when(invoiceRepository.existsByCustomerOrderOrderId(10L)).thenReturn(false);
        when(orderItemRepository.findByOrderOrderId(10L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> invoiceService.generateInvoice(10L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no line items");
    }

    // ── getAllInvoices ─────────────────────────────────────────────────────────

    @Test
    void getAllInvoices_returnsEmptyList() {
        when(invoiceRepository.findAllByOrderByInvoiceDateDesc()).thenReturn(Collections.emptyList());
        assertThat(invoiceService.getAllInvoices()).isEmpty();
    }

    // ── getInvoiceById ─────────────────────────────────────────────────────────

    @Test
    void getInvoiceById_notFound_throws404() {
        when(invoiceRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> invoiceService.getInvoiceById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getInvoiceByOrderId ────────────────────────────────────────────────────

    @Test
    void getInvoiceByOrderId_notFound_throws404() {
        when(invoiceRepository.findByCustomerOrderOrderId(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> invoiceService.getInvoiceByOrderId(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateInvoiceStatus ────────────────────────────────────────────────────

    @Test
    void updateStatus_draftToSent_success() {
        Invoice inv = buildDraftInvoice();
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(inv));
        when(invoiceRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        InvoiceResponse resp = invoiceService.updateInvoiceStatus(1L, "SENT");
        assertThat(resp.getStatus()).isEqualTo("SENT");
    }

    @Test
    void updateStatus_paidInvoice_throws400() {
        Invoice inv = buildDraftInvoice();
        inv.setStatus("PAID");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> invoiceService.updateInvoiceStatus(1L, "CANCELLED"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PAID");
    }

    @Test
    void updateStatus_invalidStatus_throws400() {
        assertThatThrownBy(() -> invoiceService.updateInvoiceStatus(1L, "INVALID"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid invoice status");
    }

    @Test
    void updateStatus_blankStatus_throws400() {
        assertThatThrownBy(() -> invoiceService.updateInvoiceStatus(1L, "  "))
                .isInstanceOf(BadRequestException.class);
    }

    // ── deleteInvoice ──────────────────────────────────────────────────────────

    @Test
    void deleteInvoice_draft_success() {
        Invoice inv = buildDraftInvoice();
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(inv));

        invoiceService.deleteInvoice(1L);

        verify(invoiceRepository).delete(inv);
    }

    @Test
    void deleteInvoice_nonDraft_throws400() {
        Invoice inv = buildDraftInvoice();
        inv.setStatus("SENT");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> invoiceService.deleteInvoice(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void deleteInvoice_notFound_throws404() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.deleteInvoice(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Invoice buildDraftInvoice() {
        Invoice inv = new Invoice();
        inv.setInvoiceId(1L);
        inv.setInvoiceNumber("INV-TEST-0001");
        inv.setCustomerOrder(completedOrder);
        inv.setStatus("DRAFT");
        inv.setSubtotal(500.0);
        inv.setTaxRate(0.0);
        inv.setTaxAmount(0.0);
        inv.setTotalAmount(500.0);
        inv.setItems(List.of());
        return inv;
    }
}
