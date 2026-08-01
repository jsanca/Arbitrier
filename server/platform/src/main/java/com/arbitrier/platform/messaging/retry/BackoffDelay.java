package com.arbitrier.platform.messaging.retry;

import com.arbitrier.platform.validation.Require;

import java.time.Duration;

/**
 * Immutable value type representing a computed delay between publication attempts.
 *
 * <p>Produced by a {@link BackoffStrategy}; consumed by the scheduler (ARB-022.6.3) that
 * decides when to submit the next attempt. This type does not perform any sleeping or
 * scheduling itself.
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 */
public record BackoffDelay(Duration value) {

    /** Canonical zero-delay sentinel — no wait before the next attempt. */
    public static final BackoffDelay ZERO = new BackoffDelay(Duration.ZERO);

    public BackoffDelay {
        Require.notNull(value, "BackoffDelay.value");
        if (value.isNegative()) {
            throw new IllegalArgumentException("BackoffDelay.value must not be negative, got: " + value);
        }
    }

    /**
     * Create a delay of the given number of milliseconds.
     *
     * @param milliseconds non-negative duration in milliseconds
     * @throws IllegalArgumentException if {@code milliseconds} is negative
     */
    public static BackoffDelay ofMillis(final long milliseconds) {
        return new BackoffDelay(Duration.ofMillis(milliseconds));
    }

    /** Returns {@code true} when this delay is zero — the next attempt should start immediately. */
    public boolean isImmediate() {
        return value.isZero();
    }
}
