package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.OrderProcessingSummaryResponse;
import com.hcl.Order_Management_System.DTO.OrderProcessingSummaryResponse.MaterialRequirementItem;
import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ConflictException;
import com.hcl.Order_Management_System.Exception.ResourceNotFoundException;
import com.hcl.Order_Management_System.Model.CustomerOrder;
import com.hcl.Order_Management_System.Model.Inventory;
import com.hcl.Order_Management_System.Model.Material;
import com.hcl.Order_Management_System.Model.OrderItem;
import com.hcl.Order_Management_System.Model.Product;
import com.hcl.Order_Management_System.Model.ProductMaterial;
import com.hcl.Order_Management_System.Model.PurchaseOrder;
import com.hcl.Order_Management_System.Model.PurchaseOrderItem;
import com.hcl.Order_Management_System.Repository.CustomerOrderRepository;
import com.hcl.Order_Management_System.Repository.InventoryRepository;
import com.hcl.Order_Management_System.Repository.OrderItemRepository;
import com.hcl.Order_Management_System.Repository.ProductMaterialRepository;
import com.hcl.Order_Management_System.Repository.PurchaseOrderItemRepository;
import com.hcl.Order_Management_System.Repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class OrderProcessingService {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductMaterialRepository productMaterialRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    public OrderProcessingService(
            CustomerOrderRepository customerOrderRepository,
            OrderItemRepository orderItemRepository,
            ProductMaterialRepository productMaterialRepository,
            InventoryRepository inventoryRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository) {

        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productMaterialRepository = productMaterialRepository;
        this.inventoryRepository = inventoryRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<String> calculateMaterialRequirements(Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        RequirementCalculation calc = computeRequirementsForOrder(order);
        List<String> requirements = new ArrayList<>();
        for (MaterialRequirementItem item : calc.requirementItems) {
            requirements.add(item.getMaterialName() + " = " + item.getRequiredQuantity() + " " + item.getUnit());
        }
        return requirements;
    }

    @Transactional(readOnly = true)
    public List<String> checkInventory(Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        RequirementCalculation calc = computeRequirementsForOrder(order);
        List<String> results = new ArrayList<>();

        for (MaterialRequirementItem item : calc.requirementItems) {
            if ("AVAILABLE".equals(item.getStatus())) {
                results.add(
                        item.getMaterialName()
                                + " | Required: " + item.getRequiredQuantity() + " " + item.getUnit()
                                + " | Available: " + item.getAvailableQuantity() + " " + item.getUnit()
                                + " | AVAILABLE"
                );
            } else {
                results.add(
                        item.getMaterialName()
                                + " | Required: " + item.getRequiredQuantity() + " " + item.getUnit()
                                + " | Available: " + item.getAvailableQuantity() + " " + item.getUnit()
                                + " | Shortage: " + item.getShortageQuantity() + " " + item.getUnit()
                );
            }
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<String> getShortages(Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        RequirementCalculation calc = computeRequirementsForOrder(order);
        List<String> shortages = new ArrayList<>();

        for (MaterialRequirementItem item : calc.requirementItems) {
            if (item.getShortageQuantity() > 0) {
                shortages.add(
                        item.getMaterialName()
                                + " | Material ID: " + item.getMaterialId()
                                + " | Shortage: " + item.getShortageQuantity() + " " + item.getUnit()
                );
            }
        }
        return shortages;
    }

    @Transactional(readOnly = true)
    public OrderProcessingSummaryResponse getProcessingSummary(Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        RequirementCalculation calc = computeRequirementsForOrder(order);
        List<PurchaseOrder> linkedPos = purchaseOrderRepository.findByCustomerOrderOrderId(orderId);

        boolean activeCreatedPoExists = linkedPos.stream()
                .anyMatch(po -> "CREATED".equalsIgnoreCase(po.getStatus()));

        boolean hasShortages = calc.requirementItems.stream()
                .anyMatch(r -> r.getShortageQuantity() > 0);

        String status = order.getStatus() != null ? order.getStatus().toUpperCase() : "";
        boolean isNew = "NEW".equals(status);
        boolean isInProgress = "IN_PROGRESS".equals(status);
        boolean sufficientInventory = calc.hasOrderItems && !hasShortages;

        OrderProcessingSummaryResponse summary = new OrderProcessingSummaryResponse();
        summary.setOrderId(order.getOrderId());
        summary.setOrderNumber(order.getOrderNumber());
        summary.setStatus(order.getStatus());
        summary.setHasOrderItems(calc.hasOrderItems);
        summary.setSufficientInventory(sufficientInventory);
        summary.setActiveCreatedPoExists(activeCreatedPoExists);
        summary.setCanGeneratePurchaseOrder(isNew && hasShortages && !activeCreatedPoExists);
        summary.setCanStartProcessing(isNew && sufficientInventory);
        summary.setCanComplete(isInProgress);
        summary.setCanCancel(isNew);
        summary.setProductsWithoutBom(calc.productsWithoutBom);
        summary.setRequirements(calc.requirementItems);
        summary.setLinkedPurchaseOrders(linkedPos);

        return summary;
    }

    @Transactional(readOnly = true)
    public Map<Long, Double> computeRequiredByMaterial(Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        RequirementCalculation calc = computeRequirementsForOrder(order);
        Map<Long, Double> requiredByMaterial = new LinkedHashMap<>();
        for (MaterialRequirementItem item : calc.requirementItems) {
            requiredByMaterial.put(item.getMaterialId(), item.getRequiredQuantity());
        }
        return requiredByMaterial;
    }

    @Transactional
    public PurchaseOrder createPurchaseOrderForShortages(Long orderId) {
        CustomerOrder order = customerOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!"NEW".equalsIgnoreCase(order.getStatus())) {
            throw new ConflictException(
                    "Shortage purchase orders can only be generated for NEW orders. Current status: "
                            + order.getStatus());
        }

        if (purchaseOrderRepository.existsByCustomerOrderOrderIdAndStatus(orderId, "CREATED")) {
            throw new ConflictException(
                    "An active CREATED purchase order already exists for order "
                            + order.getOrderNumber() + ".");
        }

        RequirementCalculation calc = computeRequirementsForOrder(order);
        if (!calc.hasOrderItems) {
            throw new BadRequestException("Order has no items to calculate material shortages.");
        }

        List<PurchaseOrderItem> shortageItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (MaterialRequirementItem req : calc.requirementItems) {
            if (req.getShortageQuantity() > 0) {
                Material material = calc.materialsById.get(req.getMaterialId());
                double unitCost = roundVal(material.getUnitCost() != null ? material.getUnitCost() : 0.0);
                double subtotal = roundVal(req.getShortageQuantity() * unitCost);

                PurchaseOrderItem purchaseOrderItem = new PurchaseOrderItem();
                purchaseOrderItem.setMaterial(material);
                purchaseOrderItem.setQuantity(req.getShortageQuantity());
                purchaseOrderItem.setUnitCost(unitCost);
                purchaseOrderItem.setSubtotal(subtotal);
                shortageItems.add(purchaseOrderItem);

                totalAmount = totalAmount.add(BigDecimal.valueOf(subtotal));
            }
        }

        if (shortageItems.isEmpty()) {
            throw new BadRequestException("No material shortages found for order " + order.getOrderNumber() + ".");
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPoNumber("PO-AUTO-" + orderId + "-" + System.currentTimeMillis());
        purchaseOrder.setSupplierName("Default Supplier");
        purchaseOrder.setCustomerOrder(order);
        purchaseOrder.setOrderDate(LocalDateTime.now());
        purchaseOrder.setStatus("CREATED");
        purchaseOrder.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP).doubleValue());

        purchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        for (PurchaseOrderItem item : shortageItems) {
            item.setPurchaseOrder(purchaseOrder);
            purchaseOrderItemRepository.save(item);
        }

        return purchaseOrder;
    }

    private RequirementCalculation computeRequirementsForOrder(CustomerOrder order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderOrderId(order.getOrderId());
        Map<Long, Double> requiredByMaterial = new LinkedHashMap<>();
        Map<Long, Material> materialsById = new LinkedHashMap<>();
        Set<String> productsWithoutBom = new LinkedHashSet<>();

        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            Long productId = product.getProductId();
            Integer orderQuantity = orderItem.getQuantity();

            List<ProductMaterial> productMaterials =
                    productMaterialRepository.findByProductProductId(productId);

            if (productMaterials.isEmpty()) {
                productsWithoutBom.add(product.getProductCode() + " - " + product.getProductName());
                continue;
            }

            for (ProductMaterial productMaterial : productMaterials) {
                Material material = productMaterial.getMaterial();
                Long materialId = material.getMaterialId();
                double requiredQuantity = productMaterial.getQuantityRequired() * orderQuantity;
                requiredByMaterial.merge(materialId, requiredQuantity, Double::sum);
                materialsById.putIfAbsent(materialId, material);
            }
        }

        List<MaterialRequirementItem> items = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : requiredByMaterial.entrySet()) {
            Long materialId = entry.getKey();
            double requiredQuantity = roundVal(entry.getValue());
            Material material = materialsById.get(materialId);

            Optional<Inventory> inventory =
                    inventoryRepository.findByMaterialMaterialId(materialId);

            double availableQuantity = inventory.isPresent()
                    && inventory.get().getQuantityAvailable() != null
                    ? roundVal(inventory.get().getQuantityAvailable())
                    : 0.0;

            double shortage = availableQuantity < requiredQuantity
                    ? roundVal(requiredQuantity - availableQuantity)
                    : 0.0;

            MaterialRequirementItem reqItem = new MaterialRequirementItem();
            reqItem.setMaterialId(materialId);
            reqItem.setMaterialCode(material.getMaterialCode());
            reqItem.setMaterialName(material.getMaterialName());
            reqItem.setUnit(material.getUnit());
            reqItem.setRequiredQuantity(requiredQuantity);
            reqItem.setAvailableQuantity(availableQuantity);
            reqItem.setShortageQuantity(shortage);
            reqItem.setUnitCost(roundVal(material.getUnitCost() != null ? material.getUnitCost() : 0.0));
            reqItem.setStatus(shortage > 0 ? "SHORTAGE" : "AVAILABLE");
            items.add(reqItem);
        }

        return new RequirementCalculation(
                !orderItems.isEmpty(),
                new ArrayList<>(productsWithoutBom),
                items,
                materialsById
        );
    }

    private double roundVal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record RequirementCalculation(
            boolean hasOrderItems,
            List<String> productsWithoutBom,
            List<MaterialRequirementItem> requirementItems,
            Map<Long, Material> materialsById
    ) {}
}
