package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.StartSagaCommand;
import com.arbitrier.orchestrator.application.port.inbound.StartSagaResult;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link StartSagaService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class StartSagaServiceTest {

    private static final String SAGA_ID = "saga-001";
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private InMemorySagaRepository repository;
    private RecordingSagaEventPublisher publisher;
    private StartSagaService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        publisher = new RecordingSagaEventPublisher();
        service = new StartSagaService(repository, publisher);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void start_returns_result_with_saga_id() {
        StartSagaResult result = service.start(command());

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    @Test
    void start_persists_saga_with_started_status() {
        service.start(command());

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.STARTED);
    }

    @Test
    void start_persists_saga_with_order_created_step() {
        service.start(command());

        assertThat(repository.getById(SAGA_ID_VO).currentStep()).isEqualTo(SagaStep.ORDER_CREATED);
    }

    @Test
    void start_persists_saga_with_correct_order_and_customer_ids() {
        service.start(command());

        assertThat(repository.getById(SAGA_ID_VO).orderId()).isEqualTo(ORDER_ID);
        assertThat(repository.getById(SAGA_ID_VO).customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void start_publishes_saga_started_event() {
        service.start(command());

        assertThat(publisher.startedEvents()).hasSize(1);
        assertThat(publisher.startedEvents().get(0).sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(publisher.startedEvents().get(0).orderId()).isEqualTo(ORDER_ID);
        assertThat(publisher.startedEvents().get(0).customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void start_publishes_only_started_event() {
        service.start(command());

        assertThat(publisher.startedEvents()).hasSize(1);
        assertThat(publisher.advancedEvents()).isEmpty();
        assertThat(publisher.compensatedEvents()).isEmpty();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StartSagaCommand("", ORDER_ID, CUSTOMER_ID))
                .withMessageContaining("sagaId");
    }

    @Test
    void blank_order_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StartSagaCommand(SAGA_ID, "", CUSTOMER_ID))
                .withMessageContaining("orderId");
    }

    @Test
    void blank_customer_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StartSagaCommand(SAGA_ID, ORDER_ID, ""))
                .withMessageContaining("customerId");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StartSagaCommand command() {
        return new StartSagaCommand(SAGA_ID, ORDER_ID, CUSTOMER_ID);
    }
}
