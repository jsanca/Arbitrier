package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: begin compensation for a saga instance.
 *
 * <p>Marks the saga as {@code COMPENSATING} only. Compensating commands are issued
 * by ARB-016 wiring, not this use case.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface CompensateSagaUseCase {

    /**
     * Transitions the saga to the {@code COMPENSATING} status.
     *
     * @param command the compensate request
     * @return the result containing the saga ID
     * @throws IllegalArgumentException if the saga does not exist or is already terminal/compensating
     */
    CompensateSagaResult compensate(CompensateSagaCommand command);
}
