package com.arbitrier.inventory.domain.event;

import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.inventory.domain.model.WarehouseId;
import com.arbitrier.platform.validation.Require;

import java.util.List;

/**
 * Domain event emitted when all requested stock quantities are successfully reserved.
 *
 * <p>Layer: domain/event
 * <p>Module: inventory-service
 */
public record StockReservedDomainEvent(
        StockReservationId reservationId,
        String orderId,
        WarehouseId warehouseId,
        List<StockReservationLine> lines) {

    public StockReservedDomainEvent {
        Require.notNull(reservationId, "StockReservedDomainEvent.reservationId");
        Require.notBlank(orderId, "StockReservedDomainEvent.orderId");
        Require.notNull(warehouseId, "StockReservedDomainEvent.warehouseId");
        Require.notEmpty(lines, "StockReservedDomainEvent.lines");
        lines = List.copyOf(lines);
    }
}
