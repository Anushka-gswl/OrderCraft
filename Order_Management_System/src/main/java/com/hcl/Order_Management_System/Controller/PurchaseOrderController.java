package com.hcl.Order_Management_System.Controller;

import com.hcl.Order_Management_System.DTO.PurchaseOrderDetailsResponse;
import com.hcl.Order_Management_System.DTO.PurchaseOrderRequest;
import com.hcl.Order_Management_System.Model.PurchaseOrder;
import com.hcl.Order_Management_System.Service.PurchaseOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(
            @RequestBody PurchaseOrderRequest purchaseOrderRequest) {

        PurchaseOrder created = purchaseOrderService.createPurchaseOrder(purchaseOrderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderService.getAllPurchaseOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrderById(
            @PathVariable Long id) {

        return purchaseOrderService.getPurchaseOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<PurchaseOrderDetailsResponse> getPurchaseOrderDetails(
            @PathVariable Long id) {

        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderDetails(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrder> updatePurchaseOrder(
            @PathVariable Long id,
            @RequestBody PurchaseOrderRequest purchaseOrderRequest) {

        return ResponseEntity.ok(
                purchaseOrderService.updatePurchaseOrder(id, purchaseOrderRequest));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrder> receivePurchaseOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                purchaseOrderService.receivePurchaseOrder(id)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchaseOrder(
            @PathVariable Long id) {

        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.noContent().build();
    }
}
