package com.arbitrier.credit.application.service;

import com.arbitrier.credit.application.port.inbound.ReserveCreditCommand;
import com.arbitrier.credit.application.port.inbound.ReserveCreditResult;
import com.arbitrier.credit.application.port.inbound.ReserveCreditUseCase;
import com.arbitrier.credit.application.port.outbound.CreditLimitPort;
import com.arbitrier.credit.application.port.outbound.CreditReservationEventPublisher;
import com.arbitrier.credit.application.port.outbound.CreditReservationRepository;
import com.arbitrier.credit.domain.event.CreditApprovedDomainEvent;
import com.arbitrier.credit.domain.event.CreditRejectedDomainEvent;
import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.CreditReservationStatus;
import com.arbitrier.credit.domain.model.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: reserve credit for an order against the customer's credit limit.
 *
 * <h2>Reservation outcomes</h2>
 * <ul>
 *   <li><b>APPROVED</b> — the customer has sufficient available credit; the requested amount
 *       is reserved against their limit.</li>
 *   <li><b>REJECTED</b> — the customer's available credit is less than the requested amount.</li>
 * </ul>
 *
 * <h2>Allocation logic</h2>
 * <p>The available credit is queried via {@link CreditLimitPort#availableCredit(String)}.
 * If {@code requested ≤ available} the reservation is APPROVED; otherwise REJECTED.
 *
 * <p>OPEN QUESTION: Currency mismatch handling — if the requested currency differs from the
 * credit limit currency the behaviour is undefined. The JPA adapter implementation must
 * specify the comparison strategy before go-live.
 *
 * <p>Layer: application/service
 * <p>Module: credit-service
 */
public class ReserveCreditService implements ReserveCreditUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReserveCreditService.class);

    private final CreditLimitPort creditLimitPort;
    private final CreditReservationRepository repository;
    private final CreditReservationEventPublisher eventPublisher;

    public ReserveCreditService(
            final CreditLimitPort creditLimitPort,
            final CreditReservationRepository repository,
            final CreditReservationEventPublisher eventPublisher) {
        this.creditLimitPort = creditLimitPort;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ReserveCreditResult reserve(final ReserveCreditCommand command) {

        final CreditReservationId reservationId = CreditReservationId.of(command.creditReservationId());

        final Money available = creditLimitPort.availableCredit(command.customerId());
        final CreditReservation reservation = createReservation(reservationId, command, available);

        repository.save(reservation);
        publishEvent(reservation, command);

        log.info("Credit reservation complete reservationId={} orderId={} customerId={} amount={} {} outcome={}",
                reservationId, command.orderId(), command.customerId(),
                command.amount().amount(), command.amount().currency(), reservation.status());

        return new ReserveCreditResult(reservationId, reservation.status());
    }

    private CreditReservation createReservation(final CreditReservationId reservationId,
                                                 final ReserveCreditCommand command,
                                                 final Money available) {

        final boolean sufficient = available.canCover(command.amount());

        if (sufficient) {
            return CreditReservation.approved(reservationId, command.orderId(), command.amount());
        }
        return CreditReservation.rejected(reservationId, command.orderId(), command.amount());
    }

    private void publishEvent(final CreditReservation reservation, final ReserveCreditCommand command) {
        final CreditReservationId id = reservation.id();
        switch (reservation.status()) {
            case APPROVED -> eventPublisher.publishApproved(
                    new CreditApprovedDomainEvent(id, command.orderId(), command.customerId(), command.amount()));
            case REJECTED -> eventPublisher.publishRejected(
                    new CreditRejectedDomainEvent(id, command.orderId(), command.customerId(), command.amount()));
            default -> throw new IllegalStateException("Unexpected credit reservation status: " + reservation.status());
        }
    }
}
