package com.arbitrier.order.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Identifies the corporate customer (buyer organisation) placing an order.
 *
 * <p>Layer: domain/model
 * <p>Module: order-service
 */
public record CustomerId(String value) {
    public CustomerId {
        Require.notBlank(value, "CustomerId.value");
    }

    /** Creates a {@code CustomerId} from the given string. */
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
