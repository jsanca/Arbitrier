package com.arbitrier.order.adapter.outbound;

import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;
import com.arbitrier.order.application.port.outbound.AvailabilityLineResponse;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurable stub {@link InventoryAvailabilityPort} for use in unit and integration tests.
 *
 * <p>Per-SKU availability is set before each test. Returns 0 for any SKU that has not been
 * explicitly configured (simulates out-of-stock).
 *
 * <p>Not for production use.
 */
public class StubInventoryAvailabilityPort implements InventoryAvailabilityPort {

    private final Map<String, Integer> availability = new HashMap<>();

    /** Sets the available stock quantity for the given SKU. */
    public void setAvailable(final String sku, final int quantity) {
        availability.put(sku, quantity);
    }

    /** Makes all given SKUs return unlimited availability ({@code Integer.MAX_VALUE}). */
    public void setUnlimited(final String... skus) {
        for (String sku : skus) {
            availability.put(sku, Integer.MAX_VALUE);
        }
    }

    @Override
    public List<AvailabilityLineResponse> checkAvailability(final List<AvailabilityLineQuery> lines) {
        return lines.stream()
                .map(l -> new AvailabilityLineResponse(l.sku(), availability.getOrDefault(l.sku(), 0)))
                .toList();
    }
}
