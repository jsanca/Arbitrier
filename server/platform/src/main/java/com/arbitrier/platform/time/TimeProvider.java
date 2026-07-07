package com.arbitrier.platform.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Abstraction over wall-clock time.
 *
 * <p>Inject {@code TimeProvider} into services instead of calling {@link Instant#now()} directly.
 * This makes time deterministic in tests via {@link FixedTimeProvider}.
 *
 * <p>Production services bind {@link SystemClock#INSTANCE} in their {@code config} package.
 */
public interface TimeProvider {

    /**
     * Returns the current instant according to this provider.
     *
     * @return the current {@link Instant}; never null
     */
    Instant now();

    /**
     * Returns today's date in the given time zone.
     *
     * @param zone the time zone; must not be null
     * @return today as a {@link LocalDate}
     */
    default LocalDate today(ZoneId zone) {
        return LocalDate.ofInstant(now(), zone);
    }
}
