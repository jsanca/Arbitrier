package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.AdvanceSagaCommand;
import com.arbitrier.orchestrator.application.port.inbound.AdvanceSagaResult;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link AdvanceSagaService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class AdvanceSagaServiceTest {

    private static final String SAGA_ID = "saga-advance-001";
    private static final String ORDER_ID = "order-advance-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private InMemorySagaRepository repository;
    private RecordingSagaEventPublisher publisher;
    private AdvanceSagaService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        publisher = new RecordingSagaEventPublisher();
        service = new AdvanceSagaService(repository, publisher);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void advance_returns_result_with_saga_id_and_new_step() {
        saveStarted();

        AdvanceSagaResult result = service.advance(command(SagaStep.RESERVE_INVENTORY));

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(result.currentStep()).isEqualTo(SagaStep.RESERVE_INVENTORY);
    }

    @Test
    void advance_persists_updated_step() {
        saveStarted();

        service.advance(command(SagaStep.RESERVE_INVENTORY));

        assertThat(repository.getById(SAGA_ID_VO).currentStep()).isEqualTo(SagaStep.RESERVE_INVENTORY);
    }

    @Test
    void advance_does_not_change_status() {
        saveStarted();

        service.advance(command(SagaStep.RESERVE_INVENTORY));

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.STARTED);
    }

    @Test
    void advance_publishes_advanced_event_with_correct_fields() {
        saveStarted();

        service.advance(command(SagaStep.VALIDATE_CREDIT));

        assertThat(publisher.advancedEvents()).hasSize(1);
        assertThat(publisher.advancedEvents().get(0).sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(publisher.advancedEvents().get(0).orderId()).isEqualTo(ORDER_ID);
        assertThat(publisher.advancedEvents().get(0).currentStep()).isEqualTo(SagaStep.VALIDATE_CREDIT);
    }

    @Test
    void advance_publishes_only_advanced_event() {
        saveStarted();

        service.advance(command(SagaStep.RESERVE_INVENTORY));

        assertThat(publisher.advancedEvents()).hasSize(1);
        assertThat(publisher.startedEvents()).isEmpty();
        assertThat(publisher.compensatedEvents()).isEmpty();
    }

    // ── Invalid transitions ───────────────────────────────────────────────────

    @Test
    void advance_on_terminal_saga_throws() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID).complete());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.advance(command(SagaStep.RESERVE_INVENTORY)))
                .withMessageContaining("non-terminal");
    }

    @Test
    void advance_on_compensating_saga_throws() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID).compensate());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.advance(command(SagaStep.RESERVE_INVENTORY)))
                .withMessageContaining("COMPENSATING");
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void advance_non_existent_saga_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.advance(command(SagaStep.RESERVE_INVENTORY)))
                .withMessageContaining(SAGA_ID);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AdvanceSagaCommand("", SagaStep.RESERVE_INVENTORY))
                .withMessageContaining("sagaId");
    }

    @Test
    void null_next_step_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AdvanceSagaCommand(SAGA_ID, null))
                .withMessageContaining("nextStep");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveStarted() {
        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID));
    }

    private AdvanceSagaCommand command(SagaStep step) {
        return new AdvanceSagaCommand(SAGA_ID, step);
    }
}
