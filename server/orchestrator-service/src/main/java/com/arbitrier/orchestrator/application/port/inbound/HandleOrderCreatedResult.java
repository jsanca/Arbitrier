package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Result of handling an OrderCreated event.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleOrderCreatedResult(SagaId sagaId, String stockReservationId) {
}
