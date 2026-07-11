package com.arbitrier.orchestrator.application.port.outbound;

/**
 * Command issued to the credit-service to release a previously approved credit reservation.
 *
 * <p>OPEN QUESTION (ARB-017): This command is not issued in any ARB-016 compensation flow —
 * CreditRejected means credit was never approved. This port is defined for future flows
 * where credit approval is followed by a later failure requiring credit release.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public record ReleaseCreditSagaCommand(
        String sagaId,
        String creditReservationId,
        String orderId) {
}
