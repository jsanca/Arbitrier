package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.application.port.inbound.HandleCompensationFailedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCompensationFailedResult;
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
 * Unit tests for {@link HandleCompensationFailedService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class HandleCompensationFailedServiceTest {

    private static final String SAGA_ID = "saga-001";
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final String STOCK_RESERVATION_ID = "stock-res-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private InMemorySagaRepository repository;
    private InMemoryOutboxRepository outboxRepository;
    private DomainEventToOutboxMapper outboxMapper;
    private HandleCompensationFailedService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        outboxRepository = new InMemoryOutboxRepository();
        outboxMapper = new DomainEventToOutboxMapper(
                new JacksonEventSerializer(new ObjectMapper()),
                FixedTimeProvider.of(Instant.parse("2026-01-15T10:00:00Z")));
        service = new HandleCompensationFailedService(repository, outboxRepository, outboxMapper);

        repository.save(
                Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID)
                        .inventoryReserved(STOCK_RESERVATION_ID)
                        .creditRejected());
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void handle_returns_result_with_saga_id() {
        HandleCompensationFailedResult result = service.handle(command());

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    @Test
    void handle_transitions_saga_to_failed_compensation() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.FAILED_COMPENSATION);
    }

    @Test
    void handle_writes_compensation_failed_event_to_outbox() {
        service.handle(command());

        assertThat(outboxRepository.findAll()).hasSize(1);
        var event = outboxRepository.findAll().get(0);
        assertThat(event.eventType()).isEqualTo("SagaCompensationFailedDomainEvent");
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
                .isThrownBy(() -> service.handle(new HandleCompensationFailedCommand("unknown-id")))
                .withMessageContaining("No saga found");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleCompensationFailedCommand(""))
                .withMessageContaining("sagaId");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HandleCompensationFailedCommand command() {
        return new HandleCompensationFailedCommand(SAGA_ID);
    }
}
