package com.arbitrier.inventory.adapter.outbound;

import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link StockReservationRepository} for use in unit and integration tests.
 *
 * <p>Not thread-safe. Not for production use.
 */
public class InMemoryStockReservationRepository implements StockReservationRepository {

    private final Map<StockReservationId, StockReservation> store = new HashMap<>();

    @Override
    public void save(StockReservation reservation) {
        store.put(reservation.id(), reservation);
    }

    @Override
    public Optional<StockReservation> findById(StockReservationId id) {
        return Optional.ofNullable(store.get(id));
    }

    /** Returns the number of reservations currently stored. */
    public int size() {
        return store.size();
    }

    /** Returns the stored reservation by ID, or throws if absent (for test assertions). */
    public StockReservation getById(StockReservationId id) {
        return findById(id).orElseThrow(() ->
                new AssertionError("Expected reservation with id " + id + " but not found"));
    }
}
