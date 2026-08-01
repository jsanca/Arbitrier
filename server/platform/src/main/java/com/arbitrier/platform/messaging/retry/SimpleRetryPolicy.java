package com.arbitrier.platform.messaging.retry;

import com.arbitrier.platform.validation.Require;

/**
 * Default {@link RetryPolicy} that retries until a configurable maximum attempt count is reached.
 *
 * <p>Returns {@link RetryDecision#retry} while {@code attempt < maxAttempts} and
 * {@link RetryDecision#stop} once the attempt ceiling is reached. No delay or backoff is
 * applied; scheduling is the caller's responsibility.
 *
 * <p>To express "no retry" (try once, fail permanently), construct with {@code maxAttempts = 1}.
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 */
public final class SimpleRetryPolicy implements RetryPolicy {

    private final int maxAttempts;

    /**
     * Create a policy that allows up to {@code maxAttempts} total attempts.
     *
     * @param maxAttempts maximum number of attempts before the policy returns stop; must be &gt;= 1
     * @throws IllegalArgumentException if {@code maxAttempts} is less than 1
     */
    public SimpleRetryPolicy(final int maxAttempts) {
        Require.isTrue(maxAttempts >= 1, "maxAttempts must be >= 1, got: " + maxAttempts);
        this.maxAttempts = maxAttempts;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link RetryDecision#retry} when {@code attempt < maxAttempts},
     * and {@link RetryDecision#stop} otherwise.
     */
    @Override
    public RetryDecision evaluate(final int attempt, final Throwable failure) {
        Require.notNull(failure, "failure");
        if (attempt < maxAttempts) {
            return RetryDecision.retry(attempt, maxAttempts);
        }
        return RetryDecision.stop(attempt, maxAttempts);
    }

    /** Returns the maximum number of attempts this policy allows. */
    public int maxAttempts() {
        return maxAttempts;
    }
}
