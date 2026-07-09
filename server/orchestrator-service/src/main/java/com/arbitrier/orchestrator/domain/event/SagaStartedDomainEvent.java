package com.arbitrier.orchestrator.domain.event;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Emitted when a new saga instance is created for an order.
 *
 * <p>Layer: domain/event
 * <p>Module: orchestrator-service
 */
public record SagaStartedDomainEvent(
        SagaId sagaId,
        String orderId,
        String customerId) {
}
