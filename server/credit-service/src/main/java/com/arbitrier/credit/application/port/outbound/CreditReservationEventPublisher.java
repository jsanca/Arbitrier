package com.arbitrier.credit.application.port.outbound;

import com.arbitrier.credit.domain.event.CreditApprovedDomainEvent;
import com.arbitrier.credit.domain.event.CreditRejectedDomainEvent;
import com.arbitrier.credit.domain.event.CreditReleasedDomainEvent;

/**
 * Outbound port: publishes credit reservation domain events.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: credit-service
 */
public interface CreditReservationEventPublisher {

    /** Publishes a {@link CreditApprovedDomainEvent}. */
    void publishApproved(CreditApprovedDomainEvent event);

    /** Publishes a {@link CreditRejectedDomainEvent}. */
    void publishRejected(CreditRejectedDomainEvent event);

    /** Publishes a {@link CreditReleasedDomainEvent}. */
    void publishReleased(CreditReleasedDomainEvent event);
}
