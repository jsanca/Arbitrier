package com.arbitrier.credit.application.service;

import com.arbitrier.credit.adapter.outbound.ConfigurableCreditLimitPort;
import com.arbitrier.credit.adapter.outbound.InMemoryCreditReservationRepository;
import com.arbitrier.credit.application.port.inbound.ReserveCreditCommand;
import com.arbitrier.credit.application.port.inbound.ReserveCreditResult;
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
    private InMemoryOutboxRepository outboxRepository;
    private DomainEventToOutboxMapper outboxMapper;
    private ReserveCreditService service;

    @BeforeEach
    void setUp() {
        creditLimitPort = new ConfigurableCreditLimitPort();
        repository = new InMemoryCreditReservationRepository();
        outboxRepository = new InMemoryOutboxRepository();
        outboxMapper = new DomainEventToOutboxMapper(
                new JacksonEventSerializer(new ObjectMapper()),
                FixedTimeProvider.of(Instant.parse("2026-01-15T10:00:00Z")));
        service = new ReserveCreditService(creditLimitPort, repository, outboxRepository, outboxMapper);
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
    void reserve_when_sufficient_credit_writes_credit_approved_event_to_outbox() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, ONE_THOUSAND_USD);

        service.reserve(command(ONE_THOUSAND_USD));

        assertThat(outboxRepository.findAll()).hasSize(1);
        var event = outboxRepository.findAll().get(0);
        assertThat(event.eventType()).isEqualTo("CreditApprovedDomainEvent");
        assertThat(event.aggregateType()).isEqualTo("CreditReservation");
        assertThat(event.aggregateId()).isEqualTo(RESERVATION_ID);
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
    void reserve_when_insufficient_credit_writes_credit_rejected_event_to_outbox() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, FIVE_HUNDRED_USD);

        service.reserve(command(ONE_THOUSAND_USD));

        assertThat(outboxRepository.findAll()).hasSize(1);
        var event = outboxRepository.findAll().get(0);
        assertThat(event.eventType()).isEqualTo("CreditRejectedDomainEvent");
        assertThat(event.aggregateId()).isEqualTo(RESERVATION_ID);
    }

    @Test
    void reserve_when_zero_available_credit_is_rejected() {
        // Default in ConfigurableCreditLimitPort is zero — no setup needed

        ReserveCreditResult result = service.reserve(command(ONE_THOUSAND_USD));

        assertThat(result.outcome()).isEqualTo(CreditReservationStatus.REJECTED);
    }

    // ── Only one event published per call ─────────────────────────────────────

    @Test
    void reserve_approved_writes_only_one_outbox_event() {
        creditLimitPort.setAvailableCredit(CUSTOMER_ID, TWO_THOUSAND_USD);

        service.reserve(command(ONE_THOUSAND_USD));

        assertThat(outboxRepository.findAll()).hasSize(1);
        assertThat(outboxRepository.findAll().get(0).eventType()).isEqualTo("CreditApprovedDomainEvent");
    }

    @Test
    void reserve_rejected_writes_only_one_outbox_event() {
        // Zero credit by default

        service.reserve(command(ONE_THOUSAND_USD));

        assertThat(outboxRepository.findAll()).hasSize(1);
        assertThat(outboxRepository.findAll().get(0).eventType()).isEqualTo("CreditRejectedDomainEvent");
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
