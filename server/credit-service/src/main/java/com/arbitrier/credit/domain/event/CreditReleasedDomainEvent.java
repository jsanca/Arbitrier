package com.arbitrier.credit.domain.event;

import com.arbitrier.credit.domain.model.CreditReservationId;

/**
 * Emitted when a previously approved credit reservation is released back to the credit line.
 *
 * <p>Not emitted for REJECTED reservations; they never held any credit.
 *
 * <p>Layer: domain/event
 * <p>Module: credit-service
 */
public record CreditReleasedDomainEvent(
        CreditReservationId reservationId,
        String orderId) {
}
