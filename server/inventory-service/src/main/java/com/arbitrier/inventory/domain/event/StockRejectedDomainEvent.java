package com.arbitrier.inventory.domain.event;

import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.WarehouseId;
import com.arbitrier.platform.validation.Require;

/**
 * Domain event emitted when none of the requested stock lines can be reserved.
 *
 * <p>Layer: domain/event
 * <p>Module: inventory-service
 */
public record StockRejectedDomainEvent(
        StockReservationId reservationId,
        String orderId,
        WarehouseId warehouseId) {

    public StockRejectedDomainEvent {
        Require.notNull(reservationId, "StockRejectedDomainEvent.reservationId");
        Require.notBlank(orderId, "StockRejectedDomainEvent.orderId");
        Require.notNull(warehouseId, "StockRejectedDomainEvent.warehouseId");
    }
}
