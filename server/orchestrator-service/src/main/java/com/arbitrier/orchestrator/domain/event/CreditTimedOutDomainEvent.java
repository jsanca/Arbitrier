package com.arbitrier.orchestrator.domain.event;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Emitted when the credit reservation step has timed out without a response.
 *
 * <p>Layer: domain/event
 * <p>Module: orchestrator-service
 */
public record CreditTimedOutDomainEvent(
        SagaId sagaId,
        String orderId,
        int attemptNumber) {
}
