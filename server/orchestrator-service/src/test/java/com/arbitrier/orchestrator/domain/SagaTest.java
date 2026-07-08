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

    @Test
    void start_sets_started_status_and_reserve_inventory_step() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID);

        assertThat(saga.id()).isEqualTo(SAGA_ID);
        assertThat(saga.orderId()).isEqualTo(ORDER_ID);
        assertThat(saga.status()).isEqualTo(SagaStatus.STARTED);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.RESERVE_INVENTORY);
        assertThat(saga.customerDecision()).isNull();
    }

    @Test
    void await_customer_decision_transitions_to_awaiting_customer_decision() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID).awaitCustomerDecision();

        assertThat(saga.status()).isEqualTo(SagaStatus.AWAITING_CUSTOMER_DECISION);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.AWAIT_CUSTOMER_DECISION);
    }

    @Test
    void apply_customer_decision_from_started_throws() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> saga.applyCustomerDecision(CustomerDecision.ACCEPT_PARTIAL))
                .withMessageContaining("AWAITING_CUSTOMER_DECISION");
    }

    @Test
    void apply_customer_decision_accept_partial_resumes_with_started_and_validate_credit_step() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID)
                .awaitCustomerDecision()
                .applyCustomerDecision(CustomerDecision.ACCEPT_PARTIAL);

        assertThat(saga.status()).isEqualTo(SagaStatus.STARTED);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.VALIDATE_CREDIT);
        assertThat(saga.customerDecision()).isEqualTo(CustomerDecision.ACCEPT_PARTIAL);
    }

    @Test
    void apply_customer_decision_null_throws_null_pointer_exception() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID).awaitCustomerDecision();

        assertThatNullPointerException()
                .isThrownBy(() -> saga.applyCustomerDecision(null));
    }

    @Test
    void complete_transitions_to_completed() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID).complete();

        assertThat(saga.status()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(saga.currentStep()).isEqualTo(SagaStep.COMPLETE_ORDER);
    }

    @Test
    void cancel_transitions_to_cancelled() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID).cancel();

        assertThat(saga.status()).isEqualTo(SagaStatus.CANCELLED);
    }

    @Test
    void cancel_from_terminal_throws() {
        Saga completed = Saga.start(SAGA_ID, ORDER_ID).complete();

        assertThatIllegalArgumentException()
                .isThrownBy(completed::cancel)
                .withMessageContaining("non-terminal");
    }

    @Test
    void fail_compensation_transitions_to_failed_compensation() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID).failCompensation();

        assertThat(saga.status()).isEqualTo(SagaStatus.FAILED_COMPENSATION);
    }

    @Test
    void customer_decision_enum_has_all_expected_values() {
        assertThat(CustomerDecision.values())
                .containsExactlyInAnyOrder(
                        CustomerDecision.ACCEPT_PARTIAL,
                        CustomerDecision.WAIT_BACKORDER,
                        CustomerDecision.CANCEL_ORDER);
    }
}
