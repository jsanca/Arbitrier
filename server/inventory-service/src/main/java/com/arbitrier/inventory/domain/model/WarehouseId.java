package com.arbitrier.inventory.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Identifies the warehouse from which stock is reserved.
 *
 * <p>Layer: domain/model
 * <p>Module: inventory-service
 */
public record WarehouseId(String value) {
    public WarehouseId {
        Require.notBlank(value, "WarehouseId.value");
    }

    /** Creates a {@code WarehouseId} from the given string. */
    public static WarehouseId of(String value) {
        return new WarehouseId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
