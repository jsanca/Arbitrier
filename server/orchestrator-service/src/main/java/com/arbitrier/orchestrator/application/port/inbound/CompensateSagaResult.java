package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Result of beginning saga compensation.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record CompensateSagaResult(SagaId sagaId) {
}
