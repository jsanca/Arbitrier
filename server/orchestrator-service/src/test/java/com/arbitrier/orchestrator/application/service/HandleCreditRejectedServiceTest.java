package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditRejectedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditRejectedResult;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private RecordingSagaEventPublisher eventPublisher;
    private RecordingReleaseStockCommandPublisher releaseStockPublisher;
    private HandleCreditRejectedService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        eventPublisher = new RecordingSagaEventPublisher();
        releaseStockPublisher = new RecordingReleaseStockCommandPublisher();
        service = new HandleCreditRejectedService(repository, eventPublisher, releaseStockPublisher);

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
    void handle_publishes_saga_compensated_event() {
        service.handle(command());

        assertThat(eventPublisher.compensatedEvents()).hasSize(1);
        var event = eventPublisher.compensatedEvents().get(0);
        assertThat(event.sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
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
    void handle_does_not_publish_release_credit_command() {
        service.handle(command());

        assertThat(eventPublisher.cancelledEvents()).isEmpty();
        assertThat(eventPublisher.completedEvents()).isEmpty();
        assertThat(eventPublisher.totalEventCount()).isEqualTo(1);
    }

    @Test
    void handle_publishes_only_compensated_event() {
        service.handle(command());

        assertThat(eventPublisher.compensatedEvents()).hasSize(1);
        assertThat(eventPublisher.startedEvents()).isEmpty();
        assertThat(eventPublisher.advancedEvents()).isEmpty();
        assertThat(eventPublisher.cancelledEvents()).isEmpty();
        assertThat(eventPublisher.completedEvents()).isEmpty();
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
