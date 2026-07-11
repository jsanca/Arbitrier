package com.arbitrier.orchestrator.domain;

import com.arbitrier.orchestrator.domain.model.CorporateBulkOrderSagaRetryPolicy;
import com.arbitrier.orchestrator.domain.model.RetryDecision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link CorporateBulkOrderSagaRetryPolicy}.
 *
 * <p>Verifies that the retry policy models business-specific attempt limits
 * and delegates correctly to {@link com.arbitrier.orchestrator.domain.model.RetryContext}.
 */
class CorporateBulkOrderSagaRetryPolicyTest {

    @Test
    void inventory_timeout_before_max_attempts_returns_retry() {
        var policy = new CorporateBulkOrderSagaRetryPolicy(3, 3);

        assertThat(policy.evaluateInventory(1)).isEqualTo(RetryDecision.RETRY);
        assertThat(policy.evaluateInventory(2)).isEqualTo(RetryDecision.RETRY);
    }

    @Test
    void inventory_timeout_at_max_attempts_returns_exhaust() {
        var policy = new CorporateBulkOrderSagaRetryPolicy(3, 3);

        assertThat(policy.evaluateInventory(3)).isEqualTo(RetryDecision.EXHAUST);
    }

    @Test
    void credit_timeout_before_max_attempts_returns_retry() {
        var policy = new CorporateBulkOrderSagaRetryPolicy(3, 3);

        assertThat(policy.evaluateCredit(1)).isEqualTo(RetryDecision.RETRY);
        assertThat(policy.evaluateCredit(2)).isEqualTo(RetryDecision.RETRY);
    }

    @Test
    void credit_timeout_at_max_attempts_returns_exhaust() {
        var policy = new CorporateBulkOrderSagaRetryPolicy(3, 3);

        assertThat(policy.evaluateCredit(3)).isEqualTo(RetryDecision.EXHAUST);
    }

    @Test
    void inventory_and_credit_limits_are_independent() {
        var policy = new CorporateBulkOrderSagaRetryPolicy(2, 5);

        assertThat(policy.evaluateInventory(2)).isEqualTo(RetryDecision.EXHAUST);
        assertThat(policy.evaluateCredit(2)).isEqualTo(RetryDecision.RETRY);
        assertThat(policy.evaluateCredit(5)).isEqualTo(RetryDecision.EXHAUST);
    }

    @Test
    void retry_decision_should_retry_returns_true_for_retry() {
        assertThat(RetryDecision.RETRY.shouldRetry()).isTrue();
    }

    @Test
    void retry_decision_should_retry_returns_false_for_exhaust() {
        assertThat(RetryDecision.EXHAUST.shouldRetry()).isFalse();
    }

    @Test
    void zero_inventory_max_attempts_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CorporateBulkOrderSagaRetryPolicy(0, 3))
                .withMessageContaining("inventoryMaxAttempts");
    }

    @Test
    void zero_credit_max_attempts_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CorporateBulkOrderSagaRetryPolicy(3, 0))
                .withMessageContaining("creditMaxAttempts");
    }
}
