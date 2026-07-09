package com.arbitrier.inventory.adapter.outbound;

import com.arbitrier.inventory.application.port.outbound.StockReservationEventPublisher;
import com.arbitrier.inventory.domain.event.StockPartiallyReservedDomainEvent;
import com.arbitrier.inventory.domain.event.StockRejectedDomainEvent;
import com.arbitrier.inventory.domain.event.StockReleasedDomainEvent;
import com.arbitrier.inventory.domain.event.StockReservedDomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Recording {@link StockReservationEventPublisher} for use in unit tests.
 *
 * <p>Captures all published events so tests can assert on which events were emitted
 * and in what order.
 *
 * <p>Not for production use.
 */
public class RecordingStockReservationEventPublisher implements StockReservationEventPublisher {

    private final List<StockReservedDomainEvent> reservedEvents = new ArrayList<>();
    private final List<StockPartiallyReservedDomainEvent> partiallyReservedEvents = new ArrayList<>();
    private final List<StockRejectedDomainEvent> rejectedEvents = new ArrayList<>();
    private final List<StockReleasedDomainEvent> releasedEvents = new ArrayList<>();

    @Override
    public void publishReserved(StockReservedDomainEvent event) {
        reservedEvents.add(event);
    }

    @Override
    public void publishPartiallyReserved(StockPartiallyReservedDomainEvent event) {
        partiallyReservedEvents.add(event);
    }

    @Override
    public void publishRejected(StockRejectedDomainEvent event) {
        rejectedEvents.add(event);
    }

    @Override
    public void publishReleased(StockReleasedDomainEvent event) {
        releasedEvents.add(event);
    }

    /** Returns all published {@link StockReservedDomainEvent}s. */
    public List<StockReservedDomainEvent> reservedEvents() {
        return List.copyOf(reservedEvents);
    }

    /** Returns all published {@link StockPartiallyReservedDomainEvent}s. */
    public List<StockPartiallyReservedDomainEvent> partiallyReservedEvents() {
        return List.copyOf(partiallyReservedEvents);
    }

    /** Returns all published {@link StockRejectedDomainEvent}s. */
    public List<StockRejectedDomainEvent> rejectedEvents() {
        return List.copyOf(rejectedEvents);
    }

    /** Returns all published {@link StockReleasedDomainEvent}s. */
    public List<StockReleasedDomainEvent> releasedEvents() {
        return List.copyOf(releasedEvents);
    }

    /** Returns the total number of events published across all types. */
    public int totalEventCount() {
        return reservedEvents.size() + partiallyReservedEvents.size()
                + rejectedEvents.size() + releasedEvents.size();
    }
}
