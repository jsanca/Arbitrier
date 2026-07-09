package com.arbitrier.inventory.domain.event;

import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.platform.validation.Require;

/**
 * Domain event emitted when a stock reservation is released (RESERVED → RELEASED
 * or PARTIALLY_RESERVED → RELEASED).
 *
 * <p>This event is NOT emitted for REJECTED reservations — a rejected reservation
 * held no stock, so there is nothing to announce as released.
 *
 * <p>Layer: domain/event
 * <p>Module: inventory-service
 */
public record StockReleasedDomainEvent(
        StockReservationId reservationId,
        String orderId) {

    public StockReleasedDomainEvent {
        Require.notNull(reservationId, "StockReleasedDomainEvent.reservationId");
        Require.notBlank(orderId, "StockReleasedDomainEvent.orderId");
    }
}
