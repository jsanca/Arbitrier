package com.arbitrier.credit.application.service;

import com.arbitrier.credit.adapter.outbound.InMemoryCreditReservationRepository;
import com.arbitrier.credit.adapter.outbound.RecordingCreditReservationEventPublisher;
import com.arbitrier.credit.application.port.inbound.ReleaseCreditCommand;
import com.arbitrier.credit.application.port.inbound.ReleaseCreditResult;
import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.CreditReservationStatus;
import com.arbitrier.credit.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
    private static final String CUSTOMER_ID = "cust-001";

    private static final CreditReservationId RES_ID = CreditReservationId.of(RESERVATION_ID);
    private static final Money AMOUNT = Money.of(new BigDecimal("500.00"), "USD");

    private InMemoryCreditReservationRepository repository;
    private RecordingCreditReservationEventPublisher publisher;
    private ReleaseCreditService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCreditReservationRepository();
        publisher = new RecordingCreditReservationEventPublisher();
        service = new ReleaseCreditService(repository, publisher);
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
    void release_approved_reservation_publishes_released_event() {
        saveApproved();

        service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(publisher.releasedEvents()).hasSize(1);
        assertThat(publisher.releasedEvents().get(0).reservationId()).isEqualTo(RES_ID);
        assertThat(publisher.releasedEvents().get(0).orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void release_approved_publishes_only_released_event() {
        saveApproved();

        service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(publisher.releasedEvents()).hasSize(1);
        assertThat(publisher.approvedEvents()).isEmpty();
        assertThat(publisher.rejectedEvents()).isEmpty();
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

        assertThat(publisher.releasedEvents()).hasSize(1); // only one event total
    }

    // ── Release REJECTED reservation ─────────────────────────────────────────
    //
    // Decision: REJECTED reservations never held credit, so releasing is a no-op.
    // The application service checks status before calling domain release() (which
    // would throw for REJECTED) and returns without persisting or publishing an event.
    // Documented in ReleaseCreditService Javadoc.

    @Test
    void release_rejected_reservation_returns_result() {
        saveRejected();

        ReleaseCreditResult result = service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(result.reservationId()).isEqualTo(RES_ID);
    }

    @Test
    void release_rejected_reservation_does_not_publish_any_event() {
        saveRejected();

        service.release(new ReleaseCreditCommand(RESERVATION_ID));

        assertThat(publisher.totalEventCount()).isZero();
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
