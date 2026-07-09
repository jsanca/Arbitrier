package com.arbitrier.orchestrator.application.port.outbound;

/**
 * Outbound port: issues a reserve-stock command to the inventory-service.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public interface ReserveStockCommandPublisher {

    /** Publishes a {@link ReserveStockSagaCommand}. */
    void publishReserveStock(ReserveStockSagaCommand command);
}
