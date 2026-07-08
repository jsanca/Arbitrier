package com.arbitrier.inventory.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Represents the reservation outcome for a single SKU line.
 *
 * <p>Layer: domain/model
 * <p>Module: inventory-service
 */
public record StockReservationLine(String skuCode, int requestedQuantity, int reservedQuantity) {
    public StockReservationLine {
        Require.notBlank(skuCode, "StockReservationLine.skuCode");
        Require.isTrue(requestedQuantity > 0,
                "StockReservationLine.requestedQuantity must be positive, got: " + requestedQuantity);
        Require.isTrue(reservedQuantity >= 0,
                "StockReservationLine.reservedQuantity must be non-negative, got: " + reservedQuantity);
        Require.isTrue(reservedQuantity <= requestedQuantity,
                "StockReservationLine.reservedQuantity must not exceed requestedQuantity");
    }

    /** Returns true when all requested quantity has been reserved. */
    public boolean isFullyReserved() {
        return reservedQuantity == requestedQuantity;
    }

    /** Returns true when some but not all requested quantity has been reserved. */
    public boolean isPartiallyReserved() {
        return reservedQuantity > 0 && reservedQuantity < requestedQuantity;
    }

    /** Returns true when none of the requested quantity has been reserved. */
    public boolean isUnreserved() {
        return reservedQuantity == 0;
    }
}
