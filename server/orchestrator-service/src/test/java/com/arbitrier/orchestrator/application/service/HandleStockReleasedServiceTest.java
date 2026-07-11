package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReleasedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReleasedResult;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link HandleStockReleasedService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class HandleStockReleasedServiceTest {

    private static final String SAGA_ID = "saga-001";
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final String STOCK_RESERVATION_ID = "stock-res-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private InMemorySagaRepository repository;
    private RecordingSagaEventPublisher eventPublisher;
    private HandleStockReleasedService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        eventPublisher = new RecordingSagaEventPublisher();
        service = new HandleStockReleasedService(repository, eventPublisher);

        repository.save(
                Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID)
                        .inventoryReserved(STOCK_RESERVATION_ID)
                        .creditRejected());
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void handle_returns_result_with_saga_id() {
        HandleStockReleasedResult result = service.handle(command());

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    @Test
    void handle_cancels_saga_after_compensation() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.CANCELLED);
    }

    @Test
    void handle_preserves_compensate_inventory_step() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).currentStep()).isEqualTo(SagaStep.COMPENSATE_INVENTORY);
    }

    @Test
    void handle_preserves_stock_reservation_id() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).stockReservationId()).isEqualTo(STOCK_RESERVATION_ID);
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
    void handle_publishes_only_cancelled_event() {
        service.handle(command());

        assertThat(eventPublisher.cancelledEvents()).hasSize(1);
        assertThat(eventPublisher.startedEvents()).isEmpty();
        assertThat(eventPublisher.advancedEvents()).isEmpty();
        assertThat(eventPublisher.completedEvents()).isEmpty();
        assertThat(eventPublisher.compensatedEvents()).isEmpty();
    }

    // ── Guard: must be COMPENSATING ───────────────────────────────────────────

    @Test
    void handle_on_non_compensating_saga_throws() {
        repository.save(Saga.start(SagaId.of("saga-fresh"), ORDER_ID, CUSTOMER_ID));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(new HandleStockReleasedCommand("saga-fresh")))
                .withMessageContaining("COMPENSATING");
    }

    // ── Saga not found ────────────────────────────────────────────────────────

    @Test
    void handle_with_unknown_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(new HandleStockReleasedCommand("unknown-id")))
                .withMessageContaining("No saga found");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleStockReleasedCommand(""))
                .withMessageContaining("sagaId");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HandleStockReleasedCommand command() {
        return new HandleStockReleasedCommand(SAGA_ID);
    }
}
