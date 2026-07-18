package com.arbitrier.inventory.application.port.inbound;

/**
 * Describes a single product that could not satisfy its requested quantity.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record UnavailableInventoryItem(
        String productId,
        int requestedQuantity,
        int availableQuantity,
        InventoryUnavailabilityReason reason
) {
}
