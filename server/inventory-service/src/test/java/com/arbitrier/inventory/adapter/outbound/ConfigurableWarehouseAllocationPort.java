package com.arbitrier.inventory.adapter.outbound;

import com.arbitrier.inventory.application.port.outbound.AllocationPlan;
import com.arbitrier.inventory.application.port.outbound.RequestedStockLine;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
import com.arbitrier.inventory.domain.model.StockAllocation;
import com.arbitrier.inventory.domain.model.WarehouseId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurable in-memory {@link WarehouseAllocationPort} for use in tests.
 *
 * <p>Stock levels are registered per-warehouse per-SKU. Allocation is greedy first-fit:
 * warehouses are iterated in insertion order; each warehouse fills as much of the remaining
 * requested quantity as its stock allows.
 *
 * <p>Not for production use.
 */
public class ConfigurableWarehouseAllocationPort implements WarehouseAllocationPort {

    private static final String DEFAULT_WAREHOUSE = "default-warehouse";

    private final LinkedHashMap<String, Map<String, Integer>> warehouseStock = new LinkedHashMap<>();

    /** Sets available stock for a specific warehouse and SKU. */
    public void setAvailable(final String warehouseId, final String sku, final int quantity) {
        warehouseStock.computeIfAbsent(warehouseId, k -> new LinkedHashMap<>()).put(sku, quantity);
    }

    /**
     * Convenience form: sets available stock in the default warehouse.
     * Useful for single-warehouse tests that don't care about warehouse identity.
     */
    public void setAvailable(final String sku, final int quantity) {
        setAvailable(DEFAULT_WAREHOUSE, sku, quantity);
    }

    @Override
    public AllocationPlan allocate(final List<RequestedStockLine> lines) {
        final List<StockAllocation> allocations = new ArrayList<>();

        for (final RequestedStockLine line : lines) {
            int remaining = line.requestedQuantity();

            for (final Map.Entry<String, Map<String, Integer>> whEntry : warehouseStock.entrySet()) {
                if (remaining == 0) break;

                final String warehouseId = whEntry.getKey();
                final int stock = whEntry.getValue().getOrDefault(line.sku(), 0);
                final int allocated = Math.min(stock, remaining);

                if (allocated > 0) {
                    allocations.add(new StockAllocation(WarehouseId.of(warehouseId), line.sku(), allocated));
                    remaining -= allocated;
                }
            }
        }

        return new AllocationPlan(allocations);
    }
}
