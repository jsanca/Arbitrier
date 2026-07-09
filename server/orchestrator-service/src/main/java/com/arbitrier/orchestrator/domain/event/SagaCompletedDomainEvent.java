package com.arbitrier.orchestrator.domain.event;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Emitted when the saga reaches the {@code COMPLETED} terminal state.
 *
 * <p>Layer: domain/event
 * <p>Module: orchestrator-service
 */
public record SagaCompletedDomainEvent(
        SagaId sagaId,
        String orderId) {
}
