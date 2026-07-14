package com.arbitrier.inventory.application.service;

import com.arbitrier.inventory.application.port.inbound.ReleaseStockCommand;
import com.arbitrier.inventory.application.port.inbound.ReleaseStockResult;
import com.arbitrier.inventory.application.port.inbound.ReleaseStockUseCase;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.domain.event.StockReleasedDomainEvent;
import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationStatus;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: release a stock reservation.
 *
 * <h2>Idempotency</h2>
 * <p>Releasing an already-RELEASED reservation is a no-op: the same result is returned
 * without persisting or writing an event to the outbox.
 *
 * <h2>Release of REJECTED reservations</h2>
 * <p>A REJECTED reservation held no stock — the quantity reserved was zero for every line.
 * Releasing it is treated as a no-op: the method returns immediately with the reservation ID
 * and does NOT persist a state change or write a {@link StockReleasedDomainEvent} to the outbox.
 *
 * <p>Layer: application/service
 * <p>Module: inventory-service
 */
public class ReleaseStockService implements ReleaseStockUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReleaseStockService.class);
    private static final String AGGREGATE_TYPE = "StockReservation";

    private final StockReservationRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;

    public ReleaseStockService(
            final StockReservationRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper) {

        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
    }

    @Override
    @Transactional
    public ReleaseStockResult release(final ReleaseStockCommand command) {

        final StockReservationId reservationId = StockReservationId.of(command.reservationId());

        final StockReservation reservation = repository.findById(reservationId)
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

        final StockReservation released = reservation.release();
        repository.save(released);
        outboxRepository.save(outboxMapper.map(
                new StockReleasedDomainEvent(reservationId, reservation.orderId()),
                reservationId.value(),
                AGGREGATE_TYPE));

        log.info("Stock reservation released reservationId={} orderId={}",
                reservationId, reservation.orderId());

        return new ReleaseStockResult(reservationId);
    }
}
