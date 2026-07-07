package com.arbitrier.platform.time;

import java.time.Instant;

/**
 * {@link TimeProvider} implementation that delegates to {@link Instant#now()}.
 *
 * <p>Use the singleton {@link #INSTANCE} rather than creating new instances.
 * Register it as a Spring bean in each service's {@code config} package:
 * <pre>{@code
 * @Bean
 * TimeProvider timeProvider() {
 *     return SystemClock.INSTANCE;
 * }
 * }</pre>
 */
public final class SystemClock implements TimeProvider {

    /** Singleton — use this instead of instantiating {@code SystemClock} directly. */
    public static final TimeProvider INSTANCE = new SystemClock();

    private SystemClock() {
    }

    @Override
    public Instant now() {
        return Instant.now();
    }
}
