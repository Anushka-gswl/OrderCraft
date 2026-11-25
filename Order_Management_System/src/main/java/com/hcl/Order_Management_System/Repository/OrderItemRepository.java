package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderOrderId(Long orderId);

    void deleteByOrderOrderId(Long orderId);
}