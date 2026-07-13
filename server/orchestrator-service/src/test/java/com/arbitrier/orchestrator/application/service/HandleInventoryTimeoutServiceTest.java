package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.inbound.HandleInventoryTimeoutCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleInventoryTimeoutResult;
import com.arbitrier.orchestrator.domain.model.CorporateBulkOrderSagaRetryPolicy;
import com.arbitrier.orchestrator.domain.model.RetryDecision;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.messaging.serialization.JacksonEventSerializer;
import com.arbitrier.platform.messaging.test.InMemoryOutboxRepository;
import com.arbitrier.platform.time.FixedTimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link HandleInventoryTimeoutService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class HandleInventoryTimeoutServiceTest {

    private static final String SAGA_ID = "saga-001";
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final String STOCK_RES_ID = "stock-res-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private static final int MAX_ATTEMPTS = 3;
    private static final CorporateBulkOrderSagaRetryPolicy POLICY =
            new CorporateBulkOrderSagaRetryPolicy(MAX_ATTEMPTS, MAX_ATTEMPTS);

    private InMemorySagaRepository repository;
    private InMemoryOutboxRepository outboxRepository;
    private DomainEventToOutboxMapper outboxMapper;
    private RecordingReleaseStockCommandPublisher releaseStockPublisher;
    private HandleInventoryTimeoutService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        outboxRepository = new InMemoryOutboxRepository();
        outboxMapper = new DomainEventToOutboxMapper(
                new JacksonEventSerializer(new ObjectMapper()),
                FixedTimeProvider.of(Instant.parse("2026-01-15T10:00:00Z")));
        releaseStockPublisher = new RecordingReleaseStockCommandPublisher();
        service = new HandleInventoryTimeoutService(
                repository, outboxRepository, outboxMapper, releaseStockPublisher, POLICY);

        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID).awaitInventoryResponse());
    }

    // ── Retry path ────────────────────────────────────────────────────────────

    @Test
    void inventory_timeout_within_limit_returns_retry_decision() {
        HandleInventoryTimeoutResult result = service.handle(command(1));

        assertThat(result.decision()).isEqualTo(RetryDecision.RETRY);
    }

    @Test
    void inventory_timeout_within_limit_saga_remains_waiting_for_inventory() {
        service.handle(command(1));

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.WAITING_FOR_INVENTORY);
    }

    @Test
    void inventory_timeout_within_limit_writes_inventory_timed_out_event_to_outbox() {
        service.handle(command(1));

        assertThat(outboxRepository.findAll()).hasSize(1);
        var event = outboxRepository.findAll().get(0);
        assertThat(event.eventType()).isEqualTo("InventoryTimedOutDomainEvent");
        assertThat(event.aggregateType()).isEqualTo("Saga");
        assertThat(event.aggregateId()).isEqualTo(SAGA_ID);
    }

    @Test
    void inventory_timeout_retry_does_not_issue_release_stock_command() {
        service.handle(command(1));

        assertThat(releaseStockPublisher.commands()).isEmpty();
    }

    @Test
    void inventory_timeout_result_contains_saga_id() {
        HandleInventoryTimeoutResult result = service.handle(command(1));

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    // ── Exhaust path: compensation ────────────────────────────────────────────

    @Test
    void inventory_timeout_exhaustion_returns_exhaust_decision() {
        HandleInventoryTimeoutResult result = service.handle(command(MAX_ATTEMPTS));

        assertThat(result.decision()).isEqualTo(RetryDecision.EXHAUST);
    }

    @Test
    void inventory_timeout_exhaustion_transitions_saga_to_compensating() {
        service.handle(command(MAX_ATTEMPTS));

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.COMPENSATING);
    }

    @Test
    void inventory_timeout_exhaustion_issues_release_stock_command_idempotently() {
        service.handle(command(MAX_ATTEMPTS));

        assertThat(releaseStockPublisher.commands()).hasSize(1);
        var cmd = releaseStockPublisher.commands().get(0);
        assertThat(cmd.sagaId()).isEqualTo(SAGA_ID);
        assertThat(cmd.stockReservationId()).isEqualTo(STOCK_RES_ID);
        assertThat(cmd.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void inventory_timeout_exhaustion_writes_saga_compensated_event_to_outbox() {
        service.handle(command(MAX_ATTEMPTS));

        assertThat(outboxRepository.findAll())
                .anyMatch(e -> e.eventType().equals("SagaCompensatedDomainEvent"));
    }

    @Test
    void inventory_timeout_exhaustion_does_not_write_inventory_timed_out_event() {
        service.handle(command(MAX_ATTEMPTS));

        assertThat(outboxRepository.findAll())
                .noneMatch(e -> e.eventType().equals("InventoryTimedOutDomainEvent"));
    }

    @Test
    void inventory_timeout_retry_writes_exactly_one_outbox_event() {
        service.handle(command(1));

        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    // ── Wrong state guards ────────────────────────────────────────────────────

    @Test
    void inventory_timeout_on_completed_saga_throws() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID).complete());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(command(1)))
                .withMessageContaining("WAITING_FOR_INVENTORY");
    }

    @Test
    void inventory_timeout_on_started_saga_throws() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(command(1)))
                .withMessageContaining("WAITING_FOR_INVENTORY");
    }

    @Test
    void inventory_timeout_on_compensating_saga_throws() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID)
                .awaitInventoryResponse()
                .compensate());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(command(1)))
                .withMessageContaining("WAITING_FOR_INVENTORY");
    }

    // ── Saga not found ────────────────────────────────────────────────────────

    @Test
    void inventory_timeout_with_unknown_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(
                        new HandleInventoryTimeoutCommand("unknown-id", STOCK_RES_ID, 1)))
                .withMessageContaining("No saga found");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleInventoryTimeoutCommand("", STOCK_RES_ID, 1))
                .withMessageContaining("sagaId");
    }

    @Test
    void blank_stock_reservation_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleInventoryTimeoutCommand(SAGA_ID, "", 1))
                .withMessageContaining("stockReservationId");
    }

    @Test
    void zero_attempt_number_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleInventoryTimeoutCommand(SAGA_ID, STOCK_RES_ID, 0))
                .withMessageContaining("attemptNumber");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HandleInventoryTimeoutCommand command(final int attemptNumber) {
        return new HandleInventoryTimeoutCommand(SAGA_ID, STOCK_RES_ID, attemptNumber);
    }
}
