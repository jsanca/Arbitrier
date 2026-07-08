package com.arbitrier.order.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Stock-keeping unit code identifying a product variant in an order line.
 *
 * <p>Layer: domain/model
 * <p>Module: order-service
 */
public record Sku(String value) {
    public Sku {
        Require.notBlank(value, "Sku.value");
    }

    /** Creates a {@code Sku} from the given string. */
    public static Sku of(String value) {
        return new Sku(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
