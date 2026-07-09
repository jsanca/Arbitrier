package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReserveStockCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.HandleOrderCreatedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleOrderCreatedResult;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link HandleOrderCreatedService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class HandleOrderCreatedServiceTest {

    private static final String SAGA_ID = "saga-001";
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private InMemorySagaRepository repository;
    private RecordingSagaEventPublisher eventPublisher;
    private RecordingReserveStockCommandPublisher stockCommandPublisher;
    private HandleOrderCreatedService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        eventPublisher = new RecordingSagaEventPublisher();
        stockCommandPublisher = new RecordingReserveStockCommandPublisher();
        service = new HandleOrderCreatedService(repository, eventPublisher, stockCommandPublisher);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void handle_returns_result_with_saga_id() {
        HandleOrderCreatedResult result = service.handle(command());

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    @Test
    void handle_returns_result_with_non_blank_stock_reservation_id() {
        HandleOrderCreatedResult result = service.handle(command());

        assertThat(result.stockReservationId()).isNotBlank();
    }

    @Test
    void handle_persists_saga_with_started_status_and_order_created_step() {
        service.handle(command());

        var saga = repository.getById(SAGA_ID_VO);
        assertThat(saga.status()).isEqualTo(SagaStatus.STARTED);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.ORDER_CREATED);
    }

    @Test
    void handle_persists_saga_with_correct_ids() {
        service.handle(command());

        var saga = repository.getById(SAGA_ID_VO);
        assertThat(saga.orderId()).isEqualTo(ORDER_ID);
        assertThat(saga.customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void handle_publishes_saga_started_event() {
        service.handle(command());

        assertThat(eventPublisher.startedEvents()).hasSize(1);
        var event = eventPublisher.startedEvents().get(0);
        assertThat(event.sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void handle_publishes_reserve_stock_command() {
        HandleOrderCreatedResult result = service.handle(command());

        assertThat(stockCommandPublisher.commands()).hasSize(1);
        var cmd = stockCommandPublisher.commands().get(0);
        assertThat(cmd.sagaId()).isEqualTo(SAGA_ID);
        assertThat(cmd.orderId()).isEqualTo(ORDER_ID);
        assertThat(cmd.stockReservationId()).isEqualTo(result.stockReservationId());
    }

    @Test
    void handle_publishes_only_started_event_no_advance_no_completed() {
        service.handle(command());

        assertThat(eventPublisher.startedEvents()).hasSize(1);
        assertThat(eventPublisher.advancedEvents()).isEmpty();
        assertThat(eventPublisher.completedEvents()).isEmpty();
        assertThat(eventPublisher.compensatedEvents()).isEmpty();
    }

    @Test
    void handle_generates_unique_stock_reservation_ids_for_each_call() {
        var result1 = service.handle(command());
        var result2 = service.handle(new HandleOrderCreatedCommand("saga-002", ORDER_ID, CUSTOMER_ID));

        assertThat(result1.stockReservationId()).isNotEqualTo(result2.stockReservationId());
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleOrderCreatedCommand("", ORDER_ID, CUSTOMER_ID))
                .withMessageContaining("sagaId");
    }

    @Test
    void blank_order_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleOrderCreatedCommand(SAGA_ID, "", CUSTOMER_ID))
                .withMessageContaining("orderId");
    }

    @Test
    void blank_customer_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleOrderCreatedCommand(SAGA_ID, ORDER_ID, ""))
                .withMessageContaining("customerId");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HandleOrderCreatedCommand command() {
        return new HandleOrderCreatedCommand(SAGA_ID, ORDER_ID, CUSTOMER_ID);
    }
}
