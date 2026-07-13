package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedResult;
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
 * Unit tests for {@link HandleStockRejectedService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class HandleStockRejectedServiceTest {

    private static final String SAGA_ID = "saga-001";
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private InMemorySagaRepository repository;
    private InMemoryOutboxRepository outboxRepository;
    private DomainEventToOutboxMapper outboxMapper;
    private HandleStockRejectedService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        outboxRepository = new InMemoryOutboxRepository();
        outboxMapper = new DomainEventToOutboxMapper(
                new JacksonEventSerializer(new ObjectMapper()),
                FixedTimeProvider.of(Instant.parse("2026-01-15T10:00:00Z")));
        service = new HandleStockRejectedService(repository, outboxRepository, outboxMapper);

        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void handle_returns_result_with_saga_id() {
        HandleStockRejectedResult result = service.handle(command());

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    @Test
    void handle_cancels_saga() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.CANCELLED);
    }

    @Test
    void handle_writes_saga_cancelled_event_to_outbox() {
        service.handle(command());

        assertThat(outboxRepository.findAll()).hasSize(1);
        var event = outboxRepository.findAll().get(0);
        assertThat(event.eventType()).isEqualTo("SagaCancelledDomainEvent");
        assertThat(event.aggregateType()).isEqualTo("Saga");
        assertThat(event.aggregateId()).isEqualTo(SAGA_ID);
    }

    @Test
    void handle_writes_only_one_outbox_event() {
        service.handle(command());

        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    // ── Saga not found ────────────────────────────────────────────────────────

    @Test
    void handle_with_unknown_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(new HandleStockRejectedCommand("unknown-id")))
                .withMessageContaining("No saga found");
    }

    // ── Terminal saga guard ───────────────────────────────────────────────────

    @Test
    void handle_on_already_cancelled_saga_throws() {
        service.handle(command());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(command()))
                .withMessageContaining("non-terminal");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleStockRejectedCommand(""))
                .withMessageContaining("sagaId");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HandleStockRejectedCommand command() {
        return new HandleStockRejectedCommand(SAGA_ID);
    }
}
