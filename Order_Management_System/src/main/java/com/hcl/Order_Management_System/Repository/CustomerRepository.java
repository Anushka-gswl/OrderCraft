package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}