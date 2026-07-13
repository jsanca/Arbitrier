package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditRejectedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditRejectedResult;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
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
 * Unit tests for {@link HandleCreditRejectedService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class HandleCreditRejectedServiceTest {

    private static final String SAGA_ID = "saga-001";
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final String STOCK_RESERVATION_ID = "stock-res-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private InMemorySagaRepository repository;
    private InMemoryOutboxRepository outboxRepository;
    private DomainEventToOutboxMapper outboxMapper;
    private RecordingReleaseStockCommandPublisher releaseStockPublisher;
    private HandleCreditRejectedService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        outboxRepository = new InMemoryOutboxRepository();
        outboxMapper = new DomainEventToOutboxMapper(
                new JacksonEventSerializer(new ObjectMapper()),
                FixedTimeProvider.of(Instant.parse("2026-01-15T10:00:00Z")));
        releaseStockPublisher = new RecordingReleaseStockCommandPublisher();
        service = new HandleCreditRejectedService(repository, outboxRepository, outboxMapper, releaseStockPublisher);

        repository.save(
                Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID)
                        .inventoryReserved(STOCK_RESERVATION_ID));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void handle_returns_result_with_saga_id() {
        HandleCreditRejectedResult result = service.handle(command());

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    @Test
    void handle_transitions_saga_to_compensating() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.COMPENSATING);
    }

    @Test
    void handle_advances_saga_to_compensate_inventory_step() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).currentStep()).isEqualTo(SagaStep.COMPENSATE_INVENTORY);
    }

    @Test
    void handle_writes_saga_compensated_event_to_outbox() {
        service.handle(command());

        assertThat(outboxRepository.findAll()).hasSize(1);
        var event = outboxRepository.findAll().get(0);
        assertThat(event.eventType()).isEqualTo("SagaCompensatedDomainEvent");
        assertThat(event.aggregateType()).isEqualTo("Saga");
        assertThat(event.aggregateId()).isEqualTo(SAGA_ID);
    }

    @Test
    void handle_publishes_release_stock_command_with_stored_reservation_id() {
        service.handle(command());

        assertThat(releaseStockPublisher.commands()).hasSize(1);
        var cmd = releaseStockPublisher.commands().get(0);
        assertThat(cmd.sagaId()).isEqualTo(SAGA_ID);
        assertThat(cmd.orderId()).isEqualTo(ORDER_ID);
        assertThat(cmd.stockReservationId()).isEqualTo(STOCK_RESERVATION_ID);
    }

    @Test
    void handle_writes_only_one_outbox_event() {
        service.handle(command());

        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    // ── Missing stock reservation guard ───────────────────────────────────────

    @Test
    void credit_rejected_without_stock_reservation_throws() {
        repository.save(Saga.start(SagaId.of("saga-fresh"), ORDER_ID, CUSTOMER_ID));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(new HandleCreditRejectedCommand("saga-fresh")))
                .withMessageContaining("stock reservation");
    }

    // ── Saga not found ────────────────────────────────────────────────────────

    @Test
    void handle_with_unknown_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(new HandleCreditRejectedCommand("unknown-id")))
                .withMessageContaining("No saga found");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleCreditRejectedCommand(""))
                .withMessageContaining("sagaId");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HandleCreditRejectedCommand command() {
        return new HandleCreditRejectedCommand(SAGA_ID);
    }
}
