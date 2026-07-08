package com.arbitrier.order.domain.model;

/**
 * Reason that a corporate bulk order was cancelled.
 *
 * <p>Layer: domain/model
 * <p>Module: order-service
 */
public enum CancellationReason {
    CUSTOMER_CANCELLED,
    CUSTOMER_DEFERRED,
    INSUFFICIENT_CREDIT,
    SYSTEM_TIMEOUT
}
