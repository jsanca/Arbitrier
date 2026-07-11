package com.arbitrier.inventory.application.port.outbound;

import com.arbitrier.inventory.domain.model.StockAllocation;

import java.util.List;

/**
 * The result of a {@link WarehouseAllocationPort} query: warehouse-level allocations for
 * each requested SKU.
 *
 * <p>An allocation plan may contain zero allocations for a SKU (nothing available),
 * one allocation (single-warehouse), or multiple allocations (split across warehouses).
 *
 * <p>Layer: application/port/outbound
 * <p>Module: inventory-service
 */
public record AllocationPlan(List<StockAllocation> allocations) {

    public AllocationPlan {
        allocations = List.copyOf(allocations);
    }

    /**
     * Returns all {@link StockAllocation}s for the given SKU.
     *
     * @param sku the stock-keeping unit code
     * @return matching allocations; empty if no stock is available for this SKU
     */
    public List<StockAllocation> forSku(final String sku) {
        return allocations.stream()
                .filter(a -> a.sku().equals(sku))
                .toList();
    }

    /**
     * Returns the total quantity allocated across all warehouses for the given SKU.
     *
     * @param sku the stock-keeping unit code
     * @return total allocated quantity; 0 if no allocation exists for this SKU
     */
    public int totalAllocated(final String sku) {
        return forSku(sku).stream().mapToInt(StockAllocation::quantity).sum();
    }
}
