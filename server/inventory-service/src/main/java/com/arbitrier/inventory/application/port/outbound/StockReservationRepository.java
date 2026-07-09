package com.arbitrier.inventory.application.port.outbound;

import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;

import java.util.Optional;

/**
 * Outbound port: persists and loads {@link StockReservation} aggregates.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: inventory-service
 */
public interface StockReservationRepository {

    /** Persists or updates the reservation. */
    void save(StockReservation reservation);

    /** Finds a reservation by its unique identifier. */
    Optional<StockReservation> findById(StockReservationId id);
}
