package com.arbitrier.inventory.application.port.inbound;

import com.arbitrier.platform.validation.Require;

import java.util.List;

/**
 * Application command to check available global stock quantities without making a reservation.
 *
 * <p>This is a read-only query: it does not mutate any domain state, persist anything,
 * or publish events.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record CheckStockAvailabilityCommand(List<CheckStockAvailabilityLineCommand> lines) {

    public CheckStockAvailabilityCommand {
        Require.notEmpty(lines, "CheckStockAvailabilityCommand.lines");
        lines = List.copyOf(lines);
    }
}
