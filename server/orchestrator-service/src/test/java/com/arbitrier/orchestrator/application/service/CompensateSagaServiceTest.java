package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaCommand;
import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaResult;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private RecordingSagaEventPublisher publisher;
    private CompensateSagaService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        publisher = new RecordingSagaEventPublisher();
        service = new CompensateSagaService(repository, publisher);
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
    void compensate_publishes_compensated_event() {
        saveStarted();

        service.compensate(new CompensateSagaCommand(SAGA_ID));

        assertThat(publisher.compensatedEvents()).hasSize(1);
        assertThat(publisher.compensatedEvents().get(0).sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(publisher.compensatedEvents().get(0).orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void compensate_publishes_only_compensated_event() {
        saveStarted();

        service.compensate(new CompensateSagaCommand(SAGA_ID));

        assertThat(publisher.compensatedEvents()).hasSize(1);
        assertThat(publisher.startedEvents()).isEmpty();
        assertThat(publisher.advancedEvents()).isEmpty();
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
