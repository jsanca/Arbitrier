package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Result of starting a new saga instance.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record StartSagaResult(SagaId sagaId) {
}
