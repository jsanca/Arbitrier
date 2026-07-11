package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: handle a timeout of the credit reservation step.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface HandleCreditTimeoutUseCase {

    /** Handles the timeout, evaluates the retry policy, and transitions the saga accordingly. */
    HandleCreditTimeoutResult handle(HandleCreditTimeoutCommand command);
}
