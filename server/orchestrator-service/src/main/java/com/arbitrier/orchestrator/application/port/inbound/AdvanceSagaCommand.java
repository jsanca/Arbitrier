package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.orchestrator.domain.model.SagaStep;
import com.arbitrier.platform.validation.Require;

/**
 * Command to advance a saga to a new processing step.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record AdvanceSagaCommand(String sagaId, SagaStep nextStep) {

    public AdvanceSagaCommand {
        Require.notBlank(sagaId, "AdvanceSagaCommand.sagaId");
        Require.notNull(nextStep, "AdvanceSagaCommand.nextStep");
    }
}
