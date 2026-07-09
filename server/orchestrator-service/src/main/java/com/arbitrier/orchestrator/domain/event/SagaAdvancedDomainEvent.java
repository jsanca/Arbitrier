package com.arbitrier.orchestrator.domain.event;

import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStep;

/**
 * Emitted when a saga advances to a new processing step.
 *
 * <p>Layer: domain/event
 * <p>Module: orchestrator-service
 */
public record SagaAdvancedDomainEvent(
        SagaId sagaId,
        String orderId,
        SagaStep currentStep) {
}
