package com.arbitrier.inventory.domain.event;

import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.inventory.domain.model.WarehouseId;
import com.arbitrier.platform.validation.Require;

import java.util.List;

/**
 * Domain event emitted when some but not all requested stock quantities can be reserved.
 *
 * <p>At least one line has a non-zero reserved quantity; at least one line is not fully reserved.
 *
 * <p>Layer: domain/event
 * <p>Module: inventory-service
 */
public record StockPartiallyReservedDomainEvent(
        StockReservationId reservationId,
        String orderId,
        WarehouseId warehouseId,
        List<StockReservationLine> lines) {

    public StockPartiallyReservedDomainEvent {
        Require.notNull(reservationId, "StockPartiallyReservedDomainEvent.reservationId");
        Require.notBlank(orderId, "StockPartiallyReservedDomainEvent.orderId");
        Require.notNull(warehouseId, "StockPartiallyReservedDomainEvent.warehouseId");
        Require.notEmpty(lines, "StockPartiallyReservedDomainEvent.lines");
        lines = List.copyOf(lines);
    }
}
