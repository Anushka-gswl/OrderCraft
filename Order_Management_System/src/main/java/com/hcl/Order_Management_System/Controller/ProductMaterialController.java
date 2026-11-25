package com.hcl.Order_Management_System.Controller;

import com.hcl.Order_Management_System.DTO.ProductMaterialRequest;
import com.hcl.Order_Management_System.Model.ProductMaterial;
import com.hcl.Order_Management_System.Service.ProductMaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/product-materials")
public class ProductMaterialController {

    private final ProductMaterialService productMaterialService;

    public ProductMaterialController(ProductMaterialService productMaterialService) {
        this.productMaterialService = productMaterialService;
    }

    @PostMapping
    public ResponseEntity<ProductMaterial> createProductMaterial(
            @RequestBody ProductMaterialRequest productMaterialRequest) {

        ProductMaterial created = productMaterialService.createProductMaterial(productMaterialRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<ProductMaterial> getAllProductMaterials() {
        return productMaterialService.getAllProductMaterials();
    }

    @GetMapping("/product/{productId}")
    public List<ProductMaterial> getProductMaterialsByProductId(@PathVariable Long productId) {
        return productMaterialService.getProductMaterialsByProductId(productId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductMaterial> getProductMaterialById(
            @PathVariable Long id) {

        return productMaterialService.getProductMaterialById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductMaterial> updateProductMaterial(
            @PathVariable Long id,
            @RequestBody ProductMaterialRequest productMaterialRequest) {

        return ResponseEntity.ok(productMaterialService.updateProductMaterial(id, productMaterialRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductMaterial(@PathVariable Long id) {
        productMaterialService.deleteProductMaterial(id);
        return ResponseEntity.noContent().build();
    }
}
