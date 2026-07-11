package com.arbitrier.order.application.port.outbound;

/**
 * Per-SKU availability returned by {@link InventoryAvailabilityPort}.
 *
 * <p>{@code availableQuantity} is the raw stock figure from the inventory system.
 * The service layer applies {@code min(available, requested)} before presenting the
 * result to the caller.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: order-service
 */
public record AvailabilityLineResponse(String sku, int availableQuantity) {
}
