package com.arbitrier.inventory.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * A single product and requested quantity within an inventory availability query.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record RequestedInventoryItem(String productId, int quantity) {

    public RequestedInventoryItem {
        Require.notBlank(productId, "RequestedInventoryItem.productId");
        Require.isTrue(quantity > 0,
                "RequestedInventoryItem.quantity must be positive, got: " + quantity);
    }
}
