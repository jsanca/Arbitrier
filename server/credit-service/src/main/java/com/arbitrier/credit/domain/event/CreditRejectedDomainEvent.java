package com.arbitrier.credit.domain.event;

import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.Money;
import com.arbitrier.platform.validation.Require;

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

    public CreditRejectedDomainEvent {
        Require.notNull(reservationId, "CreditRejectedDomainEvent.reservationId");
        Require.notBlank(orderId, "CreditRejectedDomainEvent.orderId");
        Require.notBlank(customerId, "CreditRejectedDomainEvent.customerId");
        Require.notNull(amount, "CreditRejectedDomainEvent.amount");
    }
}
