package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.PurchaseOrderDetailsResponse;
import com.hcl.Order_Management_System.DTO.PurchaseOrderItemRequest;
import com.hcl.Order_Management_System.DTO.PurchaseOrderRequest;
import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ConflictException;
import com.hcl.Order_Management_System.Exception.ResourceNotFoundException;
import com.hcl.Order_Management_System.Model.CustomerOrder;
import com.hcl.Order_Management_System.Model.Inventory;
import com.hcl.Order_Management_System.Model.Material;
import com.hcl.Order_Management_System.Model.PurchaseOrder;
import com.hcl.Order_Management_System.Model.PurchaseOrderItem;
import com.hcl.Order_Management_System.Repository.CustomerOrderRepository;
import com.hcl.Order_Management_System.Repository.InventoryRepository;
import com.hcl.Order_Management_System.Repository.MaterialRepository;
import com.hcl.Order_Management_System.Repository.PurchaseOrderItemRepository;
import com.hcl.Order_Management_System.Repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private static final DateTimeFormatter PO_NUM_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final MaterialRepository materialRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            InventoryRepository inventoryRepository,
            MaterialRepository materialRepository,
            CustomerOrderRepository customerOrderRepository) {

        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.materialRepository = materialRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderRequest request) {
        if (request == null) {
            throw new BadRequestException("Purchase order request payload is required.");
        }

        if (request.getSupplierName() == null || request.getSupplierName().isBlank()) {
            throw new BadRequestException("Supplier name is required.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Purchase order must contain at least one material item.");
        }

        CustomerOrder customerOrder = resolveOptionalCustomerOrder(request.getEffectiveCustomerOrderId());
        String poNumber = resolvePoNumberForCreate(request.getPoNumber());
        List<PurchaseOrderItem> validatedItems = buildValidatedItems(request.getItems());

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(poNumber);
        po.setSupplierName(request.getSupplierName().trim());
        po.setCustomerOrder(customerOrder);
        po.setOrderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDateTime.now());
        po.setStatus("CREATED");
        po.setTotalAmount(0.0);

        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        double totalAmount = saveItemsAndComputeTotal(savedPo, validatedItems);
        savedPo.setTotalAmount(totalAmount);

        return purchaseOrderRepository.save(savedPo);
    }

    @Transactional
    public PurchaseOrder updatePurchaseOrder(Long id, PurchaseOrderRequest request) {
        if (request == null) {
            throw new BadRequestException("Purchase order request payload is required.");
        }

        PurchaseOrder existingPo = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase Order not found with ID: " + id));

        if (!"CREATED".equalsIgnoreCase(existingPo.getStatus())) {
            throw new BadRequestException(
                    "Only purchase orders with status CREATED can be edited. Current status: "
                            + existingPo.getStatus());
        }

        if (request.getSupplierName() == null || request.getSupplierName().isBlank()) {
            throw new BadRequestException("Supplier name is required.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Purchase order must contain at least one material item.");
        }

        if (request.getPoNumber() != null && !request.getPoNumber().isBlank()) {
            String trimmedPoNumber = request.getPoNumber().trim();
            if (!trimmedPoNumber.equals(existingPo.getPoNumber())
                    && purchaseOrderRepository.existsByPoNumber(trimmedPoNumber)) {
                throw new ConflictException("Purchase order number already exists: " + trimmedPoNumber);
            }
            existingPo.setPoNumber(trimmedPoNumber);
        }

        CustomerOrder customerOrder = resolveOptionalCustomerOrder(request.getEffectiveCustomerOrderId());
        List<PurchaseOrderItem> validatedItems = buildValidatedItems(request.getItems());

        existingPo.setSupplierName(request.getSupplierName().trim());
        existingPo.setCustomerOrder(customerOrder);
        if (request.getOrderDate() != null) {
            existingPo.setOrderDate(request.getOrderDate());
        }

        purchaseOrderItemRepository.deleteByPurchaseOrderPurchaseOrderId(id);
        purchaseOrderItemRepository.flush();

        double totalAmount = saveItemsAndComputeTotal(existingPo, validatedItems);
        existingPo.setTotalAmount(totalAmount);

        return purchaseOrderRepository.save(existingPo);
    }

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAllByOrderByOrderDateDesc();
    }

    public Optional<PurchaseOrder> getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDetailsResponse getPurchaseOrderDetails(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase Order not found with ID: " + id));
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(id);
        return PurchaseOrderDetailsResponse.from(po, items);
    }

    @Transactional
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase Order not found with ID: " + id));

        String status = po.getStatus() != null ? po.getStatus().toUpperCase() : "";
        if (!"CREATED".equals(status) && !"CANCELLED".equals(status)) {
            throw new ConflictException(
                    "Cannot delete purchase order with status " + po.getStatus()
                            + ". Received purchase orders cannot be deleted.");
        }

        purchaseOrderItemRepository.deleteByPurchaseOrderPurchaseOrderId(id);
        purchaseOrderItemRepository.flush();
        purchaseOrderRepository.delete(po);
    }

    // Receive material for a Purchase Order (pessimistic lock + @Version prevents duplicate/lost updates)
    @Transactional
    public PurchaseOrder receivePurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase Order not found with ID: " + purchaseOrderId));

        String status = purchaseOrder.getStatus() != null ? purchaseOrder.getStatus().toUpperCase() : "";
        if ("RECEIVED".equals(status)) {
            throw new ConflictException(
                    "Purchase Order " + purchaseOrder.getPoNumber() + " has already been received.");
        }
        if (!"CREATED".equals(status)) {
            throw new BadRequestException(
                    "Only purchase orders with status CREATED can be received. Current status: "
                            + purchaseOrder.getStatus());
        }

        List<PurchaseOrderItem> items = new ArrayList<>(
                purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(purchaseOrderId));

        if (items.isEmpty()) {
            throw new BadRequestException(
                    "Purchase Order " + purchaseOrder.getPoNumber() + " has no material items to receive.");
        }

        // Deterministic lock ordering by materialId prevents deadlocks across concurrent PO receives
        items.sort(Comparator.comparing(item -> item.getMaterial().getMaterialId()));

        for (PurchaseOrderItem item : items) {
            Long materialId = item.getMaterial().getMaterialId();
            double receivedQuantity = item.getQuantity() != null ? item.getQuantity() : 0.0;
            if (receivedQuantity <= 0) {
                continue;
            }

            Optional<Inventory> inventoryOptional =
                    inventoryRepository.findByMaterialMaterialIdForUpdate(materialId);

            if (inventoryOptional.isPresent()) {
                Inventory inventory = inventoryOptional.get();
                double currentQuantity =
                        inventory.getQuantityAvailable() != null ? inventory.getQuantityAvailable() : 0.0;
                inventory.setQuantityAvailable(roundCurrency(currentQuantity + receivedQuantity));
                inventoryRepository.save(inventory);
            } else {
                Inventory inventory = new Inventory();
                inventory.setMaterial(item.getMaterial());
                inventory.setQuantityAvailable(roundCurrency(receivedQuantity));
                inventoryRepository.save(inventory);
            }
        }

        purchaseOrder.setStatus("RECEIVED");
        return purchaseOrderRepository.save(purchaseOrder);
    }

    private CustomerOrder resolveOptionalCustomerOrder(Long customerOrderId) {
        if (customerOrderId == null) {
            return null;
        }
        if (customerOrderId <= 0) {
            throw new BadRequestException("Invalid customer order ID.");
        }
        return customerOrderRepository.findById(customerOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer order not found with ID: " + customerOrderId));
    }

    private String resolvePoNumberForCreate(String requestedPoNumber) {
        if (requestedPoNumber != null && !requestedPoNumber.isBlank()) {
            String trimmed = requestedPoNumber.trim();
            if (purchaseOrderRepository.existsByPoNumber(trimmed)) {
                throw new ConflictException("Purchase order number already exists: " + trimmed);
            }
            return trimmed;
        }

        String candidate;
        do {
            String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            candidate = "PO-" + PO_NUM_FORMATTER.format(LocalDateTime.now()) + "-" + suffix;
        } while (purchaseOrderRepository.existsByPoNumber(candidate));
        return candidate;
    }

    private List<PurchaseOrderItem> buildValidatedItems(List<PurchaseOrderItemRequest> itemRequests) {
        List<PurchaseOrderItem> items = new ArrayList<>();
        Set<Long> seenMaterialIds = new HashSet<>();

        for (PurchaseOrderItemRequest req : itemRequests) {
            if (req == null) {
                throw new BadRequestException("Purchase order item entry cannot be null.");
            }

            Long materialId = req.getEffectiveMaterialId();
            if (materialId == null || materialId <= 0) {
                throw new BadRequestException("A valid material ID is required for every purchase order item.");
            }

            if (!seenMaterialIds.add(materialId)) {
                throw new BadRequestException(
                        "Duplicate material ID in purchase order items: " + materialId);
            }

            Double quantity = req.getQuantity();
            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("Purchase order item quantity must be greater than zero.");
            }

            Material material = materialRepository.findById(materialId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Material not found with ID: " + materialId));

            if (material.getUnitCost() == null || material.getUnitCost() < 0) {
                throw new BadRequestException(
                        "Material does not have a valid unit cost: " + material.getMaterialName());
            }

            double unitCost = roundCurrency(material.getUnitCost());
            double normalizedQty = roundCurrency(quantity);
            double subtotal = roundCurrency(unitCost * normalizedQty);

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setMaterial(material);
            item.setQuantity(normalizedQty);
            item.setUnitCost(unitCost);
            item.setSubtotal(subtotal);
            items.add(item);
        }

        return items;
    }

    private double saveItemsAndComputeTotal(PurchaseOrder po, List<PurchaseOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;

        for (PurchaseOrderItem item : items) {
            item.setPurchaseOrder(po);
            purchaseOrderItemRepository.save(item);
            total = total.add(BigDecimal.valueOf(item.getSubtotal()));
        }

        return total.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double roundCurrency(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
