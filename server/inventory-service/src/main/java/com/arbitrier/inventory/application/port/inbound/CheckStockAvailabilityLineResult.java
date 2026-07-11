package com.arbitrier.inventory.application.port.inbound;

/**
 * Per-line result of an availability check: how much of the requested quantity can be
 * fulfilled immediately and how much would go to backorder.
 *
 * <p>{@code availableQuantity} is bounded by {@code requestedQuantity} — it will never
 * exceed what was requested even if more stock exists in the warehouse.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record CheckStockAvailabilityLineResult(
        String sku,
        int requestedQuantity,
        int availableQuantity,
        boolean fullyAvailable,
        int backorderQuantity) {
}
