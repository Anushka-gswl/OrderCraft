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
import com.hcl.Order_Management_System.Model.Material;
import com.hcl.Order_Management_System.Model.OrderItem;
import com.hcl.Order_Management_System.Model.Product;
import com.hcl.Order_Management_System.Repository.CustomerOrderRepository;
import com.hcl.Order_Management_System.Repository.CustomerRepository;
import com.hcl.Order_Management_System.Repository.InventoryRepository;
import com.hcl.Order_Management_System.Repository.OrderItemRepository;
import com.hcl.Order_Management_System.Repository.ProductRepository;
import com.hcl.Order_Management_System.Repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerOrderServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OrderProcessingService orderProcessingService;

    @InjectMocks
    private CustomerOrderService customerOrderService;

    private Customer customer;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1L);
        customer.setCustomerName("ABC Manufacturing");
        customer.setEmail("abc@example.com");

        productA = new Product();
        productA.setProductId(10L);
        productA.setProductCode("PROD-001");
        productA.setProductName("Steel Frame");
        productA.setUnitPrice(1200.50);
        productA.setActive(true);

        productB = new Product();
        productB.setProductId(20L);
        productB.setProductCode("PROD-002");
        productB.setProductName("Control Unit");
        productB.setUnitPrice(500.00);
        productB.setActive(true);
    }

    @Test
    void createOrder_withMultipleProducts_computesPricesSubtotalsAndTotalFromDb() {
        CustomerOrderRequest request = new CustomerOrderRequest();
        request.setCustomerId(1L);
        request.setStatus("COMPLETED"); // must be ignored
        request.setTotalAmount(1.0); // must be ignored
        request.setItems(List.of(
                itemReq(10L, 2),
                itemReq(20L, 3)
        ));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerOrderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(productA));
        when(productRepository.findById(20L)).thenReturn(Optional.of(productB));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(inv -> {
            CustomerOrder o = inv.getArgument(0);
            if (o.getOrderId() == null) {
                o.setOrderId(100L);
            }
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerOrder created = customerOrderService.createOrder(request);

        assertEquals("NEW", created.getStatus());
        assertNotNull(created.getOrderNumber());
        assertNotNull(created.getOrderDate());
        assertEquals(3901.00, created.getTotalAmount(), 0.001);

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, times(2)).save(itemCaptor.capture());
        List<OrderItem> savedItems = itemCaptor.getAllValues();

        assertEquals(1200.50, savedItems.get(0).getUnitPrice(), 0.001);
        assertEquals(2401.00, savedItems.get(0).getSubtotal(), 0.001);
        assertEquals(500.00, savedItems.get(1).getUnitPrice(), 0.001);
        assertEquals(1500.00, savedItems.get(1).getSubtotal(), 0.001);
    }

    @Test
    void createOrder_invalidCustomer_throwsNotFound() {
        CustomerOrderRequest request = new CustomerOrderRequest();
        request.setCustomerId(999L);
        request.setItems(List.of(itemReq(10L, 1)));

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerOrderService.createOrder(request));
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void createOrder_invalidProduct_throwsNotFound() {
        CustomerOrderRequest request = new CustomerOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(itemReq(999L, 2)));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerOrderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerOrderService.createOrder(request));
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void createOrder_zeroOrNegativeQuantity_throwsBadRequest() {
        CustomerOrderRequest zeroReq = new CustomerOrderRequest();
        zeroReq.setCustomerId(1L);
        zeroReq.setItems(List.of(itemReq(10L, 0)));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerOrderRepository.existsByOrderNumber(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> customerOrderService.createOrder(zeroReq));

        CustomerOrderRequest negativeReq = new CustomerOrderRequest();
        negativeReq.setCustomerId(1L);
        negativeReq.setItems(List.of(itemReq(10L, -3)));

        assertThrows(BadRequestException.class, () -> customerOrderService.createOrder(negativeReq));
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void updateOrder_whenStatusIsNew_replacesItemsAndRecalculatesTotal() {
        CustomerOrder existing = new CustomerOrder();
        existing.setOrderId(100L);
        existing.setOrderNumber("ORD-100");
        existing.setCustomer(customer);
        existing.setOrderDate(LocalDateTime.now());
        existing.setStatus("NEW");
        existing.setTotalAmount(1200.50);

        CustomerOrderRequest updateReq = new CustomerOrderRequest();
        updateReq.setCustomerId(1L);
        updateReq.setItems(List.of(itemReq(20L, 4)));

        when(customerOrderRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(20L)).thenReturn(Optional.of(productB));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerOrder updated = customerOrderService.updateOrder(100L, updateReq);

        verify(orderItemRepository).deleteByOrderOrderId(100L);
        verify(orderItemRepository).flush();
        assertEquals(2000.00, updated.getTotalAmount(), 0.001);
    }

    @Test
    void updateOrder_whenNotNew_throwsBadRequest() {
        CustomerOrder existing = new CustomerOrder();
        existing.setOrderId(100L);
        existing.setStatus("IN_PROGRESS");

        CustomerOrderRequest updateReq = new CustomerOrderRequest();
        updateReq.setCustomerId(1L);
        updateReq.setItems(List.of(itemReq(10L, 1)));

        when(customerOrderRepository.findById(100L)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> customerOrderService.updateOrder(100L, updateReq));
        verify(orderItemRepository, never()).deleteByOrderOrderId(anyLong());
    }

    @Test
    void getOrderDetails_returnsOrderAndAssociatedItems() {
        CustomerOrder existing = new CustomerOrder();
        existing.setOrderId(100L);
        existing.setOrderNumber("ORD-100");
        existing.setCustomer(customer);
        existing.setStatus("NEW");
        existing.setTotalAmount(1000.0);

        OrderItem item = new OrderItem();
        item.setOrderItemId(1L);
        item.setOrder(existing);
        item.setProduct(productB);
        item.setQuantity(2);
        item.setUnitPrice(500.0);
        item.setSubtotal(1000.0);

        when(customerOrderRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(orderItemRepository.findByOrderOrderId(100L)).thenReturn(List.of(item));

        CustomerOrderDetailsResponse details = customerOrderService.getOrderDetails(100L);

        assertEquals("ORD-100", details.getOrderNumber());
        assertEquals(1, details.getItems().size());
        assertEquals(1000.0, details.getItems().get(0).getSubtotal(), 0.001);
    }

    @Test
    void deleteOrder_deletesItemsThenOrderForNewOrder() {
        CustomerOrder existing = new CustomerOrder();
        existing.setOrderId(100L);
        existing.setStatus("NEW");

        when(customerOrderRepository.findById(100L)).thenReturn(Optional.of(existing));

        customerOrderService.deleteOrder(100L);

        verify(orderItemRepository).deleteByOrderOrderId(100L);
        verify(orderItemRepository).flush();
        verify(customerOrderRepository).delete(existing);
    }

    @Test
    void deleteOrder_whenInProgress_throwsConflict() {
        CustomerOrder existing = new CustomerOrder();
        existing.setOrderId(100L);
        existing.setStatus("IN_PROGRESS");

        when(customerOrderRepository.findById(100L)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> customerOrderService.deleteOrder(100L));
        verify(customerOrderRepository, never()).delete(any());
    }

    @Test
    void createOrder_duplicateProductIds_throwsBadRequest() {
        CustomerOrderRequest req = new CustomerOrderRequest();
        req.setCustomerId(1L);
        req.setItems(List.of(itemReq(10L, 2), itemReq(10L, 3)));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerOrderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(productA));

        assertThrows(BadRequestException.class, () -> customerOrderService.createOrder(req));
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void createOrderWithIdempotency_sameKey_returnsExistingOrderWithoutSavingDuplicate() {
        CustomerOrder existing = new CustomerOrder();
        existing.setOrderId(200L);
        existing.setOrderNumber("ORD-200");
        existing.setIdempotencyKey("idem-key-123");
        existing.setStatus("NEW");
        existing.setTotalAmount(1200.50);

        CustomerOrderRequest req = new CustomerOrderRequest();
        req.setCustomerId(1L);
        req.setItems(List.of(itemReq(10L, 1)));

        when(customerOrderRepository.findByIdempotencyKey("idem-key-123"))
                .thenReturn(Optional.of(existing));

        CustomerOrderService.OrderCreationOutcome outcome =
                customerOrderService.createOrderWithIdempotency(req, "idem-key-123");

        assertFalse(outcome.created());
        assertEquals(200L, outcome.order().getOrderId());
        verify(customerOrderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void startOrderProcessing_deductsInventoryAndMovesOrderToInProgress() {
        CustomerOrder existing = new CustomerOrder();
        existing.setOrderId(100L);
        existing.setOrderNumber("ORD-100");
        existing.setStatus("NEW");

        OrderItem item = new OrderItem();
        item.setOrderItemId(1L);
        item.setProduct(productA);
        item.setQuantity(2);

        Material steel = new Material();
        steel.setMaterialId(5L);
        steel.setMaterialName("Steel");

        Inventory inv = new Inventory();
        inv.setInventoryId(1L);
        inv.setMaterial(steel);
        inv.setQuantityAvailable(50.0);

        when(customerOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(existing));
        when(orderItemRepository.findByOrderOrderId(100L)).thenReturn(List.of(item));
        when(orderProcessingService.computeRequiredByMaterial(100L)).thenReturn(Map.of(5L, 12.0));
        when(inventoryRepository.findByMaterialMaterialIdForUpdate(5L)).thenReturn(Optional.of(inv));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(i -> i.getArgument(0));

        CustomerOrder updated = customerOrderService.startOrderProcessing(100L);

        assertEquals("IN_PROGRESS", updated.getStatus());
        assertEquals(38.0, inv.getQuantityAvailable(), 0.001);
        verify(inventoryRepository).save(inv);
    }

    @Test
    void completeAndCancelOrder_enforceValidTransitions() {
        CustomerOrder inProg = new CustomerOrder();
        inProg.setOrderId(100L);
        inProg.setStatus("IN_PROGRESS");

        when(customerOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(inProg));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(BadRequestException.class, () -> customerOrderService.cancelOrder(100L));
        CustomerOrder completed = customerOrderService.completeOrder(100L);
        assertEquals("COMPLETED", completed.getStatus());
        assertThrows(BadRequestException.class, () -> customerOrderService.completeOrder(100L));
    }

    private OrderItemRequest itemReq(Long productId, Integer quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
