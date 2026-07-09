package com.arbitrier.credit.application.port.inbound;

import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.CreditReservationStatus;

/**
 * Result of a credit reservation attempt.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: credit-service
 */
public record ReserveCreditResult(CreditReservationId reservationId, CreditReservationStatus outcome) {
}
