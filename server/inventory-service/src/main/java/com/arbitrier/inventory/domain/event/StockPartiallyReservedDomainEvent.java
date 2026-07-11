package com.arbitrier.inventory.domain.event;

import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.platform.validation.Require;

import java.util.List;

/**
 * Domain event emitted when some but not all requested stock quantities can be reserved.
 *
 * <p>At least one line has a non-zero reserved quantity; at least one line is not fully reserved.
 * Warehouse allocation details are not exposed in this event.
 *
 * <p>Layer: domain/event
 * <p>Module: inventory-service
 */
public record StockPartiallyReservedDomainEvent(
        StockReservationId reservationId,
        String orderId,
        List<StockReservationLine> lines) {

    public StockPartiallyReservedDomainEvent {
        Require.notNull(reservationId, "StockPartiallyReservedDomainEvent.reservationId");
        Require.notBlank(orderId, "StockPartiallyReservedDomainEvent.orderId");
        Require.notEmpty(lines, "StockPartiallyReservedDomainEvent.lines");
        lines = List.copyOf(lines);
    }
}
