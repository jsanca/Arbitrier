package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.orchestrator.domain.model.SagaId;

/**
 * Result of handling a StockRejected event.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleStockRejectedResult(SagaId sagaId) {
}
