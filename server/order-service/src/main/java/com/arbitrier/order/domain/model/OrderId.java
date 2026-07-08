package com.arbitrier.order.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Unique identifier for a corporate bulk order.
 *
 * <p>Layer: domain/model
 * <p>Module: order-service
 */
public record OrderId(String value) {
    public OrderId {
        Require.notBlank(value, "OrderId.value");
    }

    /** Creates an {@code OrderId} from the given string. */
    public static OrderId of(String value) {
        return new OrderId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
