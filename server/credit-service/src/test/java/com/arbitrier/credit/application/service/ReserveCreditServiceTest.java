package com.arbitrier.credit.application.service;

import com.arbitrier.credit.adapter.outbound.ConfigurableCreditLimitPort;
import com.arbitrier.credit.adapter.outbound.InMemoryCreditReservationRepository;
import com.arbitrier.credit.adapter.outbound.RecordingCreditReservationEventPublisher;
import com.arbitrier.credit.application.port.inbound.ReserveCreditCommand;
import com.arbitrier.credit.application.port.inbound.ReserveCreditResult;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.CreditReservationStatus;
import com.arbitrier.credit.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link ReserveCreditService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class ReserveCreditServiceTest {

    private static final String ORDER_ID = "order-001";
    private static final String RESERVATION_ID = "cr-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final CreditReservationId RES_ID = CreditReservationId.of(RESERVATION_ID);

    private static final Money ONE_THOUSAND_USD = Money.of(new BigDecimal("1000.00"), "USD");
    private static final Money FIVE_HUNDRED_USD = Money.of(new BigDecimal("500.00"), "USD");
    private static final Money TWO_THOUSAND_USD = Money.of(new BigDecimal("2000.00"), "USD");

    private ConfigurableCreditLimitPort creditLimitPort;
    private InMemoryCreditReservationRepository repository;
    private RecordingCreditReservationEventPublisher publisher;
    private ReserveCreditService service;

    @BeforeEach
    void setUp() {
        creditLimitPort = new ConfigurableCreditLimitPort();
        repository = new InMemoryCreditReservationRepository();
        publisher = new RecordingCreditReservationEventPublisher();
        service = new ReserveCreditService(creditLimitPort, repository, publisher);
    }

    // ── Approve happy path ────────────────────────────────────────────────────

    @Test
    void reserve_when_sufficient_credit_returns_approved_result() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, ONE_THOUSAND_USD);

        ReserveCreditResult result = service.reserve(command(ONE_THOUSAND_USD));

        assertThat(result.reservationId()).isEqualTo(RES_ID);
        assertThat(result.outcome()).isEqualTo(CreditReservationStatus.APPROVED);
    }

    @Test
    void reserve_when_sufficient_credit_persists_approved_reservation() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, ONE_THOUSAND_USD);

        service.reserve(command(ONE_THOUSAND_USD));

        assertThat(repository.getById(RES_ID).status()).isEqualTo(CreditReservationStatus.APPROVED);
    }

    @Test
    void reserve_when_sufficient_credit_publishes_approved_event() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, ONE_THOUSAND_USD);

        service.reserve(command(ONE_THOUSAND_USD));

        assertThat(publisher.approvedEvents()).hasSize(1);
        assertThat(publisher.approvedEvents().get(0).reservationId()).isEqualTo(RES_ID);
        assertThat(publisher.approvedEvents().get(0).orderId()).isEqualTo(ORDER_ID);
        assertThat(publisher.approvedEvents().get(0).customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(publisher.approvedEvents().get(0).amount()).isEqualTo(ONE_THOUSAND_USD);
    }

    @Test
    void reserve_when_requested_equals_available_is_approved() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, ONE_THOUSAND_USD);

        ReserveCreditResult result = service.reserve(command(ONE_THOUSAND_USD));

        assertThat(result.outcome()).isEqualTo(CreditReservationStatus.APPROVED);
    }

    @Test
    void reserve_when_requested_less_than_available_is_approved() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, ONE_THOUSAND_USD);

        ReserveCreditResult result = service.reserve(command(FIVE_HUNDRED_USD));

        assertThat(result.outcome()).isEqualTo(CreditReservationStatus.APPROVED);
    }

    // ── Reject path ───────────────────────────────────────────────────────────

    @Test
    void reserve_when_insufficient_credit_returns_rejected_result() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, FIVE_HUNDRED_USD);

        ReserveCreditResult result = service.reserve(command(ONE_THOUSAND_USD));

        assertThat(result.reservationId()).isEqualTo(RES_ID);
        assertThat(result.outcome()).isEqualTo(CreditReservationStatus.REJECTED);
    }

    @Test
    void reserve_when_insufficient_credit_persists_rejected_reservation() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, FIVE_HUNDRED_USD);

        service.reserve(command(ONE_THOUSAND_USD));

        assertThat(repository.getById(RES_ID).status()).isEqualTo(CreditReservationStatus.REJECTED);
    }

    @Test
    void reserve_when_insufficient_credit_publishes_rejected_event() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, FIVE_HUNDRED_USD);

        service.reserve(command(ONE_THOUSAND_USD));

        assertThat(publisher.rejectedEvents()).hasSize(1);
        assertThat(publisher.rejectedEvents().get(0).reservationId()).isEqualTo(RES_ID);
        assertThat(publisher.rejectedEvents().get(0).orderId()).isEqualTo(ORDER_ID);
        assertThat(publisher.rejectedEvents().get(0).customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(publisher.rejectedEvents().get(0).amount()).isEqualTo(ONE_THOUSAND_USD);
    }

    @Test
    void reserve_when_zero_available_credit_is_rejected() {
        // Default in ConfigurableCreditLimitPort is zero — no setup needed

        ReserveCreditResult result = service.reserve(command(ONE_THOUSAND_USD));

        assertThat(result.outcome()).isEqualTo(CreditReservationStatus.REJECTED);
    }

    // ── Only one event published per call ─────────────────────────────────────

    @Test
    void reserve_approved_publishes_only_approved_event() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, TWO_THOUSAND_USD);

        service.reserve(command(ONE_THOUSAND_USD));

        assertThat(publisher.approvedEvents()).hasSize(1);
        assertThat(publisher.rejectedEvents()).isEmpty();
        assertThat(publisher.releasedEvents()).isEmpty();
    }

    @Test
    void reserve_rejected_publishes_only_rejected_event() {
        // Zero credit by default

        service.reserve(command(ONE_THOUSAND_USD));

        assertThat(publisher.rejectedEvents()).hasSize(1);
        assertThat(publisher.approvedEvents()).isEmpty();
        assertThat(publisher.releasedEvents()).isEmpty();
    }

    // ── Repository save ───────────────────────────────────────────────────────

    @Test
    void reserve_always_calls_repository_save() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, ONE_THOUSAND_USD);

        service.reserve(command(ONE_THOUSAND_USD));

        // Verify save was called: the reservation is findable in the repository
        assertThat(repository.findById(RES_ID)).isPresent();
    }

    // ── Command validation ────────────────────────────────────────────────────

    @Test
    void blank_orderId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReserveCreditCommand("", RESERVATION_ID, CUSTOMER_ID, ONE_THOUSAND_USD))
                .withMessageContaining("orderId");
    }

    @Test
    void blank_creditReservationId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReserveCreditCommand(ORDER_ID, "", CUSTOMER_ID, ONE_THOUSAND_USD))
                .withMessageContaining("creditReservationId");
    }

    @Test
    void blank_customerId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReserveCreditCommand(ORDER_ID, RESERVATION_ID, "", ONE_THOUSAND_USD))
                .withMessageContaining("customerId");
    }

    @Test
    void null_amount_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ReserveCreditCommand(ORDER_ID, RESERVATION_ID, CUSTOMER_ID, null))
                .withMessageContaining("amount");
    }

    @Test
    void negative_amount_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.of(new BigDecimal("-1.00"), "USD"))
                .withMessageContaining("amount must be zero or positive");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReserveCreditCommand command(Money amount) {
        return new ReserveCreditCommand(ORDER_ID, RESERVATION_ID, CUSTOMER_ID, amount);
    }
}
