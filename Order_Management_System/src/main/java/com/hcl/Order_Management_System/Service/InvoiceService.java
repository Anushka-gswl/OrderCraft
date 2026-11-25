package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.InvoiceRequest;
import com.hcl.Order_Management_System.DTO.InvoiceResponse;
import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ConflictException;
import com.hcl.Order_Management_System.Exception.ResourceNotFoundException;
import com.hcl.Order_Management_System.Model.CustomerOrder;
import com.hcl.Order_Management_System.Model.Invoice;
import com.hcl.Order_Management_System.Model.InvoiceItem;
import com.hcl.Order_Management_System.Model.OrderItem;
import com.hcl.Order_Management_System.Repository.CustomerOrderRepository;
import com.hcl.Order_Management_System.Repository.InvoiceRepository;
import com.hcl.Order_Management_System.Repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final DateTimeFormatter INV_NUM_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final Set<String> VALID_STATUSES =
            Set.of("DRAFT", "SENT", "PAID", "OVERDUE", "CANCELLED");

    private final InvoiceRepository invoiceRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            CustomerOrderRepository customerOrderRepository,
            OrderItemRepository orderItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // ── Generate Invoice ───────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse generateInvoice(Long orderId, InvoiceRequest request) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer order not found with ID: " + orderId));

        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException(
                    "An invoice can only be generated for a COMPLETED order. "
                            + "Current status: " + order.getStatus());
        }

        if (invoiceRepository.existsByCustomerOrderOrderId(orderId)) {
            throw new ConflictException(
                    "An invoice already exists for order ID: " + orderId
                            + ". Use GET /api/invoices/order/" + orderId + " to retrieve it.");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw new BadRequestException(
                    "Cannot generate an invoice for an order with no line items.");
        }

        // ── Build Invoice ──────────────────────────────────────────────────────
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setCustomerOrder(order);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setStatus("DRAFT");

        double taxRate = (request != null && request.getTaxRate() != null && request.getTaxRate() >= 0)
                ? request.getTaxRate()
                : 0.0;
        invoice.setTaxRate(round(taxRate));

        if (request != null) {
            invoice.setDueDate(request.getDueDate());
            invoice.setNotes(request.getNotes());
        }

        // ── Build InvoiceItems from OrderItems ─────────────────────────────────
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        BigDecimal subtotalAccum = BigDecimal.ZERO;

        for (OrderItem oi : orderItems) {
            InvoiceItem ii = new InvoiceItem();
            ii.setInvoice(invoice);
            if (oi.getProduct() != null) {
                ii.setProductName(oi.getProduct().getProductName());
                ii.setProductCode(
                        oi.getProduct().getProductCode() != null
                                ? oi.getProduct().getProductCode()
                                : "");
            } else {
                ii.setProductName("Unknown Product");
                ii.setProductCode("");
            }
            ii.setQuantity(oi.getQuantity());
            ii.setUnitPrice(oi.getUnitPrice());
            double lineSubtotal = round(
                    oi.getUnitPrice() != null && oi.getQuantity() != null
                            ? oi.getUnitPrice() * oi.getQuantity()
                            : 0.0);
            ii.setSubtotal(lineSubtotal);
            subtotalAccum = subtotalAccum.add(BigDecimal.valueOf(lineSubtotal));
            invoiceItems.add(ii);
        }

        double subtotal = subtotalAccum.setScale(2, RoundingMode.HALF_UP).doubleValue();
        double taxAmount = round(subtotal * taxRate / 100.0);
        double totalAmount = round(subtotal + taxAmount);

        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setItems(invoiceItems);

        Invoice saved = invoiceRepository.save(invoice);
        return InvoiceResponse.from(saved);
    }

    // ── Retrieve ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAllByOrderByInvoiceDateDesc()
                .stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with ID: " + id));
        return InvoiceResponse.from(inv);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrderId(Long orderId) {
        Invoice inv = invoiceRepository.findByCustomerOrderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No invoice found for order ID: " + orderId));
        return InvoiceResponse.from(inv);
    }

    // ── Update Status ──────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse updateInvoiceStatus(Long id, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new BadRequestException("Status must not be blank.");
        }
        String upper = newStatus.trim().toUpperCase();
        if (!VALID_STATUSES.contains(upper)) {
            throw new BadRequestException(
                    "Invalid invoice status: '" + newStatus
                            + "'. Allowed values: " + VALID_STATUSES);
        }

        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with ID: " + id));

        // Cannot move out of PAID or CANCELLED once set
        String current = inv.getStatus();
        if ("PAID".equals(current)) {
            throw new BadRequestException("A PAID invoice cannot be updated.");
        }
        if ("CANCELLED".equals(current) && !"DRAFT".equals(upper)) {
            throw new BadRequestException(
                    "A CANCELLED invoice can only be re-opened to DRAFT.");
        }

        inv.setStatus(upper);
        return InvoiceResponse.from(invoiceRepository.save(inv));
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteInvoice(Long id) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with ID: " + id));

        if (!"DRAFT".equals(inv.getStatus())) {
            throw new BadRequestException(
                    "Only DRAFT invoices can be deleted. Current status: " + inv.getStatus());
        }

        invoiceRepository.delete(inv);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String generateInvoiceNumber() {
        String candidate;
        do {
            String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            candidate = "INV-" + INV_NUM_FORMATTER.format(LocalDateTime.now()) + "-" + suffix;
        } while (invoiceRepository.findByInvoiceNumber(candidate).isPresent());
        return candidate;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
