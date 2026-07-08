package com.arbitrier.order.domain.event;

import com.arbitrier.order.domain.model.CustomerId;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.UserId;
import com.arbitrier.platform.validation.Require;
import java.util.List;

/**
 * Domain event emitted when a new corporate bulk order is created in {@code PENDING} state.
 *
 * <p>This is a pure-Java domain event. The Kafka integration layer maps it to an
 * {@code OrderCreated} Avro message when producing to the message broker.
 *
 * <p>Layer: domain/event
 * <p>Module: order-service
 */
public record OrderCreatedDomainEvent(
        OrderId orderId,
        CustomerId customerId,
        UserId submittedBy,
        List<OrderLine> lines) {

    public OrderCreatedDomainEvent {
        Require.notNull(orderId, "OrderCreatedDomainEvent.orderId");
        Require.notNull(customerId, "OrderCreatedDomainEvent.customerId");
        Require.notNull(submittedBy, "OrderCreatedDomainEvent.submittedBy");
        Require.notEmpty(lines, "OrderCreatedDomainEvent.lines");
        lines = List.copyOf(lines);
    }
}
