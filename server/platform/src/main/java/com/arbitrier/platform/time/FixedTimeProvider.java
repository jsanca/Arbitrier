package com.arbitrier.platform.time;

import java.time.Instant;
import java.util.Objects;

/**
 * {@link TimeProvider} implementation that always returns a fixed {@link Instant}.
 *
 * <p>Intended for use in unit and integration tests where deterministic time is required.
 * The {@link com.arbitrier.platform.test.FixedClock} test-support class provides
 * convenient factory methods with a well-known default instant.
 *
 * <p>Example:
 * <pre>{@code
 * TimeProvider clock = FixedTimeProvider.of(Instant.parse("2026-01-15T10:00:00Z"));
 * }</pre>
 */
public final class FixedTimeProvider implements TimeProvider {

    private final Instant fixedInstant;

    private FixedTimeProvider(Instant fixedInstant) {
        this.fixedInstant = Objects.requireNonNull(fixedInstant, "fixedInstant must not be null");
    }

    /**
     * Creates a {@code FixedTimeProvider} pinned to {@code instant}.
     *
     * @param instant the instant to return on every {@link #now()} call; must not be null
     * @return a new {@code FixedTimeProvider}
     */
    public static FixedTimeProvider of(Instant instant) {
        return new FixedTimeProvider(instant);
    }

    @Override
    public Instant now() {
        return fixedInstant;
    }
}
