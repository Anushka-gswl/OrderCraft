package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {
}