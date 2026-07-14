package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.validation.Require;

import java.util.concurrent.CompletionStage;

/**
 * Polling facade that represents one bounded Outbox dispatch cycle.
 *
 * <p>Each {@link #pollOnce()} invocation retrieves at most {@code batchSize} pending
 * messages from the repository and dispatches them sequentially via
 * {@link SequentialPendingDispatchService}. The service owns one responsibility:
 * running one cycle on demand. Scheduling, timing, and overlap prevention are
 * explicitly deferred to a later runtime configuration slice.
 *
 * <h2>Batch size</h2>
 * <p>{@code batchSize} is validated at construction time and must be positive. The
 * value is intentionally a plain constructor parameter so the class remains
 * framework-neutral. A future auto-configuration slice may bind
 * {@code arbitrier.messaging.outbox.polling.batch-size} to this constructor without
 * changing the class.
 *
 * <h2>Failure semantics</h2>
 * <p>{@code pollOnce()} propagates all failures — synchronous throws from the delegate
 * and asynchronous stage completions — unchanged. The caller (future scheduler) decides
 * how to log, isolate, or retry a failed cycle.
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
public class OutboxPollingService {

    private final SequentialPendingDispatchService sequentialDispatch;
    private final int batchSize;

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
     * <p>The returned {@link CompletionStage} has the same lifecycle as the one
     * returned by the delegate — it completes normally when all messages in the cycle
     * are dispatched, or exceptionally on the first dispatch failure.
     *
     * @return a {@link CompletionStage} representing the current polling cycle
     */
    public CompletionStage<Void> pollOnce() {
        return sequentialDispatch.dispatchPending(batchSize);
    }
}
