package com.arbitrier.order.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * A positive integer quantity of a SKU within an order line.
 *
 * <p>Layer: domain/model
 * <p>Module: order-service
 */
public record Quantity(int value) {
    public Quantity {
        Require.isTrue(value > 0, "Quantity.value must be positive, got: " + value);
    }

    /** Creates a {@code Quantity} from the given positive integer. */
    public static Quantity of(int value) {
        return new Quantity(value);
    }
}
