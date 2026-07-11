package com.arbitrier.inventory.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Internal value object: the quantity of a single SKU allocated from a specific warehouse.
 *
 * <p>Allocations are internal to the Inventory bounded context and must not be exposed
 * to Order, Saga, or any external caller. Public-facing contracts and integration events
 * express only business-level quantities (requested, reserved, backorder).
 *
 * <p>Layer: domain/model
 * <p>Module: inventory-service
 */
public record StockAllocation(WarehouseId warehouseId, String sku, int quantity) {

    public StockAllocation {
        Require.notNull(warehouseId, "StockAllocation.warehouseId");
        Require.notBlank(sku, "StockAllocation.sku");
        Require.isTrue(quantity > 0,
                "StockAllocation.quantity must be positive, got: " + quantity);
    }
}
