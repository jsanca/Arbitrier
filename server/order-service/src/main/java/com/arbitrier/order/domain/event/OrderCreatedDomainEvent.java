package com.arbitrier.order.domain.event;

import com.arbitrier.order.domain.model.CustomerId;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.UserId;
import com.arbitrier.platform.messaging.event.DomainEvent;
import com.arbitrier.platform.messaging.event.EventDescriptor;
import com.arbitrier.platform.validation.Require;
import java.util.List;

/**
 * Domain event emitted when a new corporate bulk order is created in {@code PENDING} state.
 *
 * <p>Logical identity: type {@code "order.created"}, version {@code 1}.
 * The Outbox mapper reads this descriptor to produce a stable {@code eventType} in the
 * outbox record, independent of the Java class name.
 *
 * <p>The transport layer (ARB-024.x) maps this event to the {@code OrderCreated} Avro
 * contract when publishing via Schema Registry to Kafka.
 *
 * <p>Layer: domain/event
 * <p>Module: order-service
 */
public record OrderCreatedDomainEvent(
        OrderId orderId,
        CustomerId customerId,
        UserId submittedBy,
        List<OrderLine> lines) implements DomainEvent {

    public OrderCreatedDomainEvent {
        Require.notNull(orderId, "OrderCreatedDomainEvent.orderId");
        Require.notNull(customerId, "OrderCreatedDomainEvent.customerId");
        Require.notNull(submittedBy, "OrderCreatedDomainEvent.submittedBy");
        Require.notEmpty(lines, "OrderCreatedDomainEvent.lines");
        lines = List.copyOf(lines);
    }

    @Override
    public EventDescriptor descriptor() {
        return new EventDescriptor("order.created", 1);
    }
}
