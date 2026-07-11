package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Result of handling a compensation failure.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleCompensationFailedResult(SagaId sagaId) {
}
