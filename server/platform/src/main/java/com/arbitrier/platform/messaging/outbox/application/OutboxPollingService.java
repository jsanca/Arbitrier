package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.validation.Require;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polling facade that represents one bounded Outbox dispatch cycle with
 * overlap prevention.
 *
 * <p>Each {@link #pollOnce()} invocation retrieves at most {@code batchSize} pending
 * messages from the repository and dispatches them sequentially via
 * {@link SequentialPendingDispatchService}.
 *
 * <h2>Overlap prevention</h2>
 * <p>Only one polling cycle may execute at a time. If {@code pollOnce()} is called
 * while a previous cycle is still executing asynchronously, the second invocation
 * returns {@link CompletableFuture#completedFuture(Object) completedFuture(null)}
 * immediately without starting another dispatch cycle. The running flag is always
 * cleared when a cycle finishes — whether it completes normally, exceptionally, or
 * throws synchronously — so a subsequent call may start a new cycle.
 *
 * <h2>Batch size</h2>
 * <p>{@code batchSize} is validated at construction time and must be positive. The
 * value is intentionally a plain constructor parameter so the class remains
 * framework-neutral. A future auto-configuration slice may bind
 * {@code arbitrier.messaging.outbox.polling.batch-size} to this constructor without
 * changing the class.
 *
 * <h2>Failure semantics</h2>
 * <p>When a cycle does execute, all failures are propagated unchanged. The caller
 * (future scheduler) decides how to log, isolate, or retry a failed cycle.
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
public class OutboxPollingService {

    private final SequentialPendingDispatchService sequentialDispatch;
    private final int batchSize;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Create a polling facade with the given delegate and batch size.
     *
     * @param sequentialDispatch the sequential dispatch service to delegate to; must not be null
     * @param batchSize          maximum messages per polling cycle; must be positive (greater than zero)
     * @throws IllegalArgumentException if {@code batchSize} is zero or negative
     */
    public OutboxPollingService(final SequentialPendingDispatchService sequentialDispatch,
                                final int batchSize) {
        this.sequentialDispatch = Require.notNull(sequentialDispatch, "sequentialDispatch");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive: " + batchSize);
        }
        this.batchSize = batchSize;
    }

    /**
     * Execute one polling cycle: retrieve at most {@code batchSize} pending messages
     * and dispatch them sequentially.
     *
     * <p>If a cycle is already running, this method returns
     * {@link CompletableFuture#completedFuture(Object) completedFuture(null)} immediately
     * without invoking the delegate. The skip is intentional — the in-flight cycle
     * will process any messages that arrived before it finishes.
     *
     * <p>When a cycle does run, the running flag is cleared when the returned
     * {@link CompletionStage} completes (normally or exceptionally), allowing the next
     * invocation to start a new cycle.
     *
     * @return a {@link CompletionStage} representing the current polling cycle, or an
     *         already-completed stage if a cycle was already running
     */
    public CompletionStage<Void> pollOnce() {
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        final CompletionStage<Void> cycle;
        try {
            cycle = sequentialDispatch.dispatchPending(batchSize);
        } catch (RuntimeException immediate) {
            running.set(false);
            throw immediate;
        }
        return cycle.whenComplete((result, ex) -> running.set(false));
    }
}
