package com.arbitrier.inventory.application.port.inbound;

/**
 * Inbound port: release a previously created stock reservation.
 *
 * <p>This operation is idempotent: releasing an already-released reservation
 * returns the same result without modifying state or publishing events.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public interface ReleaseStockUseCase {

    /**
     * Releases the reservation identified by the command.
     *
     * @param command contains the reservation ID to release
     * @return result with the reservation ID; always RELEASED
     * @throws IllegalArgumentException if no reservation exists with the given ID
     */
    ReleaseStockResult release(ReleaseStockCommand command);
}
