package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to handle a timeout of the inventory reservation step.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleInventoryTimeoutCommand(
        String sagaId,
        String stockReservationId,
        int attemptNumber) {

    public HandleInventoryTimeoutCommand {
        Require.notBlank(sagaId, "HandleInventoryTimeoutCommand.sagaId");
        Require.notBlank(stockReservationId, "HandleInventoryTimeoutCommand.stockReservationId");
        Require.isTrue(attemptNumber >= 1,
                "HandleInventoryTimeoutCommand.attemptNumber must be >= 1, got: " + attemptNumber);
    }
}
