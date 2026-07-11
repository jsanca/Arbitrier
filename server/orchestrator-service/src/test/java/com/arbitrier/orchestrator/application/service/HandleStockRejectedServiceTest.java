package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedResult;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private RecordingSagaEventPublisher eventPublisher;
    private HandleStockRejectedService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        eventPublisher = new RecordingSagaEventPublisher();
        service = new HandleStockRejectedService(repository, eventPublisher);

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
    void handle_publishes_saga_cancelled_event() {
        service.handle(command());

        assertThat(eventPublisher.cancelledEvents()).hasSize(1);
        var event = eventPublisher.cancelledEvents().get(0);
        assertThat(event.sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void handle_emits_no_release_commands() {
        service.handle(command());

        assertThat(eventPublisher.compensatedEvents()).isEmpty();
        assertThat(eventPublisher.totalEventCount()).isEqualTo(1);
    }

    @Test
    void handle_publishes_only_cancelled_event() {
        service.handle(command());

        assertThat(eventPublisher.cancelledEvents()).hasSize(1);
        assertThat(eventPublisher.startedEvents()).isEmpty();
        assertThat(eventPublisher.advancedEvents()).isEmpty();
        assertThat(eventPublisher.completedEvents()).isEmpty();
        assertThat(eventPublisher.compensatedEvents()).isEmpty();
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
