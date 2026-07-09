package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to handle the StockReserved event from the inventory-service.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleStockReservedCommand(String sagaId, String stockReservationId) {

    public HandleStockReservedCommand {
        Require.notBlank(sagaId, "HandleStockReservedCommand.sagaId");
        Require.notBlank(stockReservationId, "HandleStockReservedCommand.stockReservationId");
    }
}
