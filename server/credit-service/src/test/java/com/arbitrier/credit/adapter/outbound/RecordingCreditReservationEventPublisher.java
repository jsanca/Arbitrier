package com.arbitrier.credit.adapter.outbound;

import com.arbitrier.credit.application.port.outbound.CreditReservationEventPublisher;
import com.arbitrier.credit.domain.event.CreditApprovedDomainEvent;
import com.arbitrier.credit.domain.event.CreditRejectedDomainEvent;
import com.arbitrier.credit.domain.event.CreditReleasedDomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Test adapter: records published credit events for assertion in tests.
 *
 * <p>Layer: adapter/outbound (test)
 * <p>Module: credit-service
 */
public class RecordingCreditReservationEventPublisher implements CreditReservationEventPublisher {

    private final List<CreditApprovedDomainEvent> approvedEvents = new ArrayList<>();
    private final List<CreditRejectedDomainEvent> rejectedEvents = new ArrayList<>();
    private final List<CreditReleasedDomainEvent> releasedEvents = new ArrayList<>();

    @Override
    public void publishApproved(final CreditApprovedDomainEvent event) {
        approvedEvents.add(event);
    }

    @Override
    public void publishRejected(final CreditRejectedDomainEvent event) {
        rejectedEvents.add(event);
    }

    @Override
    public void publishReleased(final CreditReleasedDomainEvent event) {
        releasedEvents.add(event);
    }

    public List<CreditApprovedDomainEvent> approvedEvents() {
        return List.copyOf(approvedEvents);
    }

    public List<CreditRejectedDomainEvent> rejectedEvents() {
        return List.copyOf(rejectedEvents);
    }

    public List<CreditReleasedDomainEvent> releasedEvents() {
        return List.copyOf(releasedEvents);
    }

    public int totalEventCount() {
        return approvedEvents.size() + rejectedEvents.size() + releasedEvents.size();
    }
}
