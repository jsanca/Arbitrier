package com.arbitrier.platform.test;

import com.arbitrier.platform.time.FixedTimeProvider;
import com.arbitrier.platform.time.TimeProvider;

import java.time.Instant;

/**
 * Test-support factory for pinned-time {@link TimeProvider} instances.
 *
 * <p>Wraps {@link FixedTimeProvider} with a well-known default instant so tests
 * do not need to pick arbitrary timestamps.
 *
 * <p>Example:
 * <pre>{@code
 * TimeProvider clock = FixedClock.defaults();
 * // or
 * TimeProvider clock = FixedClock.at(Instant.parse("2026-03-01T09:00:00Z"));
 * }</pre>
 */
public final class FixedClock {

    /**
     * Stable test instant used by {@link #defaults()}.
     * Chosen to be unambiguous across time zones (10:00 UTC on 2026-01-15).
     */
    public static final Instant TEST_INSTANT = Instant.parse("2026-01-15T10:00:00Z");

    private FixedClock() {
    }

    /**
     * Returns a {@link TimeProvider} always returning {@link #TEST_INSTANT}.
     *
     * @return a pinned clock at the default test instant
     */
    public static TimeProvider defaults() {
        return FixedTimeProvider.of(TEST_INSTANT);
    }

    /**
     * Returns a {@link TimeProvider} always returning {@code instant}.
     *
     * @param instant the instant to pin; must not be null
     * @return a pinned clock at {@code instant}
     */
    public static TimeProvider at(Instant instant) {
        return FixedTimeProvider.of(instant);
    }
}
