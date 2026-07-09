package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReservedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReservedResult;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link HandleStockReservedService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class HandleStockReservedServiceTest {

    private static final String SAGA_ID = "saga-001";
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final String STOCK_RESERVATION_ID = "stock-res-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private InMemorySagaRepository repository;
    private RecordingSagaEventPublisher eventPublisher;
    private RecordingReserveCreditCommandPublisher creditCommandPublisher;
    private HandleStockReservedService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        eventPublisher = new RecordingSagaEventPublisher();
        creditCommandPublisher = new RecordingReserveCreditCommandPublisher();
        service = new HandleStockReservedService(repository, eventPublisher, creditCommandPublisher);

        repository.save(Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void handle_returns_result_with_saga_id() {
        HandleStockReservedResult result = service.handle(command());

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    @Test
    void handle_returns_result_with_non_blank_credit_reservation_id() {
        HandleStockReservedResult result = service.handle(command());

        assertThat(result.creditReservationId()).isNotBlank();
    }

    @Test
    void handle_advances_saga_to_validate_credit_step() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).currentStep()).isEqualTo(SagaStep.VALIDATE_CREDIT);
    }

    @Test
    void handle_persists_stock_reservation_id_on_saga() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).stockReservationId()).isEqualTo(STOCK_RESERVATION_ID);
    }

    @Test
    void handle_publishes_saga_advanced_event() {
        service.handle(command());

        assertThat(eventPublisher.advancedEvents()).hasSize(1);
        var event = eventPublisher.advancedEvents().get(0);
        assertThat(event.sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.currentStep()).isEqualTo(SagaStep.VALIDATE_CREDIT);
    }

    @Test
    void handle_publishes_reserve_credit_command() {
        HandleStockReservedResult result = service.handle(command());

        assertThat(creditCommandPublisher.commands()).hasSize(1);
        var cmd = creditCommandPublisher.commands().get(0);
        assertThat(cmd.sagaId()).isEqualTo(SAGA_ID);
        assertThat(cmd.orderId()).isEqualTo(ORDER_ID);
        assertThat(cmd.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(cmd.creditReservationId()).isEqualTo(result.creditReservationId());
    }

    @Test
    void handle_publishes_only_advanced_event_no_started_no_completed() {
        service.handle(command());

        assertThat(eventPublisher.advancedEvents()).hasSize(1);
        assertThat(eventPublisher.startedEvents()).isEmpty();
        assertThat(eventPublisher.completedEvents()).isEmpty();
        assertThat(eventPublisher.compensatedEvents()).isEmpty();
    }

    @Test
    void handle_generates_unique_credit_reservation_ids_for_each_call() {
        var result1 = service.handle(command());

        repository.save(Saga.start(SagaId.of("saga-002"), ORDER_ID, CUSTOMER_ID));
        var result2 = service.handle(new HandleStockReservedCommand("saga-002", STOCK_RESERVATION_ID));

        assertThat(result1.creditReservationId()).isNotEqualTo(result2.creditReservationId());
    }

    // ── Saga not found ────────────────────────────────────────────────────────

    @Test
    void handle_with_unknown_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(
                        new HandleStockReservedCommand("unknown-id", STOCK_RESERVATION_ID)))
                .withMessageContaining("No saga found");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleStockReservedCommand("", STOCK_RESERVATION_ID))
                .withMessageContaining("sagaId");
    }

    @Test
    void blank_stock_reservation_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleStockReservedCommand(SAGA_ID, ""))
                .withMessageContaining("stockReservationId");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HandleStockReservedCommand command() {
        return new HandleStockReservedCommand(SAGA_ID, STOCK_RESERVATION_ID);
    }
}
