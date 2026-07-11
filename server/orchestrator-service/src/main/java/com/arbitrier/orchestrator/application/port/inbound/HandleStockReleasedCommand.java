package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to handle the StockReleased event from the inventory-service after compensation.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleStockReleasedCommand(String sagaId) {

    public HandleStockReleasedCommand {
        Require.notBlank(sagaId, "HandleStockReleasedCommand.sagaId");
    }
}
