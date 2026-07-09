package com.arbitrier.inventory.application.port.outbound;

import com.arbitrier.inventory.domain.model.WarehouseId;

/**
 * Outbound port: queries current available stock quantity for a SKU in a warehouse.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: inventory-service
 */
public interface StockAvailabilityPort {

    /**
     * Returns the quantity currently available to reserve for the given SKU.
     *
     * @param warehouseId the warehouse to query
     * @param sku         the stock-keeping unit code
     * @return available quantity; 0 if out of stock
     */
    int availableQuantity(WarehouseId warehouseId, String sku);
}
