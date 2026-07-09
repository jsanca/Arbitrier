package com.arbitrier.inventory.adapter.outbound;

import com.arbitrier.inventory.application.port.outbound.StockAvailabilityPort;
import com.arbitrier.inventory.domain.model.WarehouseId;

import java.util.HashMap;
import java.util.Map;

/**
 * Configurable {@link StockAvailabilityPort} for use in unit tests.
 *
 * <p>Allows per-SKU availability to be set before each test. Returns 0 for any SKU
 * that has not been explicitly configured (simulates out-of-stock).
 *
 * <p>Warehouse ID is ignored — this adapter applies the same availability across all warehouses.
 *
 * <p>Not for production use.
 */
public class ConfigurableStockAvailabilityPort implements StockAvailabilityPort {

    private final Map<String, Integer> availability = new HashMap<>();

    /**
     * Sets the available quantity for the given SKU.
     *
     * @param sku      the stock-keeping unit code
     * @param quantity available units; must be non-negative
     */
    public void setAvailable(String sku, int quantity) {
        availability.put(sku, quantity);
    }

    /** Makes all SKUs return unlimited availability ({@code Integer.MAX_VALUE}). */
    public void setUnlimited(String... skus) {
        for (String sku : skus) {
            availability.put(sku, Integer.MAX_VALUE);
        }
    }

    @Override
    public int availableQuantity(WarehouseId warehouseId, String sku) {
        return availability.getOrDefault(sku, 0);
    }
}
