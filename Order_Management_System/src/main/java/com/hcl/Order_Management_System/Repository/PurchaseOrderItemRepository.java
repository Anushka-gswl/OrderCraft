package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId);

    void deleteByPurchaseOrderPurchaseOrderId(Long purchaseOrderId);
}