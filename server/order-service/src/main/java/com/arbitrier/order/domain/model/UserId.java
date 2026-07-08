package com.arbitrier.order.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Identifies an authenticated user who submitted an order.
 *
 * <p>Layer: domain/model
 * <p>Module: order-service
 */
public record UserId(String value) {
    public UserId {
        Require.notBlank(value, "UserId.value");
    }

    /** Creates a {@code UserId} from the given string. */
    public static UserId of(String value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
