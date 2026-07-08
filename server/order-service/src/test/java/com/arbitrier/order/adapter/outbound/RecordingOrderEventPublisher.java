package com.arbitrier.order.adapter.outbound;

import com.arbitrier.order.application.port.outbound.OrderEventPublisher;
import com.arbitrier.order.domain.event.OrderCreatedDomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Recording {@link OrderEventPublisher} that captures published events for test assertions.
 */
public class RecordingOrderEventPublisher implements OrderEventPublisher {

    private final List<OrderCreatedDomainEvent> published = new ArrayList<>();

    @Override
    public void publish(OrderCreatedDomainEvent event) {
        published.add(event);
    }

    /** Returns an unmodifiable snapshot of all published events. */
    public List<OrderCreatedDomainEvent> events() {
        return List.copyOf(published);
    }

    /** Returns true if any event has been published. */
    public boolean hasEvents() {
        return !published.isEmpty();
    }
}
