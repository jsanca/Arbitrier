package com.arbitrier.orchestrator.application.port.inbound;

/**
 * Inbound port: handles the arrival of an OrderCreated event to start a new saga.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public interface HandleOrderCreatedUseCase {

    /**
     * Starts a saga and issues a ReserveStock command to the inventory-service.
     *
     * @param command the OrderCreated event payload
     * @return the saga identifier and the generated stock reservation identifier
     */
    HandleOrderCreatedResult handle(HandleOrderCreatedCommand command);
}
