package com.arbitrier.inventory.application.service;

import com.arbitrier.inventory.application.port.inbound.ReleaseStockCommand;
import com.arbitrier.inventory.application.port.inbound.ReleaseStockResult;
import com.arbitrier.inventory.application.port.inbound.ReleaseStockUseCase;
import com.arbitrier.inventory.application.port.outbound.StockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.domain.event.StockReleasedDomainEvent;
import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use-case implementation: release a stock reservation.
 *
 * <h2>Idempotency</h2>
 * <p>Releasing an already-RELEASED reservation is a no-op: the same result is returned
 * without persisting or publishing an event.
 *
 * <h2>Release of REJECTED reservations</h2>
 * <p>A REJECTED reservation held no stock — the quantity reserved was zero for every line.
 * Releasing it is treated as a no-op: the method returns immediately with the reservation ID
 * and does NOT persist a state change or publish {@link StockReleasedDomainEvent}.
 *
 * <p>Rationale: emitting a {@code StockReleased} event for stock that was never held would
 * be misleading to downstream consumers. The orchestrator must not rely on receiving
 * {@code StockReleased} as a follow-up to {@code StockRejected}.
 *
 * <p>OPEN QUESTION: Confirm with orchestrator-service that the saga compensation path does
 * not route through {@code StockReleased} for the REJECTED outcome; if it does, this behavior
 * must be revisited in ARB-013 / saga wiring.
 *
 * <p>Layer: application/service
 * <p>Module: inventory-service
 */
public class ReleaseStockService implements ReleaseStockUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReleaseStockService.class);

    private final StockReservationRepository repository;
    private final StockReservationEventPublisher eventPublisher;

    public ReleaseStockService(
            StockReservationRepository repository,
            StockReservationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ReleaseStockResult release(ReleaseStockCommand command) {
        StockReservationId reservationId = StockReservationId.of(command.reservationId());

        StockReservation reservation = repository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No stock reservation found with id: " + reservationId));

        // Idempotent: already released — return without side effects
        if (reservation.status() == StockReservationStatus.RELEASED) {
            log.info("Stock reservation already released (idempotent) reservationId={} orderId={}",
                    reservationId, reservation.orderId());
            return new ReleaseStockResult(reservationId);
        }

        // No-op for REJECTED reservations — no stock was ever held
        if (reservation.status() == StockReservationStatus.REJECTED) {
            log.info("Stock release no-op for REJECTED reservation reservationId={} orderId={}",
                    reservationId, reservation.orderId());
            return new ReleaseStockResult(reservationId);
        }

        StockReservation released = reservation.release();
        repository.save(released);
        eventPublisher.publishReleased(
                new StockReleasedDomainEvent(reservationId, reservation.orderId()));

        log.info("Stock reservation released reservationId={} orderId={}",
                reservationId, reservation.orderId());

        return new ReleaseStockResult(reservationId);
    }
}
