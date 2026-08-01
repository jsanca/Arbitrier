package com.arbitrier.platform.messaging.retry;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link ExponentialBackoffStrategy}.
 */
class ExponentialBackoffStrategyTest {

    private static final Duration INITIAL = Duration.ofMillis(500);
    private static final double   MULT    = 2.0;
    private static final Duration MAX     = Duration.ofSeconds(30);

    // ── construction ──────────────────────────────────────────────────────────

    @Test
    void rejects_null_initialDelay() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ExponentialBackoffStrategy(null, MULT, MAX))
                .withMessageContaining("initialDelay");
    }

    @Test
    void rejects_null_maxDelay() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ExponentialBackoffStrategy(INITIAL, MULT, null))
                .withMessageContaining("maxDelay");
    }

    @Test
    void rejects_negative_initialDelay() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ExponentialBackoffStrategy(Duration.ofMillis(-1), MULT, MAX))
                .withMessageContaining("initialDelay");
    }

    @Test
    void rejects_multiplier_of_exactly_one() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ExponentialBackoffStrategy(INITIAL, 1.0, MAX))
                .withMessageContaining("multiplier");
    }

    @Test
    void rejects_multiplier_below_one() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ExponentialBackoffStrategy(INITIAL, 0.5, MAX))
                .withMessageContaining("multiplier");
    }

    @Test
    void rejects_zero_maxDelay() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ExponentialBackoffStrategy(INITIAL, MULT, Duration.ZERO))
                .withMessageContaining("maxDelay");
    }

    @Test
    void rejects_negative_maxDelay() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ExponentialBackoffStrategy(INITIAL, MULT, Duration.ofMillis(-1)))
                .withMessageContaining("maxDelay");
    }

    @Test
    void rejects_maxDelay_less_than_initialDelay() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ExponentialBackoffStrategy(
                        Duration.ofSeconds(5), MULT, Duration.ofSeconds(1)))
                .withMessageContaining("maxDelay");
    }

    @Test
    void accepts_valid_configuration() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, MAX);
        assertThat(strategy.initialDelay()).isEqualTo(INITIAL);
        assertThat(strategy.multiplier()).isEqualTo(MULT);
        assertThat(strategy.maxDelay()).isEqualTo(MAX);
    }

    // ── nextDelay — attempt guard ─────────────────────────────────────────────

    @Test
    void rejects_attempt_below_one() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, MAX);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> strategy.nextDelay(0))
                .withMessageContaining("attempt");
    }

    // ── nextDelay — first attempt ─────────────────────────────────────────────

    @Test
    void first_attempt_returns_zero_delay() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, MAX);

        BackoffDelay delay = strategy.nextDelay(1);

        assertThat(delay.isImmediate()).isTrue();
        assertThat(delay.value()).isEqualTo(Duration.ZERO);
    }

    // ── nextDelay — exponential growth ────────────────────────────────────────

    @Test
    void attempt_2_returns_initial_delay() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, MAX);

        BackoffDelay delay = strategy.nextDelay(2);

        assertThat(delay.value()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    void attempt_3_doubles_initial_delay() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, MAX);

        BackoffDelay delay = strategy.nextDelay(3);

        assertThat(delay.value()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void attempt_4_quadruples_initial_delay() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, MAX);

        BackoffDelay delay = strategy.nextDelay(4);

        assertThat(delay.value()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void attempt_5_grows_exponentially() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, MAX);

        BackoffDelay delay = strategy.nextDelay(5);

        assertThat(delay.value()).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    void delay_grows_with_each_attempt() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, MAX);

        long prev = 0;
        for (int attempt = 2; attempt <= 7; attempt++) {
            long ms = strategy.nextDelay(attempt).value().toMillis();
            assertThat(ms).as("attempt %d should exceed attempt %d", attempt, attempt - 1)
                          .isGreaterThan(prev);
            prev = ms;
        }
    }

    // ── nextDelay — maximum delay cap ─────────────────────────────────────────

    @Test
    void delay_is_capped_at_maxDelay() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, Duration.ofSeconds(4));

        BackoffDelay delay = strategy.nextDelay(5);

        assertThat(delay.value()).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    void further_attempts_stay_at_maxDelay() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, Duration.ofSeconds(4));

        assertThat(strategy.nextDelay(6).value()).isEqualTo(Duration.ofSeconds(4));
        assertThat(strategy.nextDelay(10).value()).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    void delay_just_before_cap_is_not_capped() {
        var strategy = new ExponentialBackoffStrategy(INITIAL, MULT, Duration.ofSeconds(4));

        BackoffDelay delay = strategy.nextDelay(4);

        assertThat(delay.value()).isEqualTo(Duration.ofSeconds(2));
    }

    // ── BackoffDelay value type ───────────────────────────────────────────────

    @Test
    void backoff_delay_zero_sentinel_is_immediate() {
        assertThat(BackoffDelay.ZERO.isImmediate()).isTrue();
        assertThat(BackoffDelay.ZERO.value()).isEqualTo(Duration.ZERO);
    }

    @Test
    void backoff_delay_of_millis_constructs_correctly() {
        BackoffDelay delay = BackoffDelay.ofMillis(1500);
        assertThat(delay.value()).isEqualTo(Duration.ofMillis(1500));
        assertThat(delay.isImmediate()).isFalse();
    }

    @Test
    void backoff_delay_rejects_negative_duration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BackoffDelay(Duration.ofMillis(-1)))
                .withMessageContaining("BackoffDelay.value");
    }
}
