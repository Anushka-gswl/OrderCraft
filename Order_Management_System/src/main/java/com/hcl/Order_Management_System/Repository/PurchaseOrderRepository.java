package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.PurchaseOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    boolean existsByPoNumber(String poNumber);

    boolean existsByCustomerOrderOrderIdAndStatus(Long orderId, String status);

    List<PurchaseOrder> findAllByOrderByOrderDateDesc();

    List<PurchaseOrder> findByCustomerOrderOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PurchaseOrder po WHERE po.purchaseOrderId = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Long id);
}
