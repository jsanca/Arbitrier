package com.arbitrier.credit.domain.model;

/**
 * Lifecycle status for a credit reservation.
 *
 * <p>Layer: domain/model
 * <p>Module: credit-service
 */
public enum CreditReservationStatus {
    APPROVED,
    REJECTED,
    RELEASED;

    /** Returns true if this reservation has reached a final, unmodifiable state. */
    public boolean isTerminal() {
        return this == REJECTED || this == RELEASED;
    }
}
