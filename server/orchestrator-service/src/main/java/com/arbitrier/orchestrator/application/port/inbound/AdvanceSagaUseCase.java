package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: advance a saga to its next processing step.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface AdvanceSagaUseCase {

    /**
     * Loads the saga and advances it to the requested step without executing business logic.
     *
     * @param command the advance request
     * @return the result containing the saga ID and updated step
     * @throws IllegalArgumentException if the saga does not exist or the transition is invalid
     */
    AdvanceSagaResult advance(AdvanceSagaCommand command);
}
