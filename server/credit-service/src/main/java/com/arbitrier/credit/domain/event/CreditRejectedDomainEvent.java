package com.arbitrier.credit.domain.event;

import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.Money;

/**
 * Emitted when a credit reservation is rejected due to insufficient available credit.
 *
 * <p>Layer: domain/event
 * <p>Module: credit-service
 */
public record CreditRejectedDomainEvent(
        CreditReservationId reservationId,
        String orderId,
        String customerId,
        Money amount) {
}
