package com.arbitrier.inventory.application.port.inbound;

import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.platform.validation.Require;

/**
 * Result of a {@link ReleaseStockUseCase} invocation.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record ReleaseStockResult(StockReservationId reservationId) {

    public ReleaseStockResult {
        Require.notNull(reservationId, "ReleaseStockResult.reservationId");
    }
}
