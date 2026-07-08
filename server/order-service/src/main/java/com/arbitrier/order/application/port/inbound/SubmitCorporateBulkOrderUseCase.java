package com.arbitrier.order.application.port.inbound;

/**
 * Inbound port: submit a corporate bulk order and create a {@code PENDING} order.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public interface SubmitCorporateBulkOrderUseCase {

    /**
     * Executes the submit-order flow.
     *
     * @param command the validated submission command
     * @return the result containing the new order ID and status
     * @throws NullPointerException     if a required field is null
     * @throws IllegalArgumentException if a field fails business validation
     */
    SubmitCorporateBulkOrderResult execute(SubmitCorporateBulkOrderCommand command);
}
