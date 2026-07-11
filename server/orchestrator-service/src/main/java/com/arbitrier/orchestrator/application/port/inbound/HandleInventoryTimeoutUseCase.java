package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: handle a timeout of the inventory reservation step.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface HandleInventoryTimeoutUseCase {

    /** Handles the timeout, evaluates the retry policy, and transitions the saga accordingly. */
    HandleInventoryTimeoutResult handle(HandleInventoryTimeoutCommand command);
}
