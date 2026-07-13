package com.arbitrier.credit.application.service;

import com.arbitrier.credit.application.port.inbound.ReleaseCreditCommand;
import com.arbitrier.credit.application.port.inbound.ReleaseCreditResult;
import com.arbitrier.credit.application.port.inbound.ReleaseCreditUseCase;
import com.arbitrier.credit.application.port.outbound.CreditReservationRepository;
import com.arbitrier.credit.domain.event.CreditReleasedDomainEvent;
import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.CreditReservationStatus;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: release an approved credit reservation back to the credit line.
 *
 * <h2>Idempotency</h2>
 * <p>Releasing an already-RELEASED reservation is a no-op: the same result is returned
 * without persisting or publishing an event.
 *
 * <h2>Release of REJECTED reservations</h2>
 * <p>A REJECTED reservation never held any credit — it records a denial, not an allocation.
 * Releasing it is a no-op at the application layer: the method returns the reservation ID
 * without persisting a state change or writing a {@link CreditReleasedDomainEvent} to the outbox.
 *
 * <p>Layer: application/service
 * <p>Module: credit-service
 */
public class ReleaseCreditService implements ReleaseCreditUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReleaseCreditService.class);
    private static final String AGGREGATE_TYPE = "CreditReservation";

    private final CreditReservationRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;

    public ReleaseCreditService(
            CreditReservationRepository repository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
    }

    @Override
    @Transactional
    public ReleaseCreditResult release(final ReleaseCreditCommand command) {
        final CreditReservationId reservationId = CreditReservationId.of(command.creditReservationId());

        final CreditReservation reservation = repository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No credit reservation found with id: " + reservationId));

        if (reservation.status() == CreditReservationStatus.RELEASED) {
            log.info("Credit reservation already released (idempotent) reservationId={} orderId={}",
                    reservationId, reservation.orderId());
            return new ReleaseCreditResult(reservationId);
        }

        // REJECTED reservations never held credit — no-op at application layer
        if (reservation.status() == CreditReservationStatus.REJECTED) {
            log.info("Credit release no-op for REJECTED reservation reservationId={} orderId={}",
                    reservationId, reservation.orderId());
            return new ReleaseCreditResult(reservationId);
        }

        final CreditReservation released = reservation.release();
        repository.save(released);
        outboxRepository.save(outboxMapper.map(
                new CreditReleasedDomainEvent(reservationId, reservation.orderId()),
                reservationId.value(),
                AGGREGATE_TYPE));

        log.info("Credit reservation released reservationId={} orderId={}",
                reservationId, reservation.orderId());

        return new ReleaseCreditResult(reservationId);
    }
}
