package com.arbitrier.credit.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Unique identifier for a credit reservation within the credit bounded context.
 *
 * <p>Layer: domain/model
 * <p>Module: credit-service
 */
public record CreditReservationId(String value) {
    public CreditReservationId {
        Require.notBlank(value, "CreditReservationId.value");
    }

    /** Creates a {@code CreditReservationId} from the given string. */
    public static CreditReservationId of(String value) {
        return new CreditReservationId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
