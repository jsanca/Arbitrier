package com.arbitrier.order.application.port.inbound;

/**
 * Recommended next action returned by the pre-saga availability check.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public enum RecommendedAction {

    /** All requested lines can be fully fulfilled — proceed directly to saga execution. */
    PROCEED_FULL,

    /** At least one line has insufficient stock — ask the customer to accept partial quantities. */
    ASK_CUSTOMER_ACCEPT_PARTIAL,

    /** No requested line has any available stock — the order cannot be executed. */
    REJECT_NO_STOCK
}
