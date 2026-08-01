package com.arbitrier.platform.messaging.retry;

/**
 * Transport-agnostic contract for computing the delay before a retry attempt.
 *
 * <p>Implementations calculate how long the caller should wait before submitting attempt
 * {@code attempt}. They must not perform scheduling, sleep, or interact with any transport
 * infrastructure — delay calculation is their sole responsibility.
 *
 * <p>By convention, {@code attempt = 1} represents the very first try (before any retry has
 * occurred), and implementations should return {@link BackoffDelay#ZERO} for that case.
 * {@code attempt = 2} is the first retry, {@code attempt = 3} the second, and so on.
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 */
public interface BackoffStrategy {

    /**
     * Compute the delay to observe before submitting the given attempt.
     *
     * @param attempt the 1-indexed attempt number (must be &gt;= 1)
     * @return a non-null {@link BackoffDelay}; never negative
     * @throws IllegalArgumentException if {@code attempt} is less than 1
     */
    BackoffDelay nextDelay(int attempt);
}
