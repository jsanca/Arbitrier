package com.arbitrier.orchestrator.application.port.outbound;

/**
 * Command issued to the inventory-service to release a previously reserved stock allocation.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public record ReleaseStockSagaCommand(
        String sagaId,
        String stockReservationId,
        String orderId) {
}
