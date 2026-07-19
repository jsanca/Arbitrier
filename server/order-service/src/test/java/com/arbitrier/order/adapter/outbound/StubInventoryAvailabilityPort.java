package com.arbitrier.order.adapter.outbound;

import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;
import com.arbitrier.order.application.port.outbound.AvailabilityLineResponse;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configurable stub {@link InventoryAvailabilityPort} for use in unit and integration tests.
 *
 * <p>Per-SKU availability is set before each test. Returns 0 for any SKU that has not been
 * explicitly configured (simulates out-of-stock).
 *
 * <p>Supports protocol failure simulation via {@link StubBehavior}:
 * <ul>
 *   <li>{@code EMPTY_RESPONSE} — returns an empty list.</li>
 *   <li>{@code MISSING_SKU} — omits one requested SKU from the response.</li>
 *   <li>{@code DUPLICATE_SKU} — includes one SKU twice in the response.</li>
 *   <li>{@code UNEXPECTED_SKU} — includes a SKU that was not requested.</li>
 * </ul>
 *
 * <p>Not for production use.
 */
public class StubInventoryAvailabilityPort implements InventoryAvailabilityPort {

    private final Map<String, Integer> availability = new HashMap<>();
    private StubBehavior behavior = StubBehavior.NORMAL;

    public enum StubBehavior {
        NORMAL,
        EMPTY_RESPONSE,
        MISSING_SKU,
        DUPLICATE_SKU,
        UNEXPECTED_SKU
    }

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

    /**
     * Configures protocol failure behavior for the next (and only the next) call.
     * Resets to {@link StubBehavior#NORMAL} after one call.
     */
    public void setNextBehavior(final StubBehavior behavior) {
        this.behavior = behavior;
    }

    /** Clears all configured availability, resetting the stub to its initial zero-return state. */
    public void reset() {
        availability.clear();
        behavior = StubBehavior.NORMAL;
    }

    @Override
    public List<AvailabilityLineResponse> checkAvailability(final List<AvailabilityLineQuery> lines) {
        try {
            return switch (behavior) {
                case NORMAL -> normalResponse(lines);
                case EMPTY_RESPONSE -> List.of();
                case MISSING_SKU -> missingSkuResponse(lines);
                case DUPLICATE_SKU -> duplicateSkuResponse(lines);
                case UNEXPECTED_SKU -> unexpectedSkuResponse(lines);
            };
        } finally {
            behavior = StubBehavior.NORMAL;
        }
    }

    private List<AvailabilityLineResponse> normalResponse(final List<AvailabilityLineQuery> lines) {
        return lines.stream()
                .map(l -> new AvailabilityLineResponse(l.sku(), availability.getOrDefault(l.sku(), 0)))
                .toList();
    }

    private List<AvailabilityLineResponse> missingSkuResponse(final List<AvailabilityLineQuery> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        final List<AvailabilityLineResponse> result = new ArrayList<>();
        for (int i = 0; i < lines.size() - 1; i++) {
            result.add(new AvailabilityLineResponse(lines.get(i).sku(), availability.getOrDefault(lines.get(i).sku(), 0)));
        }
        return result;
    }

    private List<AvailabilityLineResponse> duplicateSkuResponse(final List<AvailabilityLineQuery> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        final List<AvailabilityLineResponse> result = new ArrayList<>();
        for (final AvailabilityLineQuery line : lines) {
            result.add(new AvailabilityLineResponse(line.sku(), availability.getOrDefault(line.sku(), 0)));
        }
        result.add(result.get(0));
        return result;
    }

    private List<AvailabilityLineResponse> unexpectedSkuResponse(final List<AvailabilityLineQuery> lines) {
        final List<AvailabilityLineResponse> result = new ArrayList<>();
        for (final AvailabilityLineQuery line : lines) {
            result.add(new AvailabilityLineResponse(line.sku(), availability.getOrDefault(line.sku(), 0)));
        }
        result.add(new AvailabilityLineResponse("UNEXPECTED-SKU", 100));
        return result;
    }
}
