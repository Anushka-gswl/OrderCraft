package com.hcl.Order_Management_System.Controller;

import com.hcl.Order_Management_System.Model.PurchaseOrderItem;
import com.hcl.Order_Management_System.Service.PurchaseOrderItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/purchase-order-items")
public class PurchaseOrderItemController {

    private final PurchaseOrderItemService purchaseOrderItemService;

    public PurchaseOrderItemController(
            PurchaseOrderItemService purchaseOrderItemService) {

        this.purchaseOrderItemService = purchaseOrderItemService;
    }

    @PostMapping
    public PurchaseOrderItem createPurchaseOrderItem(
            @RequestBody PurchaseOrderItem purchaseOrderItem) {

        return purchaseOrderItemService
                .createPurchaseOrderItem(purchaseOrderItem);
    }

    @GetMapping
    public List<PurchaseOrderItem> getAllPurchaseOrderItems() {
        return purchaseOrderItemService.getAllPurchaseOrderItems();
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    public List<PurchaseOrderItem> getItemsByPurchaseOrderId(
            @PathVariable Long purchaseOrderId) {
        return purchaseOrderItemService.getItemsByPurchaseOrderId(purchaseOrderId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderItem> getPurchaseOrderItemById(
            @PathVariable Long id) {

        return purchaseOrderItemService
                .getPurchaseOrderItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchaseOrderItem(
            @PathVariable Long id) {

        purchaseOrderItemService.deletePurchaseOrderItem(id);
        return ResponseEntity.noContent().build();
    }
}
