package com.arbitrier.order.application.port.outbound;

import java.util.List;

/**
 * Outbound port: synchronous query to the inventory system for global stock availability.
 *
 * <p>This port is used exclusively for pre-saga availability negotiation. It does not
 * reserve stock. Warehouse selection is internal to the Inventory bounded context (ADR-0009).
 * The production implementation will be a gRPC or HTTP adapter that delegates to
 * {@code inventory-service}. During development, a stub adapter is used.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: order-service
 */
public interface InventoryAvailabilityPort {

    /**
     * Returns per-SKU global availability for the given lines.
     *
     * @param lines the SKUs and requested quantities to check
     * @return one response per query line, in the same order
     */
    List<AvailabilityLineResponse> checkAvailability(List<AvailabilityLineQuery> lines);
}
