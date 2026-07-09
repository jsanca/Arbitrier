package com.arbitrier.inventory.application.port.inbound;

/**
 * Inbound port: attempt to reserve stock for a single order.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public interface ReserveStockUseCase {

    /**
     * Attempts to reserve stock as described by the command.
     *
     * @param command validated reservation request
     * @return result containing the reservation ID and outcome (RESERVED, PARTIALLY_RESERVED, or REJECTED)
     */
    ReserveStockResult reserve(ReserveStockCommand command);
}
