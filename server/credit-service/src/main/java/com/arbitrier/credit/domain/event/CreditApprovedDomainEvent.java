package com.arbitrier.credit.domain.event;

import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.Money;
import com.arbitrier.platform.validation.Require;

/**
 * Emitted when a credit reservation is successfully approved.
 *
 * <p>Layer: domain/event
 * <p>Module: credit-service
 */
public record CreditApprovedDomainEvent(
        CreditReservationId reservationId,
        String orderId,
        String customerId,
        Money amount) {

    public CreditApprovedDomainEvent {
        Require.notNull(reservationId, "CreditApprovedDomainEvent.reservationId");
        Require.notBlank(orderId, "CreditApprovedDomainEvent.orderId");
        Require.notBlank(customerId, "CreditApprovedDomainEvent.customerId");
        Require.notNull(amount, "CreditApprovedDomainEvent.amount");
    }
}
