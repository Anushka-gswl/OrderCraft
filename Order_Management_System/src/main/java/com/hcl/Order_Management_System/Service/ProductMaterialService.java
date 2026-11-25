package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.ProductMaterialRequest;
import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ConflictException;
import com.hcl.Order_Management_System.Exception.ResourceNotFoundException;
import com.hcl.Order_Management_System.Model.Material;
import com.hcl.Order_Management_System.Model.Product;
import com.hcl.Order_Management_System.Model.ProductMaterial;
import com.hcl.Order_Management_System.Repository.MaterialRepository;
import com.hcl.Order_Management_System.Repository.ProductMaterialRepository;
import com.hcl.Order_Management_System.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class ProductMaterialService {

    private final ProductMaterialRepository productMaterialRepository;
    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;

    public ProductMaterialService(
            ProductMaterialRepository productMaterialRepository,
            ProductRepository productRepository,
            MaterialRepository materialRepository) {
        this.productMaterialRepository = productMaterialRepository;
        this.productRepository = productRepository;
        this.materialRepository = materialRepository;
    }

    @Transactional
    public ProductMaterial createProductMaterial(ProductMaterialRequest request) {
        if (request == null) {
            throw new BadRequestException("Product material payload is required.");
        }

        Long productId = request.getEffectiveProductId();
        Long materialId = request.getEffectiveMaterialId();
        Double quantityRequired = request.getQuantityRequired();

        if (productId == null || productId <= 0) {
            throw new BadRequestException("A valid product ID is required.");
        }
        if (materialId == null || materialId <= 0) {
            throw new BadRequestException("A valid material ID is required.");
        }
        if (quantityRequired == null || quantityRequired <= 0) {
            throw new BadRequestException("Quantity required must be greater than zero.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with ID: " + materialId));

        if (productMaterialRepository.existsByProductProductIdAndMaterialMaterialId(productId, materialId)) {
            throw new ConflictException(
                    "Material " + material.getMaterialName() + " is already mapped to product "
                            + product.getProductName() + ".");
        }

        ProductMaterial mapping = new ProductMaterial();
        mapping.setProduct(product);
        mapping.setMaterial(material);
        mapping.setQuantityRequired(roundQuantity(quantityRequired));
        return productMaterialRepository.save(mapping);
    }

    @Transactional
    public ProductMaterial updateProductMaterial(Long id, ProductMaterialRequest request) {
        if (request == null || request.getQuantityRequired() == null || request.getQuantityRequired() <= 0) {
            throw new BadRequestException("Quantity required must be greater than zero.");
        }

        ProductMaterial existing = productMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product material mapping not found with ID: " + id));

        existing.setQuantityRequired(roundQuantity(request.getQuantityRequired()));
        return productMaterialRepository.save(existing);
    }

    public List<ProductMaterial> getAllProductMaterials() {
        return productMaterialRepository.findAll();
    }

    public List<ProductMaterial> getProductMaterialsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with ID: " + productId);
        }
        return productMaterialRepository.findByProductProductId(productId);
    }

    public Optional<ProductMaterial> getProductMaterialById(Long id) {
        return productMaterialRepository.findById(id);
    }

    @Transactional
    public void deleteProductMaterial(Long id) {
        ProductMaterial existing = productMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product material mapping not found with ID: " + id));
        productMaterialRepository.delete(existing);
    }

    private double roundQuantity(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
