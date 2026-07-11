package com.arbitrier.inventory.application.port.outbound;

import com.arbitrier.platform.validation.Require;

/**
 * A single line in a {@link WarehouseAllocationPort} query: one SKU and the quantity needed.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: inventory-service
 */
public record RequestedStockLine(String sku, int requestedQuantity) {

    public RequestedStockLine {
        Require.notBlank(sku, "RequestedStockLine.sku");
        Require.isTrue(requestedQuantity > 0,
                "RequestedStockLine.requestedQuantity must be positive, got: " + requestedQuantity);
    }
}
