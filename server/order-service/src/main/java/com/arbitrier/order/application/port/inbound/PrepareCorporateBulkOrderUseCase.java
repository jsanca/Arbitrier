package com.arbitrier.order.application.port.inbound;

/**
 * Inbound port: checks inventory availability before starting the reservation saga.
 *
 * <p>This is a read-only pre-saga negotiation step. No Order aggregate is created and no
 * saga is started. The caller presents the intended order lines; the use case returns
 * per-line availability and a recommended action.
 *
 * <p>The customer then makes an explicit {@link CustomerPreSagaDecision} before the saga
 * is submitted via {@link SubmitCorporateBulkOrderUseCase}.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public interface PrepareCorporateBulkOrderUseCase {

    /**
     * Checks availability and returns a recommended action for the given intended order.
     *
     * @param command the intended order lines with warehouse context
     * @return per-line availability and recommended action
     */
    PrepareCorporateBulkOrderResult prepare(PrepareCorporateBulkOrderCommand command);
}
