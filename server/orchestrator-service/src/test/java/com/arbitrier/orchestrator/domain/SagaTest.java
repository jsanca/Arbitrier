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

    // ── stockRejected ─────────────────────────────────────────────────────────

    @Test
    void stock_rejected_cancels_saga_directly() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).stockRejected();

        assertThat(saga.status()).isEqualTo(SagaStatus.CANCELLED);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.ORDER_CREATED);
    }

    @Test
    void stock_rejected_preserves_ids() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).stockRejected();

        assertThat(saga.id()).isEqualTo(SAGA_ID);
        assertThat(saga.orderId()).isEqualTo(ORDER_ID);
        assertThat(saga.customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void stock_rejected_on_terminal_saga_throws() {
        Saga completed = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).complete();

        assertThatIllegalArgumentException()
                .isThrownBy(completed::stockRejected)
                .withMessageContaining("non-terminal");
    }

    @Test
    void stock_rejected_on_compensating_saga_throws() {
        Saga compensating = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).compensate();

        assertThatIllegalArgumentException()
                .isThrownBy(compensating::stockRejected)
                .withMessageContaining("COMPENSATING");
    }

    // ── creditRejected ────────────────────────────────────────────────────────

    @Test
    void credit_rejected_transitions_to_compensating_and_compensate_inventory_step() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .inventoryReserved(STOCK_RESERVATION_ID)
                .creditRejected();

        assertThat(saga.status()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.COMPENSATE_INVENTORY);
    }

    @Test
    void credit_rejected_preserves_stock_reservation_id() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .inventoryReserved(STOCK_RESERVATION_ID)
                .creditRejected();

        assertThat(saga.stockReservationId()).isEqualTo(STOCK_RESERVATION_ID);
    }

    @Test
    void credit_rejected_without_stock_reservation_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        assertThatIllegalArgumentException()
                .isThrownBy(saga::creditRejected)
                .withMessageContaining("stock reservation");
    }

    @Test
    void credit_rejected_on_terminal_saga_throws() {
        Saga completed = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).complete();

        assertThatIllegalArgumentException()
                .isThrownBy(completed::creditRejected)
                .withMessageContaining("non-terminal");
    }

    @Test
    void credit_rejected_on_compensating_saga_throws() {
        Saga compensating = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .inventoryReserved(STOCK_RESERVATION_ID)
                .compensate();

        assertThatIllegalArgumentException()
                .isThrownBy(compensating::creditRejected)
                .withMessageContaining("COMPENSATING");
    }

    // ── inventoryReleased ─────────────────────────────────────────────────────

    @Test
    void inventory_released_cancels_saga_from_compensating() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .inventoryReserved(STOCK_RESERVATION_ID)
                .creditRejected()
                .inventoryReleased();

        assertThat(saga.status()).isEqualTo(SagaStatus.CANCELLED);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.COMPENSATE_INVENTORY);
    }

    @Test
    void inventory_released_preserves_stock_reservation_id() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .inventoryReserved(STOCK_RESERVATION_ID)
                .creditRejected()
                .inventoryReleased();

        assertThat(saga.stockReservationId()).isEqualTo(STOCK_RESERVATION_ID);
    }

    @Test
    void inventory_released_from_non_compensating_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        assertThatIllegalArgumentException()
                .isThrownBy(saga::inventoryReleased)
                .withMessageContaining("COMPENSATING");
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
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).compensate().failCompensation();

        assertThat(saga.status()).isEqualTo(SagaStatus.FAILED_COMPENSATION);
    }

    @Test
    void fail_compensation_on_completed_saga_throws() {
        Saga completed = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).complete();

        assertThatIllegalArgumentException()
                .isThrownBy(completed::failCompensation)
                .withMessageContaining("non-terminal");
    }

    @Test
    void fail_compensation_on_cancelled_saga_throws() {
        Saga cancelled = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).cancel();

        assertThatIllegalArgumentException()
                .isThrownBy(cancelled::failCompensation)
                .withMessageContaining("non-terminal");
    }

    @Test
    void fail_compensation_on_failed_compensation_saga_throws() {
        Saga failed = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).compensate().failCompensation();

        assertThatIllegalArgumentException()
                .isThrownBy(failed::failCompensation)
                .withMessageContaining("non-terminal");
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

    // ── awaitInventoryResponse ────────────────────────────────────────────────

    @Test
    void await_inventory_response_transitions_to_waiting_for_inventory_and_reserve_inventory_step() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).awaitInventoryResponse();

        assertThat(saga.status()).isEqualTo(SagaStatus.WAITING_FOR_INVENTORY);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.RESERVE_INVENTORY);
    }

    @Test
    void await_inventory_response_preserves_identifiers() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).awaitInventoryResponse();

        assertThat(saga.id()).isEqualTo(SAGA_ID);
        assertThat(saga.orderId()).isEqualTo(ORDER_ID);
        assertThat(saga.customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void await_inventory_response_from_non_started_throws() {
        Saga waiting = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).awaitInventoryResponse();

        assertThatIllegalArgumentException()
                .isThrownBy(waiting::awaitInventoryResponse)
                .withMessageContaining("STARTED");
    }

    // ── awaitCreditResponse ───────────────────────────────────────────────────

    @Test
    void await_credit_response_transitions_to_waiting_for_credit() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .awaitInventoryResponse()
                .inventoryReserved(STOCK_RESERVATION_ID)
                .awaitCreditResponse();

        assertThat(saga.status()).isEqualTo(SagaStatus.WAITING_FOR_CREDIT);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.VALIDATE_CREDIT);
    }

    @Test
    void await_credit_response_from_non_waiting_for_inventory_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        assertThatIllegalArgumentException()
                .isThrownBy(saga::awaitCreditResponse)
                .withMessageContaining("WAITING_FOR_INVENTORY");
    }

    // ── inventoryTimedOut ─────────────────────────────────────────────────────

    @Test
    void inventory_timed_out_validates_waiting_for_inventory_and_returns_same_instance() {
        Saga waiting = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).awaitInventoryResponse();

        assertThat(waiting.inventoryTimedOut()).isSameAs(waiting);
    }

    @Test
    void inventory_timed_out_from_wrong_status_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        assertThatIllegalArgumentException()
                .isThrownBy(saga::inventoryTimedOut)
                .withMessageContaining("WAITING_FOR_INVENTORY");
    }

    @Test
    void inventory_timed_out_on_completed_saga_throws() {
        Saga completed = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).complete();

        assertThatIllegalArgumentException()
                .isThrownBy(completed::inventoryTimedOut)
                .withMessageContaining("WAITING_FOR_INVENTORY");
    }

    // ── creditTimedOut ────────────────────────────────────────────────────────

    @Test
    void credit_timed_out_validates_waiting_for_credit_and_returns_same_instance() {
        Saga waiting = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .awaitInventoryResponse()
                .inventoryReserved(STOCK_RESERVATION_ID)
                .awaitCreditResponse();

        assertThat(waiting.creditTimedOut()).isSameAs(waiting);
    }

    @Test
    void credit_timed_out_from_wrong_status_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).awaitInventoryResponse();

        assertThatIllegalArgumentException()
                .isThrownBy(saga::creditTimedOut)
                .withMessageContaining("WAITING_FOR_CREDIT");
    }

    // ── retryInventory / retryCredit ──────────────────────────────────────────

    @Test
    void retry_inventory_returns_same_instance() {
        Saga waiting = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).awaitInventoryResponse();

        assertThat(waiting.retryInventory()).isSameAs(waiting);
        assertThat(waiting.retryInventory().status()).isEqualTo(SagaStatus.WAITING_FOR_INVENTORY);
    }

    @Test
    void retry_inventory_from_wrong_status_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        assertThatIllegalArgumentException()
                .isThrownBy(saga::retryInventory)
                .withMessageContaining("WAITING_FOR_INVENTORY");
    }

    @Test
    void retry_credit_returns_same_instance() {
        Saga waiting = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .awaitInventoryResponse()
                .inventoryReserved(STOCK_RESERVATION_ID)
                .awaitCreditResponse();

        assertThat(waiting.retryCredit()).isSameAs(waiting);
        assertThat(waiting.retryCredit().status()).isEqualTo(SagaStatus.WAITING_FOR_CREDIT);
    }

    @Test
    void retry_credit_from_wrong_status_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID).awaitInventoryResponse();

        assertThatIllegalArgumentException()
                .isThrownBy(saga::retryCredit)
                .withMessageContaining("WAITING_FOR_CREDIT");
    }

    // ── compensate from waiting states ────────────────────────────────────────

    @Test
    void compensate_from_waiting_for_inventory_transitions_to_compensating() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .awaitInventoryResponse()
                .compensate();

        assertThat(saga.status()).isEqualTo(SagaStatus.COMPENSATING);
    }

    @Test
    void compensate_from_waiting_for_credit_transitions_to_compensating() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID)
                .awaitInventoryResponse()
                .inventoryReserved(STOCK_RESERVATION_ID)
                .awaitCreditResponse()
                .compensate();

        assertThat(saga.status()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(saga.stockReservationId()).isEqualTo(STOCK_RESERVATION_ID);
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
