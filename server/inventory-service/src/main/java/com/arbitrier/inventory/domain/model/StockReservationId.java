package com.arbitrier.inventory.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Unique identifier for a stock reservation within the inventory bounded context.
 *
 * <p>Layer: domain/model
 * <p>Module: inventory-service
 */
public record StockReservationId(String value) {
    public StockReservationId {
        Require.notBlank(value, "StockReservationId.value");
    }

    /** Creates a {@code StockReservationId} from the given string. */
    public static StockReservationId of(String value) {
        return new StockReservationId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
