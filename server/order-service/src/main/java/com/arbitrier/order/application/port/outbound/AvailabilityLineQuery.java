package com.arbitrier.order.application.port.outbound;

import com.arbitrier.platform.validation.Require;

/**
 * A single SKU line in an {@link InventoryAvailabilityPort} availability query.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: order-service
 */
public record AvailabilityLineQuery(String sku, int requestedQuantity) {

    public AvailabilityLineQuery {
        Require.notBlank(sku, "AvailabilityLineQuery.sku");
        Require.isTrue(requestedQuantity > 0,
                "AvailabilityLineQuery.requestedQuantity must be positive, got: " + requestedQuantity);
    }
}
