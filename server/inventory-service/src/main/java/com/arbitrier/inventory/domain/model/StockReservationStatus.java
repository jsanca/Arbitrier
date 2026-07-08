package com.arbitrier.inventory.domain.model;

/**
 * Lifecycle status for a stock reservation.
 *
 * <p>Layer: domain/model
 * <p>Module: inventory-service
 */
public enum StockReservationStatus {
    RESERVED,
    PARTIALLY_RESERVED,
    REJECTED,
    RELEASED;

    /** Returns true if this reservation has reached a final, unmodifiable state. */
    public boolean isTerminal() {
        return this == REJECTED || this == RELEASED;
    }
}
