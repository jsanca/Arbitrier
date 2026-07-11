package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Result of handling a CreditRejected event.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleCreditRejectedResult(SagaId sagaId) {
}
