package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to handle a timeout of the credit reservation step.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleCreditTimeoutCommand(
        String sagaId,
        String creditReservationId,
        int attemptNumber) {

    public HandleCreditTimeoutCommand {
        Require.notBlank(sagaId, "HandleCreditTimeoutCommand.sagaId");
        Require.notBlank(creditReservationId, "HandleCreditTimeoutCommand.creditReservationId");
        Require.isTrue(attemptNumber >= 1,
                "HandleCreditTimeoutCommand.attemptNumber must be >= 1, got: " + attemptNumber);
    }
}
