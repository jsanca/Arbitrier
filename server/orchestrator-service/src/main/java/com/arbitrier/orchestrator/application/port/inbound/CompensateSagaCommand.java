package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to begin compensation for a saga instance.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record CompensateSagaCommand(String sagaId) {

    public CompensateSagaCommand {
        Require.notBlank(sagaId, "CompensateSagaCommand.sagaId");
    }
}
