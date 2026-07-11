package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.HandleCompensationFailedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCompensationFailedResult;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private RecordingSagaEventPublisher eventPublisher;
    private HandleCompensationFailedService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        eventPublisher = new RecordingSagaEventPublisher();
        service = new HandleCompensationFailedService(repository, eventPublisher);

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
    void handle_publishes_compensation_failed_event() {
        service.handle(command());

        assertThat(eventPublisher.compensationFailedEvents()).hasSize(1);
        var event = eventPublisher.compensationFailedEvents().get(0);
        assertThat(event.sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void handle_publishes_only_compensation_failed_event() {
        service.handle(command());

        assertThat(eventPublisher.compensationFailedEvents()).hasSize(1);
        assertThat(eventPublisher.startedEvents()).isEmpty();
        assertThat(eventPublisher.advancedEvents()).isEmpty();
        assertThat(eventPublisher.completedEvents()).isEmpty();
        assertThat(eventPublisher.cancelledEvents()).isEmpty();
        assertThat(eventPublisher.compensatedEvents()).isEmpty();
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
