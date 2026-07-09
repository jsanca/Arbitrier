package com.arbitrier.inventory.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * A single line within a {@link ReserveStockCommand}: one SKU and the quantity to reserve.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record ReserveStockLineCommand(String sku, int quantity) {

    public ReserveStockLineCommand {
        Require.notBlank(sku, "ReserveStockLineCommand.sku");
        Require.isTrue(quantity > 0,
                "ReserveStockLineCommand.quantity must be positive, got: " + quantity);
    }
}
