package com.arbitrier.platform.messaging.retry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link SimpleRetryPolicy}.
 */
class SimpleRetryPolicyTest {

    private static final RuntimeException FAILURE = new RuntimeException("transport error");

    // ── construction ──────────────────────────────────────────────────────────

    @Test
    void rejects_maxAttempts_below_one() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SimpleRetryPolicy(0))
                .withMessageContaining("maxAttempts");
    }

    @Test
    void rejects_negative_maxAttempts() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SimpleRetryPolicy(-1))
                .withMessageContaining("maxAttempts");
    }

    @Test
    void accepts_maxAttempts_of_one() {
        var policy = new SimpleRetryPolicy(1);
        assertThat(policy.maxAttempts()).isEqualTo(1);
    }

    // ── evaluate — null guard ─────────────────────────────────────────────────

    @Test
    void rejects_null_failure() {
        var policy = new SimpleRetryPolicy(3);
        assertThatNullPointerException()
                .isThrownBy(() -> policy.evaluate(1, null))
                .withMessageContaining("failure");
    }

    // ── evaluate — retry before max attempts ──────────────────────────────────

    @Test
    void retries_on_first_failure_when_max_allows_more() {
        var policy = new SimpleRetryPolicy(3);

        RetryDecision decision = policy.evaluate(1, FAILURE);

        assertThat(decision.shouldRetry()).isTrue();
    }

    @Test
    void retries_on_second_failure_when_max_allows_more() {
        var policy = new SimpleRetryPolicy(3);

        RetryDecision decision = policy.evaluate(2, FAILURE);

        assertThat(decision.shouldRetry()).isTrue();
    }

    // ── evaluate — stop at max attempts ───────────────────────────────────────

    @Test
    void stops_when_attempt_equals_max() {
        var policy = new SimpleRetryPolicy(3);

        RetryDecision decision = policy.evaluate(3, FAILURE);

        assertThat(decision.shouldRetry()).isFalse();
    }

    @Test
    void stops_on_first_failure_when_maxAttempts_is_one() {
        var policy = new SimpleRetryPolicy(1);

        RetryDecision decision = policy.evaluate(1, FAILURE);

        assertThat(decision.shouldRetry()).isFalse();
    }

    // ── decision context ──────────────────────────────────────────────────────

    @Test
    void decision_carries_attempt_number() {
        var policy = new SimpleRetryPolicy(5);

        RetryDecision decision = policy.evaluate(2, FAILURE);

        assertThat(decision.attempt()).isEqualTo(2);
    }

    @Test
    void decision_carries_max_attempts() {
        var policy = new SimpleRetryPolicy(5);

        RetryDecision decision = policy.evaluate(1, FAILURE);

        assertThat(decision.maxAttempts()).isEqualTo(5);
    }

    @Test
    void stop_decision_carries_attempt_and_max() {
        var policy = new SimpleRetryPolicy(2);

        RetryDecision decision = policy.evaluate(2, FAILURE);

        assertThat(decision.shouldRetry()).isFalse();
        assertThat(decision.attempt()).isEqualTo(2);
        assertThat(decision.maxAttempts()).isEqualTo(2);
    }
}
