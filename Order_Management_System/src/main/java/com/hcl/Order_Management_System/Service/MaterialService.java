package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.Model.Material;
import com.hcl.Order_Management_System.Repository.MaterialRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public Material createMaterial(Material material) {
        return materialRepository.save(material);
    }

    public List<Material> getAllMaterials() {
        return materialRepository.findAll();
    }

    public Optional<Material> getMaterialById(Long id) {
        return materialRepository.findById(id);
    }

    public void deleteMaterial(Long id) {
        materialRepository.deleteById(id);
    }
}