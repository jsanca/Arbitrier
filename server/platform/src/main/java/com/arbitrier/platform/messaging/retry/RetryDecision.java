package com.arbitrier.platform.messaging.retry;

/**
 * Immutable result of a {@link RetryPolicy} evaluation: whether the caller should retry and
 * the context that led to the decision.
 *
 * <p>{@code attempt} is the 1-indexed number of the attempt that just failed.
 * {@code maxAttempts} is the ceiling imposed by the policy.
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 */
public record RetryDecision(boolean shouldRetry, int attempt, int maxAttempts) {

    /** Decision to retry after the given attempt. */
    public static RetryDecision retry(final int attempt, final int maxAttempts) {
        return new RetryDecision(true, attempt, maxAttempts);
    }

    /** Decision to stop after the given attempt — no further retries. */
    public static RetryDecision stop(final int attempt, final int maxAttempts) {
        return new RetryDecision(false, attempt, maxAttempts);
    }
}
