package com.arbitrier.credit.application.port.inbound;

import com.arbitrier.credit.domain.model.CreditReservationId;

/**
 * Result of a credit release operation.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: credit-service
 */
public record ReleaseCreditResult(CreditReservationId reservationId) {
}
