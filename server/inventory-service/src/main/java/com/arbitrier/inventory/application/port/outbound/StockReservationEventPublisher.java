package com.arbitrier.inventory.application.port.outbound;

import com.arbitrier.inventory.domain.event.StockPartiallyReservedDomainEvent;
import com.arbitrier.inventory.domain.event.StockRejectedDomainEvent;
import com.arbitrier.inventory.domain.event.StockReleasedDomainEvent;
import com.arbitrier.inventory.domain.event.StockReservedDomainEvent;

/**
 * Outbound port: publishes stock reservation domain events to downstream consumers.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: inventory-service
 */
public interface StockReservationEventPublisher {

    /** Publishes the event that all requested stock was reserved. */
    void publishReserved(StockReservedDomainEvent event);

    /** Publishes the event that some but not all requested stock was reserved. */
    void publishPartiallyReserved(StockPartiallyReservedDomainEvent event);

    /** Publishes the event that no stock could be reserved. */
    void publishRejected(StockRejectedDomainEvent event);

    /** Publishes the event that reserved stock was released. */
    void publishReleased(StockReleasedDomainEvent event);
}
