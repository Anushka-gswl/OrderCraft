package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoiceInvoiceId(Long invoiceId);
}
