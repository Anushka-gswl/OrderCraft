package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.ProductMaterialRequest;
import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ConflictException;
import com.hcl.Order_Management_System.Model.Material;
import com.hcl.Order_Management_System.Model.Product;
import com.hcl.Order_Management_System.Model.ProductMaterial;
import com.hcl.Order_Management_System.Repository.MaterialRepository;
import com.hcl.Order_Management_System.Repository.ProductMaterialRepository;
import com.hcl.Order_Management_System.Repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductMaterialServiceTest {

    @Mock
    private ProductMaterialRepository productMaterialRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private ProductMaterialService productMaterialService;

    private Product product;
    private Material material;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setProductId(1L);
        product.setProductName("Frame");

        material = new Material();
        material.setMaterialId(2L);
        material.setMaterialName("Steel");
    }

    @Test
    void createProductMaterial_validatesAndSaves() {
        ProductMaterialRequest req = new ProductMaterialRequest();
        req.setProductId(1L);
        req.setMaterialId(2L);
        req.setQuantityRequired(4.5);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(materialRepository.findById(2L)).thenReturn(Optional.of(material));
        when(productMaterialRepository.existsByProductProductIdAndMaterialMaterialId(1L, 2L))
                .thenReturn(false);
        when(productMaterialRepository.save(any(ProductMaterial.class))).thenAnswer(i -> {
            ProductMaterial pm = i.getArgument(0);
            pm.setProductMaterialId(99L);
            return pm;
        });

        ProductMaterial saved = productMaterialService.createProductMaterial(req);
        assertEquals(99L, saved.getProductMaterialId());
        assertEquals(4.5, saved.getQuantityRequired(), 0.001);
    }

    @Test
    void createProductMaterial_duplicateMapping_throwsConflict() {
        ProductMaterialRequest req = new ProductMaterialRequest();
        req.setProductId(1L);
        req.setMaterialId(2L);
        req.setQuantityRequired(2.0);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(materialRepository.findById(2L)).thenReturn(Optional.of(material));
        when(productMaterialRepository.existsByProductProductIdAndMaterialMaterialId(1L, 2L))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> productMaterialService.createProductMaterial(req));
        verify(productMaterialRepository, never()).save(any());
    }

    @Test
    void updateProductMaterial_invalidQty_throwsBadRequest() {
        ProductMaterialRequest req = new ProductMaterialRequest();
        req.setQuantityRequired(0.0);

        assertThrows(BadRequestException.class,
                () -> productMaterialService.updateProductMaterial(1L, req));
    }
}
