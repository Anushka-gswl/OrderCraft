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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private Material steel;
    private Material copper;
    private CustomerOrder linkedOrder;

    @BeforeEach
    void setUp() {
        steel = new Material();
        steel.setMaterialId(1L);
        steel.setMaterialCode("MAT-001");
        steel.setMaterialName("Steel Sheet");
        steel.setUnit("KG");
        steel.setUnitCost(120.0);

        copper = new Material();
        copper.setMaterialId(2L);
        copper.setMaterialCode("MAT-002");
        copper.setMaterialName("Copper Wire");
        copper.setUnit("METER");
        copper.setUnitCost(45.5);

        linkedOrder = new CustomerOrder();
        linkedOrder.setOrderId(10L);
        linkedOrder.setOrderNumber("ORD-010");
        linkedOrder.setStatus("NEW");
    }

    @Test
    void createPurchaseOrder_computesUnitCostsSubtotalsAndTotalFromDb() {
        PurchaseOrderRequest req = new PurchaseOrderRequest();
        req.setSupplierName("Tata Metals");
        req.setCustomerOrderId(10L);
        req.setStatus("RECEIVED"); // ignored
        req.setTotalAmount(1.0); // ignored
        req.setItems(List.of(itemReq(1L, 10.0), itemReq(2L, 20.0)));

        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(linkedOrder));
        when(purchaseOrderRepository.existsByPoNumber(anyString())).thenReturn(false);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(steel));
        when(materialRepository.findById(2L)).thenReturn(Optional.of(copper));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            if (po.getPurchaseOrderId() == null) {
                po.setPurchaseOrderId(50L);
            }
            return po;
        });
        when(purchaseOrderItemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder created = purchaseOrderService.createPurchaseOrder(req);

        assertEquals("CREATED", created.getStatus());
        assertEquals("Tata Metals", created.getSupplierName());
        assertEquals("ORD-010", created.getCustomerOrder().getOrderNumber());
        assertEquals(2110.0, created.getTotalAmount(), 0.001);

        ArgumentCaptor<PurchaseOrderItem> itemCaptor = ArgumentCaptor.forClass(PurchaseOrderItem.class);
        verify(purchaseOrderItemRepository, times(2)).save(itemCaptor.capture());
        assertEquals(1200.0, itemCaptor.getAllValues().get(0).getSubtotal(), 0.001);
        assertEquals(910.0, itemCaptor.getAllValues().get(1).getSubtotal(), 0.001);
    }

    @Test
    void createPurchaseOrder_invalidMaterialOrQuantity_throwsException() {
        PurchaseOrderRequest badQty = new PurchaseOrderRequest();
        badQty.setSupplierName("Vendor");
        badQty.setItems(List.of(itemReq(1L, 0.0)));
        when(purchaseOrderRepository.existsByPoNumber(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> purchaseOrderService.createPurchaseOrder(badQty));

        PurchaseOrderRequest missingMat = new PurchaseOrderRequest();
        missingMat.setSupplierName("Vendor");
        missingMat.setItems(List.of(itemReq(999L, 5.0)));
        when(materialRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> purchaseOrderService.createPurchaseOrder(missingMat));
    }

    @Test
    void receivePurchaseOrder_updatesExistingAndCreatesMissingInventory() {
        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseOrderId(50L);
        po.setPoNumber("PO-050");
        po.setStatus("CREATED");

        PurchaseOrderItem item1 = new PurchaseOrderItem();
        item1.setMaterial(steel);
        item1.setQuantity(15.0);

        PurchaseOrderItem item2 = new PurchaseOrderItem();
        item2.setMaterial(copper);
        item2.setQuantity(30.0);

        Inventory existingSteelInv = new Inventory();
        existingSteelInv.setInventoryId(1L);
        existingSteelInv.setMaterial(steel);
        existingSteelInv.setQuantityAvailable(25.0);

        when(purchaseOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(po));
        when(purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(50L))
                .thenReturn(List.of(item1, item2));
        when(inventoryRepository.findByMaterialMaterialIdForUpdate(1L)).thenReturn(Optional.of(existingSteelInv));
        when(inventoryRepository.findByMaterialMaterialIdForUpdate(2L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder received = purchaseOrderService.receivePurchaseOrder(50L);

        assertEquals("RECEIVED", received.getStatus());
        assertEquals(40.0, existingSteelInv.getQuantityAvailable(), 0.001);
        verify(inventoryRepository, times(2)).save(any(Inventory.class));
    }

    @Test
    void receivePurchaseOrder_alreadyReceived_throwsConflictAndDoesNotUpdateInventory() {
        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseOrderId(50L);
        po.setPoNumber("PO-050");
        po.setStatus("RECEIVED");

        when(purchaseOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(po));

        assertThrows(ConflictException.class, () -> purchaseOrderService.receivePurchaseOrder(50L));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void createPurchaseOrder_duplicateMaterialIds_throwsBadRequest() {
        PurchaseOrderRequest req = new PurchaseOrderRequest();
        req.setSupplierName("Tata Metals");
        req.setItems(List.of(itemReq(1L, 5.0), itemReq(1L, 10.0)));

        when(purchaseOrderRepository.existsByPoNumber(anyString())).thenReturn(false);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(steel));

        assertThrows(BadRequestException.class, () -> purchaseOrderService.createPurchaseOrder(req));
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void getPurchaseOrderDetails_returnsPoWithItems() {
        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseOrderId(50L);
        po.setPoNumber("PO-050");
        po.setSupplierName("Tata Metals");
        po.setStatus("CREATED");
        po.setTotalAmount(1200.0);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrderItemId(1L);
        item.setMaterial(steel);
        item.setQuantity(10.0);
        item.setUnitCost(120.0);
        item.setSubtotal(1200.0);

        when(purchaseOrderRepository.findById(50L)).thenReturn(Optional.of(po));
        when(purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(50L)).thenReturn(List.of(item));

        PurchaseOrderDetailsResponse details = purchaseOrderService.getPurchaseOrderDetails(50L);
        assertEquals("PO-050", details.getPoNumber());
        assertEquals(1, details.getItems().size());
    }

    @Test
    void deletePurchaseOrder_whenReceived_throwsConflict() {
        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseOrderId(50L);
        po.setStatus("RECEIVED");

        when(purchaseOrderRepository.findById(50L)).thenReturn(Optional.of(po));

        assertThrows(ConflictException.class, () -> purchaseOrderService.deletePurchaseOrder(50L));
        verify(purchaseOrderRepository, never()).delete(any());
    }

    private PurchaseOrderItemRequest itemReq(Long materialId, Double qty) {
        PurchaseOrderItemRequest req = new PurchaseOrderItemRequest();
        req.setMaterialId(materialId);
        req.setQuantity(qty);
        return req;
    }
}
