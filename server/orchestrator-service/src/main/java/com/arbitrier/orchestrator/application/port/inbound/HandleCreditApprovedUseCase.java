package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: handles the CreditApproved event from the credit-service.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface HandleCreditApprovedUseCase {

    /**
     * Records the credit approval, completes the saga, and issues a ConfirmOrder command
     * to the order-service.
     *
     * @param command the CreditApproved event payload
     * @return the completed saga identifier
     */
    HandleCreditApprovedResult handle(HandleCreditApprovedCommand command);
}
