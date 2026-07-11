package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditApprovedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditApprovedResult;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link HandleCreditApprovedService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class HandleCreditApprovedServiceTest {

    private static final String SAGA_ID = "saga-001";
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final String STOCK_RESERVATION_ID = "stock-res-001";
    private static final String CREDIT_RESERVATION_ID = "credit-res-001";
    private static final SagaId SAGA_ID_VO = SagaId.of(SAGA_ID);

    private InMemorySagaRepository repository;
    private RecordingSagaEventPublisher eventPublisher;
    private RecordingConfirmOrderCommandPublisher confirmOrderPublisher;
    private HandleCreditApprovedService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySagaRepository();
        eventPublisher = new RecordingSagaEventPublisher();
        confirmOrderPublisher = new RecordingConfirmOrderCommandPublisher();
        service = new HandleCreditApprovedService(repository, eventPublisher, confirmOrderPublisher);

        repository.save(
                Saga.start(SAGA_ID_VO, ORDER_ID, CUSTOMER_ID)
                        .inventoryReserved(STOCK_RESERVATION_ID));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void handle_returns_result_with_saga_id() {
        HandleCreditApprovedResult result = service.handle(command());

        assertThat(result.sagaId()).isEqualTo(SAGA_ID_VO);
    }

    @Test
    void handle_transitions_saga_to_completed() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).status()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(repository.getById(SAGA_ID_VO).currentStep()).isEqualTo(SagaStep.COMPLETE_ORDER);
    }

    @Test
    void handle_persists_credit_reservation_id_on_saga() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).creditReservationId()).isEqualTo(CREDIT_RESERVATION_ID);
    }

    @Test
    void handle_preserves_stock_reservation_id_on_saga() {
        service.handle(command());

        assertThat(repository.getById(SAGA_ID_VO).stockReservationId()).isEqualTo(STOCK_RESERVATION_ID);
    }

    @Test
    void handle_publishes_saga_completed_event() {
        service.handle(command());

        assertThat(eventPublisher.completedEvents()).hasSize(1);
        var event = eventPublisher.completedEvents().get(0);
        assertThat(event.sagaId()).isEqualTo(SAGA_ID_VO);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void handle_publishes_confirm_order_command() {
        service.handle(command());

        assertThat(confirmOrderPublisher.commands()).hasSize(1);
        var cmd = confirmOrderPublisher.commands().get(0);
        assertThat(cmd.sagaId()).isEqualTo(SAGA_ID);
        assertThat(cmd.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void handle_publishes_only_completed_event_no_started_no_advanced() {
        service.handle(command());

        assertThat(eventPublisher.completedEvents()).hasSize(1);
        assertThat(eventPublisher.startedEvents()).isEmpty();
        assertThat(eventPublisher.advancedEvents()).isEmpty();
        assertThat(eventPublisher.compensatedEvents()).isEmpty();
    }

    // ── Out-of-sequence guard ─────────────────────────────────────────────────

    @Test
    void credit_approved_before_stock_reserved_throws_with_clear_message() {
        repository.save(Saga.start(SagaId.of("saga-fresh"), ORDER_ID, CUSTOMER_ID));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(
                        new HandleCreditApprovedCommand("saga-fresh", CREDIT_RESERVATION_ID)))
                .withMessageContaining("stock reservation")
                .withMessageContaining("saga-fresh");
    }

    // ── Saga not found ────────────────────────────────────────────────────────

    @Test
    void handle_with_unknown_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.handle(
                        new HandleCreditApprovedCommand("unknown-id", CREDIT_RESERVATION_ID)))
                .withMessageContaining("No saga found");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_saga_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleCreditApprovedCommand("", CREDIT_RESERVATION_ID))
                .withMessageContaining("sagaId");
    }

    @Test
    void blank_credit_reservation_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new HandleCreditApprovedCommand(SAGA_ID, ""))
                .withMessageContaining("creditReservationId");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HandleCreditApprovedCommand command() {
        return new HandleCreditApprovedCommand(SAGA_ID, CREDIT_RESERVATION_ID);
    }
}
