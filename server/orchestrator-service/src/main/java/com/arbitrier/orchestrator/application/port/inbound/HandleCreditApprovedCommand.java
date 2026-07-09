package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to handle the CreditApproved event from the credit-service.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleCreditApprovedCommand(String sagaId, String creditReservationId) {

    public HandleCreditApprovedCommand {
        Require.notBlank(sagaId, "HandleCreditApprovedCommand.sagaId");
        Require.notBlank(creditReservationId, "HandleCreditApprovedCommand.creditReservationId");
    }
}
