package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}