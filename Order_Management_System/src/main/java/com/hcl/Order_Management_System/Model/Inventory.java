package com.hcl.Order_Management_System.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    @OneToOne
    @JoinColumn(name = "material_id", nullable = false, unique = true)
    private Material material;

    private Double quantityAvailable;

    @Version
    @Column(nullable = false, columnDefinition = "bigint not null default 0")
    private Long version = 0L;

    // Default constructor
    public Inventory() {
    }

    // Getters and Setters

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Double getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(Double quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}