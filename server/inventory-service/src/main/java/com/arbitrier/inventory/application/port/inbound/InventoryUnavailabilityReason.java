package com.arbitrier.inventory.application.port.inbound;

/**
 * Reason why a requested inventory item is unavailable.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public enum InventoryUnavailabilityReason {

    /** Available quantity is less than the requested quantity. */
    INSUFFICIENT_STOCK,

    /** No inventory record exists for the requested product. */
    PRODUCT_NOT_FOUND
}
