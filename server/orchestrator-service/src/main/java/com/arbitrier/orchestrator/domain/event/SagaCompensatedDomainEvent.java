package com.arbitrier.orchestrator.domain.event;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Emitted when a saga enters the {@code COMPENSATING} status.
 *
 * <p>Does not identify which compensating commands will be issued — that detail
 * belongs to ARB-016 (compensation wiring).
 *
 * <p>Layer: domain/event
 * <p>Module: orchestrator-service
 */
public record SagaCompensatedDomainEvent(
        SagaId sagaId,
        String orderId) {
}
