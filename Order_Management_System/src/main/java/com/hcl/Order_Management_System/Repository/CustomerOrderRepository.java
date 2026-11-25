package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.CustomerOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    boolean existsByOrderNumber(String orderNumber);

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey);

    List<CustomerOrder> findAllByOrderByOrderDateDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.orderId = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);
}
