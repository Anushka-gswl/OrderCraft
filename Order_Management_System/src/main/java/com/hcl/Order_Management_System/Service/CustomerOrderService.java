package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.CustomerOrderDetailsResponse;
import com.hcl.Order_Management_System.DTO.CustomerOrderRequest;
import com.hcl.Order_Management_System.DTO.OrderItemRequest;
import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ConflictException;
import com.hcl.Order_Management_System.Exception.ResourceNotFoundException;
import com.hcl.Order_Management_System.Model.Customer;
import com.hcl.Order_Management_System.Model.CustomerOrder;
import com.hcl.Order_Management_System.Model.Inventory;
import com.hcl.Order_Management_System.Model.OrderItem;
import com.hcl.Order_Management_System.Model.Product;
import com.hcl.Order_Management_System.Model.PurchaseOrder;
import com.hcl.Order_Management_System.Repository.CustomerOrderRepository;
import com.hcl.Order_Management_System.Repository.CustomerRepository;
import com.hcl.Order_Management_System.Repository.InventoryRepository;
import com.hcl.Order_Management_System.Repository.OrderItemRepository;
import com.hcl.Order_Management_System.Repository.ProductRepository;
import com.hcl.Order_Management_System.Repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomerOrderService {

    private static final DateTimeFormatter ORDER_NUM_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderProcessingService orderProcessingService;

    public CustomerOrderService(
            CustomerOrderRepository customerOrderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            InventoryRepository inventoryRepository,
            OrderProcessingService orderProcessingService) {

        this.customerOrderRepository = customerOrderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderProcessingService = orderProcessingService;
    }

    public record OrderCreationOutcome(CustomerOrder order, boolean created) {
    }

    @Transactional
    public CustomerOrder createOrder(CustomerOrderRequest request) {
        return createOrderWithIdempotency(request, null).order();
    }

    @Transactional
    public OrderCreationOutcome createOrderWithIdempotency(
            CustomerOrderRequest request,
            String idempotencyKeyHeader) {
        if (request == null) {
            throw new BadRequestException("Order request payload is required.");
        }

        String effectiveIdempotencyKey = resolveIdempotencyKey(idempotencyKeyHeader, request.getIdempotencyKey());
        if (effectiveIdempotencyKey != null) {
            Optional<CustomerOrder> existingByKey =
                    customerOrderRepository.findByIdempotencyKey(effectiveIdempotencyKey);
            if (existingByKey.isPresent()) {
                return new OrderCreationOutcome(existingByKey.get(), false);
            }
        }

        Long customerId = request.getEffectiveCustomerId();
        if (customerId == null || customerId <= 0) {
            throw new BadRequestException("A valid customer ID is required.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + customerId));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one product item.");
        }

        String orderNumber = resolveOrderNumberForCreate(request.getOrderNumber());

        List<OrderItem> validatedItems = buildValidatedOrderItems(request.getItems());

        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(orderNumber);
        order.setCustomer(customer);
        order.setOrderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDateTime.now());
        order.setStatus("NEW");
        order.setTotalAmount(0.0);
        order.setIdempotencyKey(effectiveIdempotencyKey);

        CustomerOrder savedOrder = customerOrderRepository.save(order);

        double totalAmount = saveOrderItemsAndComputeTotal(savedOrder, validatedItems);
        savedOrder.setTotalAmount(totalAmount);

        return new OrderCreationOutcome(customerOrderRepository.save(savedOrder), true);
    }

    public Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return customerOrderRepository.findByIdempotencyKey(idempotencyKey.trim());
    }

    public String resolveIdempotencyKey(String headerKey, String bodyKey) {
        String candidate = (headerKey != null && !headerKey.isBlank()) ? headerKey : bodyKey;
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String trimmed = candidate.trim();
        if (trimmed.length() > 128) {
            throw new BadRequestException("Idempotency-Key must not exceed 128 characters.");
        }
        return trimmed;
    }

    @Transactional
    public CustomerOrder updateOrder(Long id, CustomerOrderRequest request) {
        if (request == null) {
            throw new BadRequestException("Order request payload is required.");
        }

        CustomerOrder existingOrder = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + id));

        if (!"NEW".equalsIgnoreCase(existingOrder.getStatus())) {
            throw new BadRequestException(
                    "Only orders with status NEW can be edited. Current status: " + existingOrder.getStatus());
        }

        Long customerId = request.getEffectiveCustomerId();
        if (customerId == null || customerId <= 0) {
            throw new BadRequestException("A valid customer ID is required.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + customerId));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one product item.");
        }

        if (request.getOrderNumber() != null && !request.getOrderNumber().isBlank()) {
            String trimmedOrderNumber = request.getOrderNumber().trim();
            if (!trimmedOrderNumber.equals(existingOrder.getOrderNumber())
                    && customerOrderRepository.existsByOrderNumber(trimmedOrderNumber)) {
                throw new ConflictException("Order number already exists: " + trimmedOrderNumber);
            }
            existingOrder.setOrderNumber(trimmedOrderNumber);
        }

        List<OrderItem> validatedItems = buildValidatedOrderItems(request.getItems());

        existingOrder.setCustomer(customer);
        if (request.getOrderDate() != null) {
            existingOrder.setOrderDate(request.getOrderDate());
        }

        orderItemRepository.deleteByOrderOrderId(id);
        orderItemRepository.flush();

        double totalAmount = saveOrderItemsAndComputeTotal(existingOrder, validatedItems);
        existingOrder.setTotalAmount(totalAmount);

        return customerOrderRepository.save(existingOrder);
    }

    public List<CustomerOrder> getAllOrders() {
        return customerOrderRepository.findAllByOrderByOrderDateDesc();
    }

    public Optional<CustomerOrder> getOrderById(Long id) {
        return customerOrderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public CustomerOrderDetailsResponse getOrderDetails(Long id) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + id));

        List<OrderItem> items = orderItemRepository.findByOrderOrderId(id);
        return CustomerOrderDetailsResponse.from(order, items);
    }

    @Transactional
    public void deleteOrder(Long id) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + id));

        String status = order.getStatus() != null ? order.getStatus().toUpperCase() : "";
        if (!"NEW".equals(status) && !"CANCELLED".equals(status)) {
            throw new ConflictException(
                    "Cannot delete order with status " + order.getStatus()
                            + ". Only NEW or CANCELLED orders can be deleted.");
        }

        List<PurchaseOrder> linkedPurchaseOrders = purchaseOrderRepository.findByCustomerOrderOrderId(id);
        for (PurchaseOrder po : linkedPurchaseOrders) {
            po.setCustomerOrder(null);
            purchaseOrderRepository.save(po);
        }

        orderItemRepository.deleteByOrderOrderId(id);
        orderItemRepository.flush();
        customerOrderRepository.delete(order);
    }

    // Move order from NEW to IN_PROGRESS after locked inventory verification and deduction
    @Transactional
    public CustomerOrder startOrderProcessing(Long orderId) {
        CustomerOrder order = customerOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!"NEW".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException(
                    "Only orders with status NEW can start processing. Current status: " + order.getStatus());
        }

        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
        if (items.isEmpty()) {
            throw new BadRequestException("Order cannot be processed because it has no order items.");
        }

        Map<Long, Double> requiredByMaterial = orderProcessingService.computeRequiredByMaterial(orderId);

        List<Long> sortedMaterialIds = requiredByMaterial.keySet().stream()
                .sorted()
                .toList();

        for (Long materialId : sortedMaterialIds) {
            double requiredQty = requiredByMaterial.get(materialId);
            if (requiredQty <= 0) {
                continue;
            }

            Inventory inventory = inventoryRepository.findByMaterialMaterialIdForUpdate(materialId)
                    .orElseThrow(() -> new BadRequestException(
                            "Order cannot be processed because inventory is insufficient for material ID: " + materialId));

            double availableQty = inventory.getQuantityAvailable() != null
                    ? inventory.getQuantityAvailable()
                    : 0.0;

            if (availableQty < requiredQty) {
                throw new BadRequestException(
                        "Order cannot be processed because inventory is insufficient for "
                                + inventory.getMaterial().getMaterialName());
            }

            inventory.setQuantityAvailable(roundCurrency(availableQty - requiredQty));
            inventoryRepository.save(inventory);
        }

        order.setStatus("IN_PROGRESS");
        return customerOrderRepository.save(order);
    }

    @Transactional
    public CustomerOrder completeOrder(Long orderId) {
        CustomerOrder order = customerOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!"IN_PROGRESS".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException(
                    "Only orders with status IN_PROGRESS can be completed. Current status: " + order.getStatus());
        }

        order.setStatus("COMPLETED");
        return customerOrderRepository.save(order);
    }

    @Transactional
    public CustomerOrder cancelOrder(Long orderId) {
        CustomerOrder order = customerOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!"NEW".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException(
                    "Only orders with status NEW can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus("CANCELLED");
        return customerOrderRepository.save(order);
    }

    private String resolveOrderNumberForCreate(String requestedOrderNumber) {
        if (requestedOrderNumber != null && !requestedOrderNumber.isBlank()) {
            String trimmed = requestedOrderNumber.trim();
            if (customerOrderRepository.existsByOrderNumber(trimmed)) {
                throw new ConflictException("Order number already exists: " + trimmed);
            }
            return trimmed;
        }

        String candidate;
        do {
            String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            candidate = "ORD-" + ORDER_NUM_FORMATTER.format(LocalDateTime.now()) + "-" + suffix;
        } while (customerOrderRepository.existsByOrderNumber(candidate));
        return candidate;
    }

    private List<OrderItem> buildValidatedOrderItems(List<OrderItemRequest> itemRequests) {
        List<OrderItem> items = new ArrayList<>();
        Set<Long> seenProductIds = new HashSet<>();

        for (OrderItemRequest itemRequest : itemRequests) {
            if (itemRequest == null) {
                throw new BadRequestException("Order item entry cannot be null.");
            }

            Long productId = itemRequest.getEffectiveProductId();
            if (productId == null || productId <= 0) {
                throw new BadRequestException("A valid product ID is required for every order item.");
            }

            if (!seenProductIds.add(productId)) {
                throw new BadRequestException(
                        "Duplicate product ID in order items: " + productId);
            }

            Integer quantity = itemRequest.getQuantity();
            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("Order item quantity must be greater than zero.");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with ID: " + productId));

            if (Boolean.FALSE.equals(product.getActive())) {
                throw new BadRequestException(
                        "Cannot add inactive product to order: " + product.getProductName());
            }

            if (product.getUnitPrice() == null || product.getUnitPrice() < 0) {
                throw new BadRequestException(
                        "Product does not have a valid unit price: " + product.getProductName());
            }

            double unitPrice = roundCurrency(product.getUnitPrice());
            double subtotal = roundCurrency(unitPrice * quantity);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            items.add(item);
        }

        return items;
    }

    private double saveOrderItemsAndComputeTotal(CustomerOrder order, List<OrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : items) {
            item.setOrder(order);
            orderItemRepository.save(item);
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
