package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.validation.Require;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Application service that retrieves a bounded batch of pending outbox messages and
 * dispatches them sequentially through {@link DispatchOutboxMessageService}.
 *
 * <h2>Sequential algorithm</h2>
 * <p>A fold over the retrieved message list builds a {@link CompletionStage} chain:
 * <pre>
 * completed
 *   thenCompose → dispatch(message[0])
 *   thenCompose → dispatch(message[1])
 *   thenCompose → dispatch(message[n])
 * </pre>
 * Each {@code thenCompose} fires only after the previous stage completes normally.
 * No parallelism is introduced: messages are dispatched strictly one at a time.
 *
 * <h2>Failure semantics</h2>
 * <p>If dispatch of any message fails (either synchronously or asynchronously), the
 * chain terminates. Remaining messages in the batch are NOT dispatched. The returned
 * stage completes exceptionally with the first failure's exception. Future retry policy
 * belongs to later slices.
 *
 * <h2>Ordering</h2>
 * <p>Messages are dispatched in the order returned by
 * {@link OutboxRepository#findPending(int)}, which guarantees ascending
 * {@code occurredAt} order (oldest first).
 *
 * <h2>Scope</h2>
 * <p>This service dispatches one batch per invocation. Scheduling, polling loops,
 * retries, and claim/lock semantics are intentionally deferred to later slices.
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
public class SequentialPendingDispatchService {

    private final OutboxRepository outboxRepository;
    private final DispatchOutboxMessageService dispatcher;

    public SequentialPendingDispatchService(final OutboxRepository outboxRepository,
                                            final DispatchOutboxMessageService dispatcher) {
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.dispatcher = Require.notNull(dispatcher, "dispatcher");
    }

    /**
     * Retrieve at most {@code limit} pending messages and dispatch them sequentially.
     *
     * <p>If the repository returns no pending messages the returned stage is already
     * completed. If any dispatch fails the chain stops and the stage completes
     * exceptionally; remaining messages are skipped.
     *
     * @param limit maximum number of messages to retrieve and dispatch; must not be negative
     * @return a {@link CompletionStage} that completes normally after all messages in
     *         the batch are dispatched, or exceptionally on the first dispatch failure
     * @throws IllegalArgumentException if {@code limit} is negative
     */
    public CompletionStage<Void> dispatchPending(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative: " + limit);
        }
        if (limit == 0) {
            return CompletableFuture.completedFuture(null);
        }

        final List<OutboxEvent> pending = outboxRepository.findPending(limit);
        if (pending.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (final OutboxEvent event : pending) {
            chain = chain.thenCompose(ignored -> dispatcher.dispatch(event));
        }
        return chain;
    }
}
