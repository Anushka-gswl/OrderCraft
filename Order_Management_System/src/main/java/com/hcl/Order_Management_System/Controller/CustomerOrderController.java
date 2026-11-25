package com.hcl.Order_Management_System.Controller;

import com.hcl.Order_Management_System.DTO.CustomerOrderDetailsResponse;
import com.hcl.Order_Management_System.DTO.CustomerOrderRequest;
import com.hcl.Order_Management_System.DTO.OrderProcessingSummaryResponse;
import com.hcl.Order_Management_System.Model.CustomerOrder;
import com.hcl.Order_Management_System.Service.CustomerOrderService;
import com.hcl.Order_Management_System.Service.OrderProcessingService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final OrderProcessingService orderProcessingService;

    public CustomerOrderController(
            CustomerOrderService customerOrderService,
            OrderProcessingService orderProcessingService) {

        this.customerOrderService = customerOrderService;
        this.orderProcessingService = orderProcessingService;
    }

    @PostMapping
    public ResponseEntity<CustomerOrder> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CustomerOrderRequest orderRequest) {
        String effectiveKey = customerOrderService.resolveIdempotencyKey(
                idempotencyKey,
                orderRequest != null ? orderRequest.getIdempotencyKey() : null);

        try {
            CustomerOrderService.OrderCreationOutcome outcome =
                    customerOrderService.createOrderWithIdempotency(orderRequest, idempotencyKey);
            HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status).body(outcome.order());
        } catch (DataIntegrityViolationException ex) {
            if (effectiveKey != null) {
                Optional<CustomerOrder> existing = customerOrderService.findByIdempotencyKey(effectiveKey);
                if (existing.isPresent()) {
                    return ResponseEntity.ok(existing.get());
                }
            }
            throw ex;
        }
    }

    @GetMapping
    public List<CustomerOrder> getAllOrders() {
        return customerOrderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerOrder> getOrderById(@PathVariable Long id) {
        return customerOrderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<CustomerOrderDetailsResponse> getOrderDetails(@PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.getOrderDetails(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerOrder> updateOrder(
            @PathVariable Long id,
            @RequestBody CustomerOrderRequest orderRequest) {
        return ResponseEntity.ok(customerOrderService.updateOrder(id, orderRequest));
    }

    @GetMapping("/{id}/material-requirements")
    public List<String> getMaterialRequirements(@PathVariable Long id) {
        return orderProcessingService.calculateMaterialRequirements(id);
    }

    @GetMapping("/{id}/inventory-check")
    public List<String> checkInventory(@PathVariable Long id) {
        return orderProcessingService.checkInventory(id);
    }

    @GetMapping("/{id}/shortages")
    public List<String> getShortages(@PathVariable Long id) {
        return orderProcessingService.getShortages(id);
    }

    @GetMapping("/{id}/processing-summary")
    public ResponseEntity<OrderProcessingSummaryResponse> getProcessingSummary(@PathVariable Long id) {
        return ResponseEntity.ok(orderProcessingService.getProcessingSummary(id));
    }

    @PostMapping("/{id}/create-purchase-order")
    public ResponseEntity<?> createPurchaseOrderForShortages(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                orderProcessingService.createPurchaseOrderForShortages(id)
        );
    }

    @PostMapping("/{id}/start-processing")
    public ResponseEntity<CustomerOrder> startOrderProcessing(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                customerOrderService.startOrderProcessing(id)
        );
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<CustomerOrder> completeOrder(@PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.completeOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CustomerOrder> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.cancelOrder(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        customerOrderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
