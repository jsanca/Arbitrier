package com.arbitrier.order.domain.model;

/**
 * Lifecycle states for a corporate bulk order.
 *
 * <p>Layer: domain/model
 * <p>Module: order-service
 */
public enum OrderStatus {
    PENDING,
    AWAITING_CUSTOMER_DECISION,
    CONFIRMED,
    PARTIALLY_CONFIRMED,
    CANCELLED;

    /** Returns true if the order has reached a final outcome. */
    public boolean isTerminal() {
        return this == CONFIRMED || this == PARTIALLY_CONFIRMED || this == CANCELLED;
    }
}
