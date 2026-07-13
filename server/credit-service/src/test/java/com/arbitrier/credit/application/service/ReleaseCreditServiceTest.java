package com.arbitrier.credit.application.service;

import com.arbitrier.credit.adapter.outbound.InMemoryCreditReservationRepository;
import com.arbitrier.credit.application.port.inbound.ReleaseCreditCommand;
import com.arbitrier.credit.application.port.inbound.ReleaseCreditResult;
import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.CreditReservationStatus;
import com.arbitrier.credit.domain.model.Money;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.messaging.serialization.JacksonEventSerializer;
import com.arbitrier.platform.messaging.test.InMemoryOutboxRepository;
import com.arbitrier.platform.time.FixedTimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ReleaseCreditService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class ReleaseCreditServiceTest {

    private static final String ORDER_ID = "order-release-001";
    private static final String RESERVATION_ID = "cr-release-001";

    private static final CreditReservationId RES_ID = CreditReservationId.of(RESERVATION_ID);
    private static final Money AMOUNT = Money.of(new BigDecimal("500.00"), "USD");

    private InMemoryCreditReservationRepository repository;
    private InMemoryOutboxRepository outboxRepository;
    private DomainEventToOutboxMapper outboxMapper;
    private ReleaseCreditService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCreditReservationRepository();
        outboxRepository = new InMemoryOutboxRepository();
        outboxMapper = new DomainEventToOutboxMapper(
                new JacksonEventSerializer(new ObjectMapper()),
                FixedTimeProvider.of(Instant.parse("2026-01-15T10:00:00Z")));
        service = new ReleaseCreditService(repository, outboxRepository, outboxMapper);
    }

    // ── Release APPROVED reservation ──────────────────────────────────────────

    @Test
    void release_approved_reservation_returns_result_with_reservation_id() {
        saveApproved();

        ReleaseCreditResult result = service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(result.reservationId()).isEqualTo(RES_ID);
    }

    @Test
    void release_approved_reservation_persists_released_status() {
        saveApproved();

        service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(repository.getById(RES_ID).status()).isEqualTo(CreditReservationStatus.RELEASED);
    }

    @Test
    void release_approved_reservation_writes_credit_released_event_to_outbox() {
        saveApproved();

        service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(outboxRepository.findAll()).hasSize(1);
        var event = outboxRepository.findAll().get(0);
        assertThat(event.eventType()).isEqualTo("CreditReleasedDomainEvent");
        assertThat(event.aggregateType()).isEqualTo("CreditReservation");
        assertThat(event.aggregateId()).isEqualTo(RESERVATION_ID);
    }

    // ── Idempotency ───────────────────────────────────────────────────────────

    @Test
    void release_already_released_returns_same_result() {
        saveApproved();
        service.release(new ReleaseCreditCommand(RESERVATION_ID)); // first call

        ReleaseCreditResult secondResult = service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(secondResult.reservationId()).isEqualTo(RES_ID);
    }

    @Test
    void release_already_released_does_not_publish_duplicate_event() {
        saveApproved();
        service.release(new ReleaseCreditCommand(RESERVATION_ID)); // first: publishes

        service.release(new ReleaseCreditCommand(RESERVATION_ID)); // second: no-op

        assertThat(outboxRepository.findAll()).hasSize(1); // only one event total
    }

    // ── Release REJECTED reservation ─────────────────────────────────────────
    //
    // Decision: REJECTED reservations never held credit, so releasing is a no-op.
    // The application service checks status before calling domain release() (which
    // would throw for REJECTED) and returns without persisting or writing to the outbox.
    // Documented in ReleaseCreditService Javadoc.

    @Test
    void release_rejected_reservation_returns_result() {
        saveRejected();

        ReleaseCreditResult result = service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(result.reservationId()).isEqualTo(RES_ID);
    }

    @Test
    void release_rejected_reservation_does_not_write_any_outbox_event() {
        saveRejected();

        service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void release_rejected_reservation_does_not_change_persisted_status() {
        saveRejected();

        service.release(new ReleaseCreditCommand(RESERVATION_ID));

        // Status remains REJECTED — no state transition for a no-op release
        assertThat(repository.getById(RES_ID).status()).isEqualTo(CreditReservationStatus.REJECTED);
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void release_non_existent_reservation_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.release(new ReleaseCreditCommand("does-not-exist")))
                .withMessageContaining("does-not-exist");
    }

    // ── Repository interactions ───────────────────────────────────────────────

    @Test
    void release_approved_calls_repository_save_with_released_status() {
        saveApproved();

        service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(repository.getById(RES_ID).status()).isEqualTo(CreditReservationStatus.RELEASED);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_reservation_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReleaseCreditCommand(""))
                .withMessageContaining("creditReservationId");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveApproved() {
        repository.save(CreditReservation.approved(RES_ID, ORDER_ID, AMOUNT));
    }

    private void saveRejected() {
        repository.save(CreditReservation.rejected(RES_ID, ORDER_ID, AMOUNT));
    }
}
