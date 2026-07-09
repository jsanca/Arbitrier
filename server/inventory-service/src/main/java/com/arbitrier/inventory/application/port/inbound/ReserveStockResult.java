package com.arbitrier.inventory.application.port.inbound;

import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationStatus;
import com.arbitrier.platform.validation.Require;

/**
 * Result of a {@link ReserveStockUseCase} invocation.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record ReserveStockResult(StockReservationId reservationId, StockReservationStatus outcome) {

    public ReserveStockResult {
        Require.notNull(reservationId, "ReserveStockResult.reservationId");
        Require.notNull(outcome, "ReserveStockResult.outcome");
    }
}
