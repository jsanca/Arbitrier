package com.arbitrier.platform.messaging.retry;

import com.arbitrier.platform.validation.Require;

import java.time.Duration;

/**
 * {@link BackoffStrategy} that grows the delay exponentially across successive attempts.
 *
 * <h2>Delay formula</h2>
 * <pre>
 *   attempt == 1  →  0 ms  (first try; no prior failure)
 *   attempt &gt;= 2  →  min(initialDelay × multiplierᵃᵗᵗᵉᵐᵖᵗ⁻², maxDelay)
 * </pre>
 *
 * <h2>Example progression</h2>
 * <p>With {@code initialDelay=500 ms}, {@code multiplier=2.0}, {@code maxDelay=30 s}:
 * <pre>
 *   attempt 1  →    0 ms
 *   attempt 2  →  500 ms
 *   attempt 3  →    1  s
 *   attempt 4  →    2  s
 *   attempt 5  →    4  s
 *   attempt 6  →    8  s
 *   attempt 7  →   16  s
 *   attempt 8  →   30  s  (capped)
 * </pre>
 *
 * <p>No jitter, scheduling, or sleeping is performed here; timing concerns belong to the
 * scheduler introduced in ARB-022.6.3.
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 */
public final class ExponentialBackoffStrategy implements BackoffStrategy {

    private final Duration initialDelay;
    private final double multiplier;
    private final Duration maxDelay;

    /**
     * Create a strategy with the given growth parameters.
     *
     * @param initialDelay delay applied on the first retry (attempt 2); must not be negative
     * @param multiplier   growth factor applied on each subsequent retry; must be &gt; 1.0
     * @param maxDelay     upper bound on the computed delay; must be positive and &gt;= initialDelay
     * @throws NullPointerException     if {@code initialDelay} or {@code maxDelay} is null
     * @throws IllegalArgumentException if any parameter violates the constraints above
     */
    public ExponentialBackoffStrategy(final Duration initialDelay,
                                      final double multiplier,
                                      final Duration maxDelay) {
        Require.notNull(initialDelay, "initialDelay");
        Require.notNull(maxDelay, "maxDelay");
        if (initialDelay.isNegative()) {
            throw new IllegalArgumentException("initialDelay must not be negative, got: " + initialDelay);
        }
        if (multiplier <= 1.0) {
            throw new IllegalArgumentException("multiplier must be > 1.0, got: " + multiplier);
        }
        if (maxDelay.isNegative() || maxDelay.isZero()) {
            throw new IllegalArgumentException("maxDelay must be positive, got: " + maxDelay);
        }
        if (maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException(
                    "maxDelay must be >= initialDelay, got maxDelay=" + maxDelay + " initialDelay=" + initialDelay);
        }
        this.initialDelay = initialDelay;
        this.multiplier = multiplier;
        this.maxDelay = maxDelay;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link BackoffDelay#ZERO} for {@code attempt == 1}. For subsequent attempts
     * computes {@code initialDelay × multiplier^(attempt−2)}, capped at {@code maxDelay}.
     *
     * @throws IllegalArgumentException if {@code attempt} is less than 1
     */
    @Override
    public BackoffDelay nextDelay(final int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1, got: " + attempt);
        }
        if (attempt == 1) {
            return BackoffDelay.ZERO;
        }
        final long initialMs = initialDelay.toMillis();
        final long maxMs = maxDelay.toMillis();
        final long rawMs = Math.round(initialMs * Math.pow(multiplier, attempt - 2));
        return BackoffDelay.ofMillis(Math.min(rawMs, maxMs));
    }

    /** Returns the initial delay applied on the first retry. */
    public Duration initialDelay() {
        return initialDelay;
    }

    /** Returns the multiplicative growth factor. */
    public double multiplier() {
        return multiplier;
    }

    /** Returns the upper bound on the computed delay. */
    public Duration maxDelay() {
        return maxDelay;
    }
}
