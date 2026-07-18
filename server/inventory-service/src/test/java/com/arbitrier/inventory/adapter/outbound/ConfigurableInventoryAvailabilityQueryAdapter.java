package com.arbitrier.inventory.adapter.outbound;

import com.arbitrier.inventory.application.port.outbound.InventoryAvailabilityQueryPort;
import com.arbitrier.inventory.application.port.outbound.InventoryAvailabilitySnapshot;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configurable in-memory {@link InventoryAvailabilityQueryPort} for use in tests.
 *
 * <p>Stock levels are set per product ID. Products not registered are treated as not found
 * (absent from the returned map).
 *
 * <p>Not for production use.
 */
public class ConfigurableInventoryAvailabilityQueryAdapter implements InventoryAvailabilityQueryPort {

    private final Map<String, Integer> stock = new LinkedHashMap<>();

    /** Registers available quantity for the given product ID. */
    public void setAvailable(final String productId, final int quantity) {
        stock.put(productId, quantity);
    }

    @Override
    public Map<String, InventoryAvailabilitySnapshot> findAvailability(
            final Collection<String> productIds) {

        final Map<String, InventoryAvailabilitySnapshot> result = new LinkedHashMap<>();
        for (final String productId : productIds) {
            if (stock.containsKey(productId)) {
                result.put(productId, new InventoryAvailabilitySnapshot(productId, stock.get(productId)));
            }
        }
        return result;
    }
}
