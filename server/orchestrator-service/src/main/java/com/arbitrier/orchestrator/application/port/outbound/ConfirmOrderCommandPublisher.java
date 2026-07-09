package com.arbitrier.orchestrator.application.port.outbound;

/**
 * Outbound port: issues a confirm-order command to the order-service.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public interface ConfirmOrderCommandPublisher {

    /** Publishes a {@link ConfirmOrderSagaCommand}. */
    void publishConfirmOrder(ConfirmOrderSagaCommand command);
}
