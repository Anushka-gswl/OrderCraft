package com.hcl.Order_Management_System.Controller;

import com.hcl.Order_Management_System.DTO.InvoiceRequest;
import com.hcl.Order_Management_System.DTO.InvoiceResponse;
import com.hcl.Order_Management_System.DTO.UpdateInvoiceStatusRequest;
import com.hcl.Order_Management_System.Service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * POST /api/invoices
     * Generate a new invoice from a COMPLETED customer order.
     * Body: { "orderId": 5, "taxRate": 18.0, "dueDate": "2025-02-28", "notes": "..." }
     */
    @PostMapping
    public ResponseEntity<InvoiceResponse> generateInvoice(
            @RequestParam("orderId") Long orderId,
            @RequestBody(required = false) InvoiceRequest request) {
        InvoiceResponse response = invoiceService.generateInvoice(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/invoices
     * List all invoices ordered by invoice date descending.
     */
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    /**
     * GET /api/invoices/{id}
     * Get a single invoice by its invoice ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    /**
     * GET /api/invoices/order/{orderId}
     * Get the invoice associated with a specific customer order.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<InvoiceResponse> getInvoiceByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(invoiceService.getInvoiceByOrderId(orderId));
    }

    /**
     * PUT /api/invoices/{id}/status
     * Update the status of an invoice. Body: { "status": "SENT" }
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<InvoiceResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateInvoiceStatusRequest body) {
        return ResponseEntity.ok(invoiceService.updateInvoiceStatus(id, body.getStatus()));
    }

    /**
     * DELETE /api/invoices/{id}
     * Delete a DRAFT invoice.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
