package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.ProductMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductMaterialRepository extends JpaRepository<ProductMaterial, Long> {

    List<ProductMaterial> findByProductProductId(Long productId);

    boolean existsByProductProductIdAndMaterialMaterialId(Long productId, Long materialId);

    Optional<ProductMaterial> findByProductProductIdAndMaterialMaterialId(Long productId, Long materialId);
}