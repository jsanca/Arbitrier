package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.time.TimeProvider;
import com.arbitrier.platform.validation.Require;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Application service that atomically claims a bounded batch of pending outbox events
 * and dispatches them sequentially, enabling safe concurrent execution across multiple
 * workers.
 *
 * <h2>Claim-and-dispatch algorithm</h2>
 * <ol>
 *   <li>Call {@link OutboxRepository#claimPending} — one database transaction claims up to
 *       {@code limit} PENDING events and transitions them to CLAIMED. Two workers calling
 *       this concurrently receive disjoint result sets; the database resolves contention via
 *       {@code SELECT ... FOR UPDATE SKIP LOCKED}.</li>
 *   <li>Build a sequential {@link CompletionStage} chain from the claimed events: each event
 *       is dispatched only after the previous dispatch completes.</li>
 * </ol>
 *
 * <h2>Failure semantics</h2>
 * <p>If any event dispatch fails the chain terminates. Events remaining in the batch are
 * skipped in this cycle and remain in CLAIMED state until a retry or timeout policy
 * resets them to PENDING (deferred to ARB-022.6).
 *
 * <h2>Worker identity</h2>
 * <p>{@code workerId} identifies this instance in the claim record, enabling operators
 * to detect stale claims and correlate log entries across workers. It must be unique
 * per running instance (e.g. hostname + random suffix).
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
public class ClaimBasedBatchDispatchService {

    private final OutboxRepository outboxRepository;
    private final DispatchOutboxMessageService dispatcher;
    private final TimeProvider timeProvider;
    private final String workerId;

    /**
     * Create a dispatch service for the given worker.
     *
     * @param outboxRepository the outbox port used for claiming events; must not be null
     * @param dispatcher       the single-event dispatch delegate; must not be null
     * @param timeProvider     clock used when recording the claim timestamp; must not be null
     * @param workerId         non-blank identifier for this worker instance
     */
    public ClaimBasedBatchDispatchService(final OutboxRepository outboxRepository,
                                          final DispatchOutboxMessageService dispatcher,
                                          final TimeProvider timeProvider,
                                          final String workerId) {
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.dispatcher = Require.notNull(dispatcher, "dispatcher");
        this.timeProvider = Require.notNull(timeProvider, "timeProvider");
        this.workerId = Require.notBlank(workerId, "workerId");
    }

    /**
     * Atomically claim up to {@code limit} PENDING events and dispatch them sequentially.
     *
     * <p>If no events are claimed the returned stage is already completed normally.
     * If any dispatch fails the stage completes exceptionally after that event; remaining
     * events in the batch are not dispatched in this cycle.
     *
     * @param limit maximum number of events to claim and dispatch; must not be negative
     * @return a {@link CompletionStage} that completes normally after all claimed events are
     *         dispatched, or exceptionally on the first dispatch failure
     * @throws IllegalArgumentException if {@code limit} is negative
     */
    public CompletionStage<Void> dispatchClaimed(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative: " + limit);
        }
        if (limit == 0) {
            return CompletableFuture.completedFuture(null);
        }

        final List<OutboxEvent> claimed = outboxRepository.claimPending(workerId, timeProvider.now(), limit);
        if (claimed.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (final OutboxEvent event : claimed) {
            chain = chain.thenCompose(ignored -> dispatcher.dispatch(event));
        }
        return chain;
    }

    /** Returns the worker identifier used when claiming events. */
    public String workerId() {
        return workerId;
    }
}
