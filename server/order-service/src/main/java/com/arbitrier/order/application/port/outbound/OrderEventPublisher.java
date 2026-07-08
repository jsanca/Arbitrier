package com.arbitrier.order.application.port.outbound;

import com.arbitrier.order.domain.event.OrderCreatedDomainEvent;

/**
 * Outbound port: publishes order domain events to downstream consumers.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: order-service
 */
public interface OrderEventPublisher {

    /** Publishes the event that an order was created. */
    void publish(OrderCreatedDomainEvent event);
}
