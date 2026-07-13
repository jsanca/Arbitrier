package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaCommand;
import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaResult;
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
 * Unit tests for {@link CompensateSagaService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class CompensateSagaServiceTest {

    private static final String SAGA_ID = "saga-comp-001";
    private static final String ORDER_ID = "order-comp-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private InMemorySagaRepository repository;
    private InMemoryOutboxRepository outboxRepository;
    private DomainEventToOutboxMapper outboxMapper;
    private CompensateSagaService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        outboxRepository = new InMemoryOutboxRepository();
        outboxMapper = new DomainEventToOutboxMapper(
                new JacksonEventSerializer(new ObjectMapper()),
                FixedTimeProvider.of(Instant.parse("2026-01-15T10:00:00Z")));
        service = new CompensateSagaService(repository, outboxRepository, outboxMapper);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void compensate_returns_result_with_saga_id() {
        saveStarted();

        CompensateSagaResult result = service.compensate(new CompensateSagaCommand(SAGA_ID));

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    @Test
    void compensate_persists_compensating_status() {
        saveStarted();

        service.compensate(new CompensateSagaCommand(SAGA_ID));

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.COMPENSATING);
    }

    @Test
    void compensate_preserves_current_step() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID)
                .advance(SagaStep.RESERVE_INVENTORY));

        service.compensate(new CompensateSagaCommand(SAGA_ID));

        assertThat(repository.getById(SAGA_ID_VO).currentStep()).isEqualTo(SagaStep.RESERVE_INVENTORY);
    }

    @Test
    void compensate_writes_saga_compensated_event_to_outbox() {
        saveStarted();

        service.compensate(new CompensateSagaCommand(SAGA_ID));

        assertThat(outboxRepository.findAll()).hasSize(1);
        var event = outboxRepository.findAll().get(0);
        assertThat(event.eventType()).isEqualTo("SagaCompensatedDomainEvent");
        assertThat(event.aggregateType()).isEqualTo("Saga");
        assertThat(event.aggregateId()).isEqualTo(SAGA_ID);
    }

    @Test
    void compensate_writes_only_one_outbox_event() {
        saveStarted();

        service.compensate(new CompensateSagaCommand(SAGA_ID));

        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    // ── Invalid transitions ───────────────────────────────────────────────────

    @Test
    void compensate_already_compensating_throws() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID).compensate());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.compensate(new CompensateSagaCommand(SAGA_ID)))
                .withMessageContaining("already COMPENSATING");
    }

    @Test
    void compensate_terminal_saga_throws() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID).cancel());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.compensate(new CompensateSagaCommand(SAGA_ID)))
                .withMessageContaining("non-terminal");
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void compensate_non_existent_saga_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.compensate(new CompensateSagaCommand("does-not-exist")))
                .withMessageContaining("does-not-exist");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompensateSagaCommand(""))
                .withMessageContaining("sagaId");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveStarted() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID));
    }
}
