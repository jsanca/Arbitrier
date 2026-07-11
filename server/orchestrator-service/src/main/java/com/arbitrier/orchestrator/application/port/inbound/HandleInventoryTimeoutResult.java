package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.orchestrator.domain.model.RetryDecision;
import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Result of handling an inventory reservation timeout.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleInventoryTimeoutResult(SagaId sagaId, RetryDecision decision) {
}
