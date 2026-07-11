package com.arbitrier.orchestrator.domain.event;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Emitted when the saga reaches the {@code CANCELLED} terminal state.
 *
 * <p>Published for both direct cancellation (stock rejected) and post-compensation
 * cancellation (inventory released after credit rejection).
 *
 * <p>Layer: domain/event
 * <p>Module: orchestrator-service
 */
public record SagaCancelledDomainEvent(
        SagaId sagaId,
        String orderId) {
}
