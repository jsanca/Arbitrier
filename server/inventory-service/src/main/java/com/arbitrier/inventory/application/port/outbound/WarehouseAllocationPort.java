package com.arbitrier.inventory.application.port.outbound;

import java.util.List;

/**
 * Outbound port: queries global warehouse stock to obtain an allocation plan.
 *
 * <p>The port is used both for advisory availability checks and for authoritative
 * reservation planning. It is the single point through which the application layer
 * accesses warehouse-level stock data, keeping warehouse selection internal to the
 * Inventory bounded context.
 *
 * <p>The application and domain must not depend on whether the implementation uses
 * PostgreSQL, Redis, an external warehouse system, or an in-memory store.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: inventory-service
 */
public interface WarehouseAllocationPort {

    /**
     * Returns an allocation plan that satisfies the requested lines as fully as possible.
     *
     * <p>The plan may allocate a single requested line across multiple warehouses. If
     * total global stock for a SKU is less than the requested quantity, the plan contains
     * the available amount only (no over-allocation).
     *
     * @param lines the SKUs and quantities to allocate
     * @return an allocation plan with zero or more allocations per requested line
     */
    AllocationPlan allocate(List<RequestedStockLine> lines);
}
