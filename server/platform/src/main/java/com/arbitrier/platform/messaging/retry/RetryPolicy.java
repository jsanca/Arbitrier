package com.arbitrier.platform.messaging.retry;

/**
 * Transport-agnostic contract for retry decisions.
 *
 * <p>Implementations decide whether to retry based on the number of attempts made so far and
 * the failure that just occurred. They must not perform scheduling, introduce delays, or
 * interact with any transport infrastructure.
 *
 * <p>The caller is responsible for executing retries; this interface only advises.
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 */
public interface RetryPolicy {

    /**
     * Evaluate whether the caller should attempt again after a failure.
     *
     * @param attempt the 1-indexed number of the attempt that just failed (must be &gt;= 1)
     * @param failure the exception thrown or completion failure from the most recent attempt
     * @return a {@link RetryDecision} indicating whether to retry and capturing the decision context
     */
    RetryDecision evaluate(int attempt, Throwable failure);
}
