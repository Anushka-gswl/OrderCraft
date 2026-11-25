package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.Model.Inventory;
import com.hcl.Order_Management_System.Repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public Inventory createInventory(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    public Optional<Inventory> getInventoryById(Long id) {
        return inventoryRepository.findById(id);
    }

    public Optional<Inventory> getInventoryByMaterialId(Long materialId) {
        return inventoryRepository.findByMaterialMaterialId(materialId);
    }
    
    @Transactional
    public Optional<Inventory> updateInventory(Long id, Inventory inventory) {

        return inventoryRepository.findByIdForUpdate(id)
                .map(existingInventory -> {

                    if (inventory.getMaterial() != null) {
                        existingInventory.setMaterial(inventory.getMaterial());
                    }
                    existingInventory.setQuantityAvailable(inventory.getQuantityAvailable());

                    return inventoryRepository.save(existingInventory);
                });
    }

    public void deleteInventory(Long id) {
        inventoryRepository.deleteById(id);
    }
}