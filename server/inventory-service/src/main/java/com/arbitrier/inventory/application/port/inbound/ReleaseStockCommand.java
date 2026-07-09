package com.arbitrier.inventory.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Application command to release a stock reservation.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record ReleaseStockCommand(String reservationId) {

    public ReleaseStockCommand {
        Require.notBlank(reservationId, "ReleaseStockCommand.reservationId");
    }
}
