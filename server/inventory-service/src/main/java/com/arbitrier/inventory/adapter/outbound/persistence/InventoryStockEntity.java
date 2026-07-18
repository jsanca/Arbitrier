package com.arbitrier.inventory.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA entity for the {@code inventory_service.inventory_stock} table.
 *
 * <p>Represents on-hand stock quantity per SKU. Available quantity is derived at query time
 * by subtracting active reservation allocation quantities.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: inventory-service
 */
@Entity
@Table(name = "inventory_stock", schema = "inventory_service")
public class InventoryStockEntity {

    @Id
    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "on_hand_quantity", nullable = false)
    private int onHandQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected InventoryStockEntity() {
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getOnHandQuantity() { return onHandQuantity; }
    public void setOnHandQuantity(int onHandQuantity) { this.onHandQuantity = onHandQuantity; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
