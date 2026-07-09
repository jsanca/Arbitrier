package com.arbitrier.orchestrator.application.port.outbound;

/**
 * Outbound port: issues a reserve-credit command to the credit-service.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public interface ReserveCreditCommandPublisher {

    /** Publishes a {@link ReserveCreditSagaCommand}. */
    void publishReserveCredit(ReserveCreditSagaCommand command);
}
