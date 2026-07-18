package com.arbitrier.inventory.application.port.outbound;

/**
 * Point-in-time snapshot of available quantity for a single product.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: inventory-service
 */
public record InventoryAvailabilitySnapshot(String productId, int availableQuantity) {
}
