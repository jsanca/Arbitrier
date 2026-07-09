package com.arbitrier.orchestrator.application.port.outbound;

/**
 * Command issued to the credit-service to reserve credit for an order.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public record ReserveCreditSagaCommand(
        String sagaId,
        String creditReservationId,
        String orderId,
        String customerId) {
}
