package com.arbitrier.inventory.domain.event;

import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.platform.validation.Require;

import java.util.List;

/**
 * Domain event emitted when all requested stock quantities are successfully reserved.
 *
 * <p>Warehouse allocation details are not exposed in this event. Consumers receive
 * only business-level data: reservation ID, order ID, and per-line quantities.
 *
 * <p>Layer: domain/event
 * <p>Module: inventory-service
 */
public record StockReservedDomainEvent(
        StockReservationId reservationId,
        String orderId,
        List<StockReservationLine> lines) {

    public StockReservedDomainEvent {
        Require.notNull(reservationId, "StockReservedDomainEvent.reservationId");
        Require.notBlank(orderId, "StockReservedDomainEvent.orderId");
        Require.notEmpty(lines, "StockReservedDomainEvent.lines");
        lines = List.copyOf(lines);
    }
}
