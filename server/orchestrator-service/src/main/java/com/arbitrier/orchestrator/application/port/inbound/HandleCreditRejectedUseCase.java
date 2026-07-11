package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: handles a CreditRejected event from the credit-service.
 *
 * <p>Stock was already reserved, so compensation is required. The saga transitions to
 * {@code COMPENSATING} and a ReleaseStock command is issued to the inventory-service.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface HandleCreditRejectedUseCase {

    /**
     * Begins inventory compensation after credit rejection.
     *
     * @param command the CreditRejected event payload
     * @return the compensating saga identifier
     */
    HandleCreditRejectedResult handle(HandleCreditRejectedCommand command);
}
