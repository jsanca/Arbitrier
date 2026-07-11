package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: handles a StockReleased event from the inventory-service.
 *
 * <p>Called during the compensation flow after a CreditRejected event caused a
 * ReleaseStock command to be issued. Cancels the saga once inventory compensation is complete.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface HandleStockReleasedUseCase {

    /**
     * Cancels the saga after inventory compensation completes.
     *
     * @param command the StockReleased event payload
     * @return the cancelled saga identifier
     */
    HandleStockReleasedResult handle(HandleStockReleasedCommand command);
}
