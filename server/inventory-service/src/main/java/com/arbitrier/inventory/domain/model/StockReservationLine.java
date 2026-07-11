package com.arbitrier.inventory.domain.model;

import com.arbitrier.platform.validation.Require;

import java.util.List;

/**
 * Reservation outcome for a single SKU line.
 *
 * <p>The {@link #reservedQuantity()} is derived from the sum of internal
 * {@link StockAllocation} quantities, which may span multiple warehouses.
 * Warehouse allocation details are internal to the Inventory bounded context.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code requestedQuantity} must be positive.</li>
 *   <li>Total allocated quantity must not exceed {@code requestedQuantity}.</li>
 *   <li>Each allocation must reference the same SKU as this line.</li>
 * </ul>
 *
 * <p>Layer: domain/model
 * <p>Module: inventory-service
 */
public record StockReservationLine(String skuCode, int requestedQuantity, List<StockAllocation> allocations) {

    public StockReservationLine {
        Require.notBlank(skuCode, "StockReservationLine.skuCode");
        Require.isTrue(requestedQuantity > 0,
                "StockReservationLine.requestedQuantity must be positive, got: " + requestedQuantity);
        Require.notNull(allocations, "StockReservationLine.allocations");
        allocations = List.copyOf(allocations);

        allocations.forEach(a -> Require.isTrue(a.sku().equals(skuCode),
                "StockAllocation sku must match line sku, expected: " + skuCode + " got: " + a.sku()));

        int total = allocations.stream().mapToInt(StockAllocation::quantity).sum();
        Require.isTrue(total <= requestedQuantity,
                "Total allocation " + total + " exceeds requestedQuantity " + requestedQuantity
                        + " for sku: " + skuCode);
    }

    /** Returns the total quantity reserved across all warehouse allocations. */
    public int reservedQuantity() {
        return allocations.stream().mapToInt(StockAllocation::quantity).sum();
    }

    /** Returns true when all requested quantity has been reserved. */
    public boolean isFullyReserved() {
        return reservedQuantity() == requestedQuantity;
    }

    /** Returns true when some but not all requested quantity has been reserved. */
    public boolean isPartiallyReserved() {
        return reservedQuantity() > 0 && reservedQuantity() < requestedQuantity;
    }

    /** Returns true when none of the requested quantity has been reserved. */
    public boolean isUnreserved() {
        return reservedQuantity() == 0;
    }

    /** Creates a line with no allocation (nothing could be reserved for this SKU). */
    public static StockReservationLine unallocated(String sku, int requestedQuantity) {
        return new StockReservationLine(sku, requestedQuantity, List.of());
    }
}
