package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to handle the CreditRejected event from the credit-service.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleCreditRejectedCommand(String sagaId) {

    public HandleCreditRejectedCommand {
        Require.notBlank(sagaId, "HandleCreditRejectedCommand.sagaId");
    }
}
