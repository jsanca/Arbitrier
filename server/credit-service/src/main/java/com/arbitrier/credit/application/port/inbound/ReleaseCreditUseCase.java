package com.arbitrier.credit.application.port.inbound;

/**
 * Inbound port: release an approved credit reservation back to the credit line.
 *
 * <p>Idempotent — releasing an already-RELEASED reservation is a no-op.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: credit-service
 */
public interface ReleaseCreditUseCase {

    /**
     * Releases the credit reservation identified by the command.
     *
     * @param command the release request
     * @return the result containing the reservation ID
     * @throws IllegalArgumentException if the reservation does not exist
     */
    ReleaseCreditResult release(ReleaseCreditCommand command);
}
