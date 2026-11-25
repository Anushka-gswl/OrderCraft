package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByCustomerOrderOrderId(Long orderId);

    Optional<Invoice> findByCustomerOrderOrderId(Long orderId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findAllByOrderByInvoiceDateDesc();
}
