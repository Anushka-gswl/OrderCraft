package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ResourceNotFoundException;
import com.hcl.Order_Management_System.Model.Material;
import com.hcl.Order_Management_System.Model.PurchaseOrder;
import com.hcl.Order_Management_System.Model.PurchaseOrderItem;
import com.hcl.Order_Management_System.Repository.MaterialRepository;
import com.hcl.Order_Management_System.Repository.PurchaseOrderItemRepository;
import com.hcl.Order_Management_System.Repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class PurchaseOrderItemService {

    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MaterialRepository materialRepository;

    public PurchaseOrderItemService(
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            MaterialRepository materialRepository) {
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.materialRepository = materialRepository;
    }

    @Transactional
    public PurchaseOrderItem createPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        if (purchaseOrderItem == null) {
            throw new BadRequestException("Purchase order item payload is required.");
        }
        if (purchaseOrderItem.getPurchaseOrder() == null
                || purchaseOrderItem.getPurchaseOrder().getPurchaseOrderId() == null) {
            throw new BadRequestException("A valid purchase order ID is required.");
        }
        if (purchaseOrderItem.getMaterial() == null
                || purchaseOrderItem.getMaterial().getMaterialId() == null) {
            throw new BadRequestException("A valid material ID is required.");
        }
        if (purchaseOrderItem.getQuantity() == null || purchaseOrderItem.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero.");
        }

        Long poId = purchaseOrderItem.getPurchaseOrder().getPurchaseOrderId();
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with ID: " + poId));

        if (!"CREATED".equalsIgnoreCase(po.getStatus())) {
            throw new BadRequestException("Items can only be added to purchase orders with status CREATED.");
        }

        Long materialId = purchaseOrderItem.getMaterial().getMaterialId();
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with ID: " + materialId));

        double unitCost = roundCurrency(material.getUnitCost() != null ? material.getUnitCost() : 0.0);
        double qty = roundCurrency(purchaseOrderItem.getQuantity());
        double subtotal = roundCurrency(unitCost * qty);

        PurchaseOrderItem newItem = new PurchaseOrderItem();
        newItem.setPurchaseOrder(po);
        newItem.setMaterial(material);
        newItem.setQuantity(qty);
        newItem.setUnitCost(unitCost);
        newItem.setSubtotal(subtotal);

        PurchaseOrderItem saved = purchaseOrderItemRepository.save(newItem);
        recalculatePoTotal(po);
        return saved;
    }

    public List<PurchaseOrderItem> getAllPurchaseOrderItems() {
        return purchaseOrderItemRepository.findAll();
    }

    public List<PurchaseOrderItem> getItemsByPurchaseOrderId(Long purchaseOrderId) {
        if (!purchaseOrderRepository.existsById(purchaseOrderId)) {
            throw new ResourceNotFoundException("Purchase Order not found with ID: " + purchaseOrderId);
        }
        return purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(purchaseOrderId);
    }

    public Optional<PurchaseOrderItem> getPurchaseOrderItemById(Long id) {
        return purchaseOrderItemRepository.findById(id);
    }

    @Transactional
    public void deletePurchaseOrderItem(Long id) {
        PurchaseOrderItem item = purchaseOrderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order item not found with ID: " + id));

        PurchaseOrder po = item.getPurchaseOrder();
        if (!"CREATED".equalsIgnoreCase(po.getStatus())) {
            throw new BadRequestException("Items can only be removed from purchase orders with status CREATED.");
        }

        purchaseOrderItemRepository.delete(item);
        purchaseOrderItemRepository.flush();
        recalculatePoTotal(po);
    }

    private void recalculatePoTotal(PurchaseOrder po) {
        List<PurchaseOrderItem> items =
                purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(po.getPurchaseOrderId());
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            if (item.getSubtotal() != null) {
                total = total.add(BigDecimal.valueOf(item.getSubtotal()));
            }
        }
        po.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP).doubleValue());
        purchaseOrderRepository.save(po);
    }

    private double roundCurrency(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
