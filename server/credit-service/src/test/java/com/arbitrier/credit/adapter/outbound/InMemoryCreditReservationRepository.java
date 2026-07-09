package com.arbitrier.credit.adapter.outbound;

import com.arbitrier.credit.application.port.outbound.CreditReservationRepository;
import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test adapter: HashMap-backed credit reservation repository for unit tests.
 *
 * <p>Layer: adapter/outbound (test)
 * <p>Module: credit-service
 */
public class InMemoryCreditReservationRepository implements CreditReservationRepository {

    private final Map<CreditReservationId, CreditReservation> store = new HashMap<>();

    @Override
    public void save(final CreditReservation reservation) {
        store.put(reservation.id(), reservation);
    }

    @Override
    public Optional<CreditReservation> findById(final CreditReservationId id) {
        return Optional.ofNullable(store.get(id));
    }

    /** Retrieves a reservation by ID, throwing if absent. For assertions in tests. */
    public CreditReservation getById(final CreditReservationId id) {
        return findById(id).orElseThrow(
                () -> new IllegalArgumentException("No credit reservation found with id: " + id));
    }
}
