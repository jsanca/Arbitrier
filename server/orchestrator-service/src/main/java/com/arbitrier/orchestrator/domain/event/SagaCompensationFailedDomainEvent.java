package com.arbitrier.orchestrator.domain.event;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Emitted when a saga reaches the {@code FAILED_COMPENSATION} terminal state.
 *
 * <p>Indicates that the compensation flow itself encountered an unrecoverable error.
 * Manual intervention is required to resolve the saga.
 *
 * <p>Layer: domain/event
 * <p>Module: orchestrator-service
 */
public record SagaCompensationFailedDomainEvent(
        SagaId sagaId,
        String orderId) {
}
