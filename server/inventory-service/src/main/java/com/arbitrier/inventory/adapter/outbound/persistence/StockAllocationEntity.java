package com.arbitrier.inventory.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA entity for {@link com.arbitrier.inventory.domain.model.StockAllocation}.
 *
 * <p>Warehouse-level allocation detail. Internal to the Inventory bounded context.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: inventory-service
 */
@Entity
@Table(name = "stock_allocations", schema = "inventory_service")
public class StockAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id", nullable = false)
    private StockReservationLineEntity line;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected StockAllocationEntity() {
    }

    public Long getId() { return id; }

    public StockReservationLineEntity getLine() { return line; }
    public void setLine(StockReservationLineEntity line) { this.line = line; }

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
