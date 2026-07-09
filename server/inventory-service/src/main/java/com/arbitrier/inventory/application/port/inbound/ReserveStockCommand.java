package com.arbitrier.inventory.application.port.inbound;

import com.arbitrier.platform.validation.Require;

import java.util.List;

/**
 * Application command to reserve stock for a given order.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record ReserveStockCommand(
        String orderId,
        String reservationId,
        String warehouseId,
        List<ReserveStockLineCommand> lines) {

    public ReserveStockCommand {
        Require.notBlank(orderId, "ReserveStockCommand.orderId");
        Require.notBlank(reservationId, "ReserveStockCommand.reservationId");
        Require.notBlank(warehouseId, "ReserveStockCommand.warehouseId");
        Require.notEmpty(lines, "ReserveStockCommand.lines");
        lines = List.copyOf(lines);
    }
}
