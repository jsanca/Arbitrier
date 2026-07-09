package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: handles the StockReserved event from the inventory-service.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface HandleStockReservedUseCase {

    /**
     * Records the stock reservation on the saga and issues a ReserveCredit command
     * to the credit-service.
     *
     * @param command the StockReserved event payload
     * @return the saga identifier and the generated credit reservation identifier
     */
    HandleStockReservedResult handle(HandleStockReservedCommand command);
}
