package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to handle the StockRejected event from the inventory-service.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleStockRejectedCommand(String sagaId) {

    public HandleStockRejectedCommand {
        Require.notBlank(sagaId, "HandleStockRejectedCommand.sagaId");
    }
}
