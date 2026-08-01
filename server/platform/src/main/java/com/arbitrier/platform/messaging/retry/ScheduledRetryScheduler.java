package com.arbitrier.platform.messaging.retry;

import com.arbitrier.platform.validation.Require;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * {@link RetryScheduler} backed by a {@link ScheduledExecutorService}.
 *
 * <p>For {@link BackoffDelay#ZERO} delays the action is executed inline without involving the
 * executor, avoiding scheduling overhead on the common fast path.
 *
 * <p>For non-zero delays the action is submitted to the executor after the specified interval.
 * The calling thread is never blocked; the returned {@link CompletionStage} completes
 * asynchronously when the action's own stage completes.
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 */
public final class ScheduledRetryScheduler implements RetryScheduler {

    private final ScheduledExecutorService executor;

    /**
     * Create a scheduler backed by the given executor.
     *
     * @param executor the scheduled executor used for delayed submission; must not be null
     */
    public ScheduledRetryScheduler(final ScheduledExecutorService executor) {
        this.executor = Require.notNull(executor, "executor");
    }

    /**
     * {@inheritDoc}
     *
     * <p>When {@code delay} is {@link BackoffDelay#isImmediate() immediate}, the action is
     * invoked on the calling thread and its stage returned directly. Otherwise, the action is
     * submitted to the backing {@link ScheduledExecutorService} and a bridge
     * {@link CompletableFuture} is returned that completes when the action's stage completes.
     */
    @Override
    public <T> CompletionStage<T> schedule(final Supplier<CompletionStage<T>> action,
                                           final BackoffDelay delay) {
        Require.notNull(action, "action");
        Require.notNull(delay, "delay");

        if (delay.isImmediate()) {
            return action.get();
        }

        final CompletableFuture<T> bridge = new CompletableFuture<>();
        executor.schedule(() -> {
            try {
                action.get().whenComplete((value, ex) -> {
                    if (ex != null) {
                        bridge.completeExceptionally(ex);
                    } else {
                        bridge.complete(value);
                    }
                });
            } catch (final Throwable t) {
                bridge.completeExceptionally(t);
            }
        }, delay.value().toMillis(), TimeUnit.MILLISECONDS);

        return bridge;
    }
}
