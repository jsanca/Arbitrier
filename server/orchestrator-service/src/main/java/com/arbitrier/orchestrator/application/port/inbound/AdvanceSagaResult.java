package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStep;

/**
 * Result of advancing a saga to a new step.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record AdvanceSagaResult(SagaId sagaId, SagaStep currentStep) {
}
