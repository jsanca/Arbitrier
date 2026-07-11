package com.arbitrier.inventory.application.port.inbound;

/**
 * Inbound port: checks stock availability for a set of SKUs without reserving any stock.
 *
 * <p>This is a read-only query — no state mutation, no persistence, no events.
 * The result is advisory: the caller must not assume the returned quantities will
 * still be available when an actual reservation is later attempted.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public interface CheckStockAvailabilityUseCase {

    /**
     * Checks per-SKU availability in the specified warehouse.
     *
     * @param command the warehouse and lines to check
     * @return per-line availability results
     */
    CheckStockAvailabilityResult check(CheckStockAvailabilityCommand command);
}
