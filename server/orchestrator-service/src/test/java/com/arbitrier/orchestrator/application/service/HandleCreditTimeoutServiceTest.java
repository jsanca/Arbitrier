package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditTimeoutCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditTimeoutResult;
import com.arbitrier.orchestrator.domain.model.CorporateBulkOrderSagaRetryPolicy;
import com.arbitrier.orchestrator.domain.model.RetryDecision;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link HandleCreditTimeoutService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class HandleCreditTimeoutServiceTest {

    private static final String SAGA_ID = "saga-001";
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final String STOCK_RES_ID = "stock-res-001";
    private static final String CREDIT_RES_ID = "credit-res-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private static final int MAX_ATTEMPTS = 3;
    private static final CorporateBulkOrderSagaRetryPolicy POLICY =
            new CorporateBulkOrderSagaRetryPolicy(MAX_ATTEMPTS, MAX_ATTEMPTS);

    private InMemorySagaRepository repository;
    private RecordingSagaEventPublisher eventPublisher;
    private RecordingReleaseStockCommandPublisher releaseStockPublisher;
    private HandleCreditTimeoutService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        eventPublisher = new RecordingSagaEventPublisher();
        releaseStockPublisher = new RecordingReleaseStockCommandPublisher();
        service = new HandleCreditTimeoutService(
                repository, eventPublisher, releaseStockPublisher, POLICY);

        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID)
                .awaitInventoryResponse()
                .inventoryReserved(STOCK_RES_ID)
                .awaitCreditResponse());
    }

    // ── Retry path ────────────────────────────────────────────────────────────

    @Test
    void credit_timeout_within_limit_returns_retry_decision() {
        HandleCreditTimeoutResult result = service.handle(command(1));

        assertThat(result.decision()).isEqualTo(RetryDecision.RETRY);
    }

    @Test
    void credit_timeout_within_limit_saga_remains_waiting_for_credit() {
        service.handle(command(1));

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.WAITING_FOR_CREDIT);
    }

    @Test
    void credit_timeout_within_limit_publishes_credit_timed_out_event() {
        service.handle(command(1));

        assertThat(eventPublisher.creditTimedOutEvents()).hasSize(1);
        var event = eventPublisher.creditTimedOutEvents().get(0);
        assertThat(event.sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.attemptNumber()).isEqualTo(1);
    }

    @Test
    void credit_timeout_retry_does_not_issue_release_stock_command() {
        service.handle(command(1));

        assertThat(releaseStockPublisher.commands()).isEmpty();
    }

    @Test
    void credit_timeout_result_contains_saga_id() {
        HandleCreditTimeoutResult result = service.handle(command(1));

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    // ── Exhaust path: compensation ────────────────────────────────────────────

    @Test
    void credit_timeout_exhaustion_returns_exhaust_decision() {
        HandleCreditTimeoutResult result = service.handle(command(MAX_ATTEMPTS));

        assertThat(result.decision()).isEqualTo(RetryDecision.EXHAUST);
    }

    @Test
    void credit_timeout_exhaustion_transitions_saga_to_compensating() {
        service.handle(command(MAX_ATTEMPTS));

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.COMPENSATING);
    }

    @Test
    void credit_timeout_exhaustion_issues_release_stock_command_for_reserved_inventory() {
        service.handle(command(MAX_ATTEMPTS));

        assertThat(releaseStockPublisher.commands()).hasSize(1);
        var cmd = releaseStockPublisher.commands().get(0);
        assertThat(cmd.sagaId()).isEqualTo(SAGA_ID);
        assertThat(cmd.stockReservationId()).isEqualTo(STOCK_RES_ID);
        assertThat(cmd.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void credit_timeout_exhaustion_publishes_saga_compensated_event() {
        service.handle(command(MAX_ATTEMPTS));

        assertThat(eventPublisher.compensatedEvents()).hasSize(1);
        var event = eventPublisher.compensatedEvents().get(0);
        assertThat(event.sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void credit_timeout_exhaustion_does_not_publish_credit_timed_out_event() {
        service.handle(command(MAX_ATTEMPTS));

        assertThat(eventPublisher.creditTimedOutEvents()).isEmpty();
    }

    @Test
    void credit_timeout_publishes_exactly_one_event() {
        service.handle(command(1));

        assertThat(eventPublisher.totalEventCount()).isEqualTo(1);
    }

    // ── Wrong state guards ────────────────────────────────────────────────────

    @Test
    void credit_timeout_on_completed_saga_throws() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID).complete());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(command(1)))
                .withMessageContaining("WAITING_FOR_CREDIT");
    }

    @Test
    void credit_timeout_on_waiting_for_inventory_saga_throws() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID).awaitInventoryResponse());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(command(1)))
                .withMessageContaining("WAITING_FOR_CREDIT");
    }

    @Test
    void credit_timeout_on_compensating_saga_throws() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID)
                .awaitInventoryResponse()
                .inventoryReserved(STOCK_RES_ID)
                .awaitCreditResponse()
                .compensate());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(command(1)))
                .withMessageContaining("WAITING_FOR_CREDIT");
    }

    // ── Saga not found ────────────────────────────────────────────────────────

    @Test
    void credit_timeout_with_unknown_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(
                        new HandleCreditTimeoutCommand("unknown-id", CREDIT_RES_ID, 1)))
                .withMessageContaining("No saga found");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleCreditTimeoutCommand("", CREDIT_RES_ID, 1))
                .withMessageContaining("sagaId");
    }

    @Test
    void blank_credit_reservation_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleCreditTimeoutCommand(SAGA_ID, "", 1))
                .withMessageContaining("creditReservationId");
    }

    @Test
    void zero_attempt_number_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleCreditTimeoutCommand(SAGA_ID, CREDIT_RES_ID, 0))
                .withMessageContaining("attemptNumber");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HandleCreditTimeoutCommand command(final int attemptNumber) {
        return new HandleCreditTimeoutCommand(SAGA_ID, CREDIT_RES_ID, attemptNumber);
    }
}
