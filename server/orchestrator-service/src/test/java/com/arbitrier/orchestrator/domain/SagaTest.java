package com.arbitrier.orchestrator.domain;

import com.arbitrier.orchestrator.domain.model.CustomerDecision;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for the {@link Saga} aggregate root.
 */
class SagaTest {

    private static final SagaId SAGA_ID = SagaId.of("saga-001");
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final String STOCK_RESERVATION_ID = "stock-res-001";
    private static final String CREDIT_RESERVATION_ID = "credit-res-001";

    @Test
    void start_sets_started_status_and_order_created_step() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        assertThat(saga.id()).isEqualTo(SAGA_ID);
        assertThat(saga.orderId()).isEqualTo(ORDER_ID);
        assertThat(saga.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(saga.status()).isEqualTo(SagaStatus.STARTED);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.ORDER_CREATED);
        assertThat(saga.customerDecision()).isNull();
        assertThat(saga.stockReservationId()).isNull();
        assertThat(saga.creditReservationId()).isNull();
    }

    @Test
    void await_customer_decision_transitions_to_awaiting_customer_decision() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).awaitCustomerDecision();

        assertThat(saga.status()).isEqualTo(SagaStatus.AWAITING_CUSTOMER_DECISION);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.AWAIT_CUSTOMER_DECISION);
    }

    @Test
    void apply_customer_decision_from_started_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> saga.applyCustomerDecision(CustomerDecision.ACCEPT_PARTIAL))
                .withMessageContaining("AWAITING_CUSTOMER_DECISION");
    }

    @Test
    void apply_customer_decision_accept_partial_resumes_with_started_and_validate_credit_step() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .awaitCustomerDecision()
                .applyCustomerDecision(CustomerDecision.ACCEPT_PARTIAL);

        assertThat(saga.status()).isEqualTo(SagaStatus.STARTED);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.VALIDATE_CREDIT);
        assertThat(saga.customerDecision()).isEqualTo(CustomerDecision.ACCEPT_PARTIAL);
    }

    @Test
    void apply_customer_decision_null_throws_null_pointer_exception() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).awaitCustomerDecision();

        assertThatNullPointerException()
                .isThrownBy(() -> saga.applyCustomerDecision(null));
    }

    // ── inventoryReserved ─────────────────────────────────────────────────────

    @Test
    void inventory_reserved_stores_stock_reservation_id_and_advances_to_validate_credit() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .inventoryReserved(STOCK_RESERVATION_ID);

        assertThat(saga.stockReservationId()).isEqualTo(STOCK_RESERVATION_ID);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.VALIDATE_CREDIT);
        assertThat(saga.status()).isEqualTo(SagaStatus.STARTED);
    }

    @Test
    void inventory_reserved_preserves_other_saga_fields() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .inventoryReserved(STOCK_RESERVATION_ID);

        assertThat(saga.id()).isEqualTo(SAGA_ID);
        assertThat(saga.orderId()).isEqualTo(ORDER_ID);
        assertThat(saga.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(saga.creditReservationId()).isNull();
    }

    @Test
    void inventory_reserved_on_terminal_saga_throws() {
        Saga completed = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).complete();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> completed.inventoryReserved(STOCK_RESERVATION_ID))
                .withMessageContaining("non-terminal");
    }

    @Test
    void inventory_reserved_blank_id_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> saga.inventoryReserved("   "));
    }

    // ── creditApproved ────────────────────────────────────────────────────────

    @Test
    void credit_approved_stores_credit_reservation_id() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .inventoryReserved(STOCK_RESERVATION_ID)
                .creditApproved(CREDIT_RESERVATION_ID);

        assertThat(saga.creditReservationId()).isEqualTo(CREDIT_RESERVATION_ID);
        assertThat(saga.stockReservationId()).isEqualTo(STOCK_RESERVATION_ID);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.VALIDATE_CREDIT);
    }

    @Test
    void credit_approved_on_terminal_saga_throws() {
        Saga completed = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).complete();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> completed.creditApproved(CREDIT_RESERVATION_ID))
                .withMessageContaining("non-terminal");
    }

    @Test
    void credit_approved_blank_id_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> saga.creditApproved(""));
    }

    // ── complete / cancel ─────────────────────────────────────────────────────

    @Test
    void complete_transitions_to_completed() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).complete();

        assertThat(saga.status()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.COMPLETE_ORDER);
    }

    @Test
    void complete_after_credit_approved_transitions_to_completed_preserving_reservation_ids() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .inventoryReserved(STOCK_RESERVATION_ID)
                .creditApproved(CREDIT_RESERVATION_ID)
                .complete();

        assertThat(saga.status()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.COMPLETE_ORDER);
        assertThat(saga.stockReservationId()).isEqualTo(STOCK_RESERVATION_ID);
        assertThat(saga.creditReservationId()).isEqualTo(CREDIT_RESERVATION_ID);
    }

    @Test
    void cancel_transitions_to_cancelled() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).cancel();

        assertThat(saga.status()).isEqualTo(SagaStatus.CANCELLED);
    }

    @Test
    void cancel_from_terminal_throws() {
        Saga completed = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).complete();

        assertThatIllegalArgumentException()
                .isThrownBy(completed::cancel)
                .withMessageContaining("non-terminal");
    }

    @Test
    void fail_compensation_transitions_to_failed_compensation() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).failCompensation();

        assertThat(saga.status()).isEqualTo(SagaStatus.FAILED_COMPENSATION);
    }

    // ── advance ───────────────────────────────────────────────────────────────

    @Test
    void advance_updates_current_step_without_changing_status() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).advance(SagaStep.RESERVE_INVENTORY);

        assertThat(saga.currentStep()).isEqualTo(SagaStep.RESERVE_INVENTORY);
        assertThat(saga.status()).isEqualTo(SagaStatus.STARTED);
    }

    @Test
    void advance_preserves_customer_id_and_order_id() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).advance(SagaStep.VALIDATE_CREDIT);

        assertThat(saga.orderId()).isEqualTo(ORDER_ID);
        assertThat(saga.customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void advance_on_terminal_saga_throws() {
        Saga completed = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).complete();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> completed.advance(SagaStep.RESERVE_INVENTORY))
                .withMessageContaining("non-terminal");
    }

    @Test
    void advance_on_compensating_saga_throws() {
        Saga compensating = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).compensate();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> compensating.advance(SagaStep.RESERVE_INVENTORY))
                .withMessageContaining("COMPENSATING");
    }

    @Test
    void advance_null_step_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        assertThatNullPointerException()
                .isThrownBy(() -> saga.advance(null))
                .withMessageContaining("nextStep");
    }

    // ── compensate ────────────────────────────────────────────────────────────

    @Test
    void compensate_transitions_status_to_compensating() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).compensate();

        assertThat(saga.status()).isEqualTo(SagaStatus.COMPENSATING);
    }

    @Test
    void compensate_preserves_current_step() {
        Saga advanced = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).advance(SagaStep.RESERVE_INVENTORY);
        Saga compensating = advanced.compensate();

        assertThat(compensating.currentStep()).isEqualTo(SagaStep.RESERVE_INVENTORY);
    }

    @Test
    void compensate_on_terminal_saga_throws() {
        Saga cancelled = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).cancel();

        assertThatIllegalArgumentException()
                .isThrownBy(cancelled::compensate)
                .withMessageContaining("non-terminal");
    }

    @Test
    void compensate_already_compensating_throws() {
        Saga compensating = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).compensate();

        assertThatIllegalArgumentException()
                .isThrownBy(compensating::compensate)
                .withMessageContaining("already COMPENSATING");
    }

    // ── misc ──────────────────────────────────────────────────────────────────

    @Test
    void customer_decision_enum_has_all_expected_values() {
        assertThat(CustomerDecision.values())
                .containsExactlyInAnyOrder(
                        CustomerDecision.ACCEPT_PARTIAL,
                        CustomerDecision.WAIT_BACKORDER,
                        CustomerDecision.CANCEL_ORDER);
    }
}
