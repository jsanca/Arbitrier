package com.arbitrier.inventory.application.port.inbound;

import java.util.List;

/**
 * Result of the inventory availability check.
 *
 * <p>This is a point-in-time observation. {@link Available} means every requested item
 * currently has sufficient stock; it does not guarantee stock will remain when the Saga
 * later attempts the authoritative reservation.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public sealed interface InventoryAvailabilityResult {

    /** All requested items have sufficient available stock at the time of the query. */
    record Available() implements InventoryAvailabilityResult {
    }

    /** One or more requested items do not have sufficient available stock. */
    record Unavailable(List<UnavailableInventoryItem> items)
            implements InventoryAvailabilityResult {

        public Unavailable {
            items = List.copyOf(items);
        }
    }
}
