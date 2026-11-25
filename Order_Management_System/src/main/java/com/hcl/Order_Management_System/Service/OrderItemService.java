package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ResourceNotFoundException;
import com.hcl.Order_Management_System.Model.CustomerOrder;
import com.hcl.Order_Management_System.Model.OrderItem;
import com.hcl.Order_Management_System.Model.Product;
import com.hcl.Order_Management_System.Repository.CustomerOrderRepository;
import com.hcl.Order_Management_System.Repository.OrderItemRepository;
import com.hcl.Order_Management_System.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;

    public OrderItemService(
            OrderItemRepository orderItemRepository,
            CustomerOrderRepository customerOrderRepository,
            ProductRepository productRepository) {
        this.orderItemRepository = orderItemRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderItem createOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new BadRequestException("Order item payload is required.");
        }
        if (orderItem.getOrder() == null || orderItem.getOrder().getOrderId() == null) {
            throw new BadRequestException("A valid order ID is required.");
        }
        if (orderItem.getProduct() == null || orderItem.getProduct().getProductId() == null) {
            throw new BadRequestException("A valid product ID is required.");
        }
        if (orderItem.getQuantity() == null || orderItem.getQuantity() <= 0) {
            throw new BadRequestException("Order item quantity must be greater than zero.");
        }

        Long orderId = orderItem.getOrder().getOrderId();
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!"NEW".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Items can only be added to orders with status NEW.");
        }

        Long productId = orderItem.getProduct().getProductId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        if (Boolean.FALSE.equals(product.getActive())) {
            throw new BadRequestException("Cannot add inactive product: " + product.getProductName());
        }

        double unitPrice = roundCurrency(product.getUnitPrice() != null ? product.getUnitPrice() : 0.0);
        double subtotal = roundCurrency(unitPrice * orderItem.getQuantity());

        OrderItem newItem = new OrderItem();
        newItem.setOrder(order);
        newItem.setProduct(product);
        newItem.setQuantity(orderItem.getQuantity());
        newItem.setUnitPrice(unitPrice);
        newItem.setSubtotal(subtotal);

        OrderItem saved = orderItemRepository.save(newItem);
        recalculateOrderTotal(order);
        return saved;
    }

    public List<OrderItem> getAllOrderItems() {
        return orderItemRepository.findAll();
    }

    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        if (!customerOrderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId);
        }
        return orderItemRepository.findByOrderOrderId(orderId);
    }

    public Optional<OrderItem> getOrderItemById(Long id) {
        return orderItemRepository.findById(id);
    }

    @Transactional
    public void deleteOrderItem(Long id) {
        OrderItem existing = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with ID: " + id));

        CustomerOrder order = existing.getOrder();
        if (!"NEW".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Items can only be removed from orders with status NEW.");
        }

        orderItemRepository.delete(existing);
        orderItemRepository.flush();
        recalculateOrderTotal(order);
    }

    private void recalculateOrderTotal(CustomerOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            if (item.getSubtotal() != null) {
                total = total.add(BigDecimal.valueOf(item.getSubtotal()));
            }
        }
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP).doubleValue());
        customerOrderRepository.save(order);
    }

    private double roundCurrency(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
