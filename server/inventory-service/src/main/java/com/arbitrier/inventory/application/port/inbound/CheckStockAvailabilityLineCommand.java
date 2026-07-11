package com.arbitrier.inventory.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * A single line within a {@link CheckStockAvailabilityCommand}: one SKU and the quantity to check.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record CheckStockAvailabilityLineCommand(String sku, int requestedQuantity) {

    public CheckStockAvailabilityLineCommand {
        Require.notBlank(sku, "CheckStockAvailabilityLineCommand.sku");
        Require.isTrue(requestedQuantity > 0,
                "CheckStockAvailabilityLineCommand.requestedQuantity must be positive, got: " + requestedQuantity);
    }
}
