package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: handles a StockRejected event from the inventory-service.
 *
 * <p>No inventory was reserved so no compensation command is issued. The saga is
 * cancelled directly.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface HandleStockRejectedUseCase {

    /**
     * Cancels the saga because stock reservation failed.
     *
     * @param command the StockRejected event payload
     * @return the cancelled saga identifier
     */
    HandleStockRejectedResult handle(HandleStockRejectedCommand command);
}
