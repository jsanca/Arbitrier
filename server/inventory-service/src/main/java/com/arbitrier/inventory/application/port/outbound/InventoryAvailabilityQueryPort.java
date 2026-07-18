package com.arbitrier.inventory.application.port.outbound;

import java.util.Collection;
import java.util.Map;

/**
 * Outbound port: retrieves current available quantity for a set of products in one operation.
 *
 * <p>Returns a map keyed by product ID. Absence of a product ID from the returned map
 * signifies that no inventory record exists for that product ({@code PRODUCT_NOT_FOUND}).
 *
 * <p>Implementations must avoid N+1 queries when the underlying store supports batch retrieval.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: inventory-service
 */
public interface InventoryAvailabilityQueryPort {

    /**
     * Retrieves availability snapshots for the given product IDs.
     *
     * @param productIds the set of product IDs to look up; must not be null or empty
     * @return map from product ID to its availability snapshot; absent entries mean the product
     *         was not found in inventory
     */
    Map<String, InventoryAvailabilitySnapshot> findAvailability(Collection<String> productIds);
}
