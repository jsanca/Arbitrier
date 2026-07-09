package com.arbitrier.credit.application.port.outbound;

import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;

import java.util.Optional;

/**
 * Outbound port: persists and loads {@link CreditReservation} aggregates.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: credit-service
 */
public interface CreditReservationRepository {

    /** Persists or updates the given reservation. */
    void save(CreditReservation reservation);

    /** Loads a reservation by its identifier, returning empty if not found. */
    Optional<CreditReservation> findById(CreditReservationId id);
}
