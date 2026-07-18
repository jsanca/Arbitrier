package com.arbitrier.inventory.application.port.inbound;

/**
 * Inbound port: checks availability of requested inventory items without reserving any stock.
 *
 * <p>This is a read-only query. A positive result means the precondition was satisfied
 * at the time of the call; it does not guarantee stock will remain available when the
 * Saga later attempts the authoritative reservation.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public interface CheckInventoryAvailabilityUseCase {

    /**
     * Evaluates availability for all items in the query.
     *
     * @param query the batch of products and quantities to check
     * @return {@link InventoryAvailabilityResult.Available} when all items have sufficient stock;
     *         {@link InventoryAvailabilityResult.Unavailable} otherwise, containing every failing item
     * @throws IllegalArgumentException if the query fails semantic validation
     */
    InventoryAvailabilityResult check(InventoryAvailabilityQuery query);
}
