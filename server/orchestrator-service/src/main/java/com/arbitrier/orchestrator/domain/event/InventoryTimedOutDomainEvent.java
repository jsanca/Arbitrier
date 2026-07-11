package com.arbitrier.orchestrator.domain.event;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Emitted when the inventory reservation step has timed out without a response.
 *
 * <p>Layer: domain/event
 * <p>Module: orchestrator-service
 */
public record InventoryTimedOutDomainEvent(
        SagaId sagaId,
        String orderId,
        int attemptNumber) {
}
