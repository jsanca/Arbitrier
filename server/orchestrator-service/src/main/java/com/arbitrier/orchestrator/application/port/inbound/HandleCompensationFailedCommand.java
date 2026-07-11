package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to handle a compensation failure — the compensating action itself could not complete.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleCompensationFailedCommand(String sagaId) {

    public HandleCompensationFailedCommand {
        Require.notBlank(sagaId, "HandleCompensationFailedCommand.sagaId");
    }
}
