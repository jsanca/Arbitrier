package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: handles notification that a compensation action could not complete.
 *
 * <p>Transitions the saga to {@code FAILED_COMPENSATION} — a terminal state requiring
 * manual intervention.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface HandleCompensationFailedUseCase {

    /**
     * Moves the saga to {@code FAILED_COMPENSATION}.
     *
     * @param command the compensation failure payload
     * @return the failed saga identifier
     */
    HandleCompensationFailedResult handle(HandleCompensationFailedCommand command);
}
