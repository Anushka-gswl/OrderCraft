package com.hcl.Order_Management_System.Model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "product_materials",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "material_id"})
    }
)
public class ProductMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productMaterialId;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    private Double quantityRequired;

    // Default constructor
    public ProductMaterial() {
    }

    // Getters and Setters

    public Long getProductMaterialId() {
        return productMaterialId;
    }

    public void setProductMaterialId(Long productMaterialId) {
        this.productMaterialId = productMaterialId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Double getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(Double quantityRequired) {
        this.quantityRequired = quantityRequired;
    }
}