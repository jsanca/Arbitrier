package com.arbitrier.orchestrator.application.port.outbound;

/**
 * Command issued to the order-service to confirm an order after saga completion.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public record ConfirmOrderSagaCommand(
        String sagaId,
        String orderId) {
}
