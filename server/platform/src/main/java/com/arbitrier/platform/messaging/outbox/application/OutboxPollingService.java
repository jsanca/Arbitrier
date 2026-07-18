package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.validation.Require;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polling facade that represents one bounded Outbox dispatch cycle with
 * overlap prevention.
 *
 * <p>Each {@link #pollOnce()} invocation claims at most {@code batchSize} pending
 * events from the repository and dispatches them sequentially via
 * {@link ClaimBasedBatchDispatchService}.
 *
 * <h2>Overlap prevention</h2>
 * <p>Only one polling cycle may execute at a time within the same JVM. If
 * {@code pollOnce()} is called while a previous cycle is still executing
 * asynchronously, the second invocation returns
 * {@link CompletableFuture#completedFuture(Object) completedFuture(null)}
 * immediately without starting another dispatch cycle. The running flag is always
 * cleared when a cycle finishes — whether it completes normally, exceptionally, or
 * throws synchronously — so a subsequent call may start a new cycle.
 *
 * <h2>Multi-worker safety</h2>
 * <p>Multiple JVM instances may each run their own {@code OutboxPollingService}
 * concurrently. Cross-instance duplicate dispatch is prevented by the atomic claim
 * in {@link ClaimBasedBatchDispatchService}: the database resolves contention via
 * {@code SELECT ... FOR UPDATE SKIP LOCKED}, so two workers never claim the same event.
 *
 * <h2>Batch size</h2>
 * <p>{@code batchSize} is validated at construction time and must be positive. The
 * value is intentionally a plain constructor parameter so the class remains
 * framework-neutral. {@code arbitrier.messaging.outbox.polling.batch-size} is bound
 * to this value by {@code OutboxSchedulingAutoConfiguration}.
 *
 * <h2>Failure semantics</h2>
 * <p>When a cycle does execute, all failures are propagated unchanged. The caller
 * (the scheduler) decides how to log, isolate, or retry a failed cycle.
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
public class OutboxPollingService {

    private final ClaimBasedBatchDispatchService dispatcher;
    private final int batchSize;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Create a polling facade with the given delegate and batch size.
     *
     * @param dispatcher the claim-based batch dispatch service to delegate to; must not be null
     * @param batchSize  maximum events claimed per polling cycle; must be positive
     * @throws IllegalArgumentException if {@code batchSize} is zero or negative
     */
    public OutboxPollingService(final ClaimBasedBatchDispatchService dispatcher,
                                final int batchSize) {
        this.dispatcher = Require.notNull(dispatcher, "dispatcher");
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
            cycle = dispatcher.dispatchClaimed(batchSize);
        } catch (RuntimeException immediate) {
            running.set(false);
            throw immediate;
        }
        return cycle.whenComplete((result, ex) -> running.set(false));
    }
}
