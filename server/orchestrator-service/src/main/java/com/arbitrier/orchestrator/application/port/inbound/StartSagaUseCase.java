package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: start a new saga instance for a placed order.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface StartSagaUseCase {

    /**
     * Creates and persists a new saga in the {@code STARTED} state at step {@code ORDER_CREATED}.
     *
     * @param command the start request
     * @return the result containing the new saga ID
     */
    StartSagaResult start(StartSagaCommand command);
}
