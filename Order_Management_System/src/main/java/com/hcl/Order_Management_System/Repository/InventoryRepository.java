package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByMaterialMaterialId(Long materialId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.material.materialId = :materialId")
    Optional<Inventory> findByMaterialMaterialIdForUpdate(@Param("materialId") Long materialId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.inventoryId = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") Long id);
}
