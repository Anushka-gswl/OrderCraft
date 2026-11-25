package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.OrderProcessingSummaryResponse;
import com.hcl.Order_Management_System.Exception.ConflictException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductMaterialRepository productMaterialRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @InjectMocks
    private OrderProcessingService orderProcessingService;

    private CustomerOrder order;
    private Product productA;
    private Material steel;

    @BeforeEach
    void setUp() {
        order = new CustomerOrder();
        order.setOrderId(10L);
        order.setOrderNumber("ORD-010");
        order.setStatus("NEW");

        productA = new Product();
        productA.setProductId(1L);
        productA.setProductName("Frame");

        steel = new Material();
        steel.setMaterialId(5L);
        steel.setMaterialName("Steel");
        steel.setUnitCost(50.0);
    }

    @Test
    void createPurchaseOrderForShortages_whenOrderNotNew_throwsConflict() {
        order.setStatus("IN_PROGRESS");
        when(customerOrderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        assertThrows(ConflictException.class,
                () -> orderProcessingService.createPurchaseOrderForShortages(10L));
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void createPurchaseOrderForShortages_whenActiveCreatedPoExists_throwsConflict() {
        when(customerOrderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.existsByCustomerOrderOrderIdAndStatus(10L, "CREATED"))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> orderProcessingService.createPurchaseOrderForShortages(10L));
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void createPurchaseOrderForShortages_whenNewAndNoActivePo_createsAggregatedPo() {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(productA);
        item.setQuantity(3);

        ProductMaterial pm = new ProductMaterial();
        pm.setProduct(productA);
        pm.setMaterial(steel);
        pm.setQuantityRequired(4.0);

        Inventory inv = new Inventory();
        inv.setMaterial(steel);
        inv.setQuantityAvailable(2.0);

        when(customerOrderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.existsByCustomerOrderOrderIdAndStatus(10L, "CREATED"))
                .thenReturn(false);
        when(orderItemRepository.findByOrderOrderId(10L)).thenReturn(List.of(item));
        when(productMaterialRepository.findByProductProductId(1L)).thenReturn(List.of(pm));
        when(inventoryRepository.findByMaterialMaterialId(5L)).thenReturn(Optional.of(inv));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(i -> {
            PurchaseOrder po = i.getArgument(0);
            po.setPurchaseOrderId(88L);
            return po;
        });
        when(purchaseOrderItemRepository.save(any(PurchaseOrderItem.class)))
                .thenAnswer(i -> i.getArgument(0));

        PurchaseOrder po = orderProcessingService.createPurchaseOrderForShortages(10L);

        assertEquals("CREATED", po.getStatus());
        assertEquals(500.0, po.getTotalAmount(), 0.001); // (3*4 - 2) * 50 = 500
    }

    @Test
    void getProcessingSummary_aggregatesSharedMaterialsAndSetsWorkflowFlags() {
        Product productB = new Product();
        productB.setProductId(2L);
        productB.setProductName("Panel");

        OrderItem item1 = new OrderItem();
        item1.setOrder(order);
        item1.setProduct(productA);
        item1.setQuantity(2);

        OrderItem item2 = new OrderItem();
        item2.setOrder(order);
        item2.setProduct(productB);
        item2.setQuantity(3);

        ProductMaterial pm1 = new ProductMaterial();
        pm1.setProduct(productA);
        pm1.setMaterial(steel);
        pm1.setQuantityRequired(2.0); // 4.0

        ProductMaterial pm2 = new ProductMaterial();
        pm2.setProduct(productB);
        pm2.setMaterial(steel);
        pm2.setQuantityRequired(3.0); // 9.0 => total 13.0

        Inventory inv = new Inventory();
        inv.setMaterial(steel);
        inv.setQuantityAvailable(5.0); // shortage 8.0

        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderOrderId(10L)).thenReturn(List.of(item1, item2));
        when(productMaterialRepository.findByProductProductId(1L)).thenReturn(List.of(pm1));
        when(productMaterialRepository.findByProductProductId(2L)).thenReturn(List.of(pm2));
        when(inventoryRepository.findByMaterialMaterialId(5L)).thenReturn(Optional.of(inv));
        when(purchaseOrderRepository.findByCustomerOrderOrderId(10L)).thenReturn(List.of());

        OrderProcessingSummaryResponse summary = orderProcessingService.getProcessingSummary(10L);

        assertEquals(1, summary.getRequirements().size());
        assertEquals(13.0, summary.getRequirements().get(0).getRequiredQuantity(), 0.001);
        assertEquals(8.0, summary.getRequirements().get(0).getShortageQuantity(), 0.001);
        assertFalse(summary.isSufficientInventory());
        assertTrue(summary.isCanGeneratePurchaseOrder());
        assertFalse(summary.isCanStartProcessing());
    }
}
