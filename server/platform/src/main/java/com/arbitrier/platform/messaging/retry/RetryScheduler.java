package com.arbitrier.platform.messaging.retry;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Transport-agnostic contract for scheduling a retry attempt after a computed delay.
 *
 * <p>Implementations own all timing concerns: they decide how to observe the delay and how to
 * submit the action for execution. They must never block the calling thread and must never call
 * {@link Thread#sleep}.
 *
 * <p>By convention, when {@code delay} is {@link BackoffDelay#ZERO} (i.e.,
 * {@link BackoffDelay#isImmediate()} is {@code true}), implementations should execute the
 * action without involving any scheduling infrastructure.
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 */
public interface RetryScheduler {

    /**
     * Schedule {@code action} for execution after the given {@code delay}.
     *
     * <p>The returned stage completes when the action's own stage completes — either normally
     * or exceptionally. The calling thread must not be blocked while waiting for the result.
     *
     * @param action supplier that produces the work to execute; must not be null
     * @param delay  how long to wait before invoking {@code action}; must not be null
     * @param <T>    result type of the action
     * @return a {@link CompletionStage} that mirrors the action's completion
     */
    <T> CompletionStage<T> schedule(Supplier<CompletionStage<T>> action, BackoffDelay delay);
}
