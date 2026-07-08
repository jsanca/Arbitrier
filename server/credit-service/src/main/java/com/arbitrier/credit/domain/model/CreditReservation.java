package com.arbitrier.credit.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Aggregate root representing a credit reservation for an order.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@link #release()} is only valid for {@code APPROVED} reservations; idempotent if
 *       already {@code RELEASED}.</li>
 *   <li>{@code REJECTED} reservations represent a credit denial and hold no reserved credit.</li>
 * </ul>
 *
 * <p>Layer: domain/model
 * <p>Module: credit-service
 */
public final class CreditReservation {

    private final CreditReservationId id;
    private final String orderId;
    private final Money amount;
    private final CreditReservationStatus status;

    private CreditReservation(CreditReservationId id, String orderId, Money amount,
                               CreditReservationStatus status) {
        this.id = Require.notNull(id, "CreditReservation.id");
        this.orderId = Require.notBlank(orderId, "CreditReservation.orderId");
        this.amount = Require.notNull(amount, "CreditReservation.amount");
        this.status = Require.notNull(status, "CreditReservation.status");
    }

    /** Creates an approved credit reservation. */
    public static CreditReservation approved(CreditReservationId id, String orderId, Money amount) {
        return new CreditReservation(id, orderId, amount, CreditReservationStatus.APPROVED);
    }

    /** Creates a rejected credit reservation (credit limit exceeded or unavailable). */
    public static CreditReservation rejected(CreditReservationId id, String orderId,
                                              Money requestedAmount) {
        return new CreditReservation(id, orderId, requestedAmount,
                CreditReservationStatus.REJECTED);
    }

    /**
     * Releases an approved reservation back to the credit line.
     * Idempotent if already {@code RELEASED}.
     *
     * @throws IllegalArgumentException if the reservation is {@code REJECTED}
     */
    public CreditReservation release() {
        if (status == CreditReservationStatus.RELEASED) {
            return this;
        }
        Require.isTrue(status == CreditReservationStatus.APPROVED,
                "release() is only valid for APPROVED reservations, current: " + status);
        return new CreditReservation(id, orderId, amount, CreditReservationStatus.RELEASED);
    }

    /** Returns the unique reservation identifier. */
    public CreditReservationId id() {
        return id;
    }

    /** Returns the identifier of the order this reservation belongs to. */
    public String orderId() {
        return orderId;
    }

    /** Returns the reserved monetary amount. */
    public Money amount() {
        return amount;
    }

    /** Returns the current lifecycle status of this reservation. */
    public CreditReservationStatus status() {
        return status;
    }
}
