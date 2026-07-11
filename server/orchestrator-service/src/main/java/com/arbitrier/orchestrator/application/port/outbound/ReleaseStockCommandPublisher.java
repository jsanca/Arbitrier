package com.arbitrier.orchestrator.application.port.outbound;

/**
 * Outbound port: issues a release-stock command to the inventory-service during compensation.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public interface ReleaseStockCommandPublisher {

    /** Publishes a {@link ReleaseStockSagaCommand}. */
    void publishReleaseStock(ReleaseStockSagaCommand command);
}
