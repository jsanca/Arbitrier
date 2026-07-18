package com.arbitrier.inventory.application.port.inbound;

import com.arbitrier.platform.validation.Require;

import java.util.List;

/**
 * Query object for the inventory availability use case.
 *
 * <p>Encapsulates all requested items as a single batch query, enabling the
 * use case to evaluate all products in one operation and report all failures
 * rather than stopping at the first.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record InventoryAvailabilityQuery(
        String requestId,
        List<RequestedInventoryItem> items
) {

    public InventoryAvailabilityQuery {
        Require.notBlank(requestId, "InventoryAvailabilityQuery.requestId");
        Require.notNull(items, "InventoryAvailabilityQuery.items");
        Require.notEmpty(items, "InventoryAvailabilityQuery.items");
        items = List.copyOf(items);
    }
}
