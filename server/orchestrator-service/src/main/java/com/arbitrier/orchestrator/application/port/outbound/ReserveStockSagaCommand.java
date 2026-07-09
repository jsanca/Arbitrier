package com.arbitrier.orchestrator.application.port.outbound;

/**
 * Command issued to the inventory-service to reserve stock for an order.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public record ReserveStockSagaCommand(
        String sagaId,
        String stockReservationId,
        String orderId) {
}
