package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboundMessagePublisher;
import com.arbitrier.platform.messaging.retry.BackoffDelay;
import com.arbitrier.platform.messaging.retry.BackoffStrategy;
import com.arbitrier.platform.messaging.retry.RetryDecision;
import com.arbitrier.platform.messaging.retry.RetryPolicy;
import com.arbitrier.platform.messaging.retry.RetryScheduler;
import com.arbitrier.platform.messaging.retry.SimpleRetryPolicy;
import com.arbitrier.platform.validation.Require;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Application-level service that dispatches a single pending {@link OutboxEvent} to its
 * transport and updates the outbox status according to the publication result.
 *
 * <p>This service coordinates two ports and three retry-infrastructure collaborators:
 * <ol>
 *   <li>{@link OutboundMessagePublisher} — hands the message to the active transport adapter.</li>
 *   <li>{@link OutboxRepository} — records the publication outcome.</li>
 *   <li>{@link RetryPolicy} — decides whether another attempt should occur after a failure.</li>
 *   <li>{@link BackoffStrategy} — computes the delay before the next attempt.</li>
 *   <li>{@link RetryScheduler} — submits the next attempt after the computed delay without
 *       blocking the calling thread.</li>
 * </ol>
 *
 * <h2>Retry pipeline</h2>
 * <pre>
 * Dispatch Failure
 *   → RetryPolicy.evaluate()
 *       STOP  → markFailed, complete exceptionally
 *       RETRY → BackoffStrategy.nextDelay(nextAttempt)
 *                 → RetryScheduler.schedule(nextAttempt, delay)
 *                     → [after delay] repeat
 * </pre>
 *
 * <h2>Success flow</h2>
 * <pre>
 * publisher.publish(event) → [broker ack] → outboxRepository.markPublished(eventId)
 * </pre>
 *
 * <h2>Transaction decision</h2>
 * <p>This service is intentionally <em>not</em> {@code @Transactional}. Keeping a database
 * connection open while waiting for Kafka acknowledgement would exhaust the connection pool
 * under load. {@link OutboxRepository#markPublished(java.util.UUID)} and
 * {@link OutboxRepository#markFailed(java.util.UUID)} each execute in their own short,
 * independent transaction managed by the repository implementation.
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
public class DispatchOutboxMessageService {

    private final OutboundMessagePublisher publisher;
    private final OutboxRepository outboxRepository;
    private final RetryPolicy retryPolicy;
    private final BackoffStrategy backoffStrategy;
    private final RetryScheduler retryScheduler;

    /**
     * Create a fully configured dispatch service.
     *
     * @param publisher        the transport adapter; must not be null
     * @param outboxRepository the outbox port; must not be null
     * @param retryPolicy      decides whether to retry after a failure; must not be null
     * @param backoffStrategy  computes the delay before the next attempt; must not be null
     * @param retryScheduler   submits attempts after the computed delay; must not be null
     */
    public DispatchOutboxMessageService(final OutboundMessagePublisher publisher,
                                        final OutboxRepository outboxRepository,
                                        final RetryPolicy retryPolicy,
                                        final BackoffStrategy backoffStrategy,
                                        final RetryScheduler retryScheduler) {
        this.publisher = Require.notNull(publisher, "publisher");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.retryPolicy = Require.notNull(retryPolicy, "retryPolicy");
        this.backoffStrategy = Require.notNull(backoffStrategy, "backoffStrategy");
        this.retryScheduler = Require.notNull(retryScheduler, "retryScheduler");
    }

    /**
     * Create a dispatch service with a custom retry policy and no delay between retries.
     *
     * <p>Retries are executed immediately (no scheduling). Useful in tests and configurations
     * where delay is managed externally.
     *
     * @param publisher        the transport adapter; must not be null
     * @param outboxRepository the outbox port; must not be null
     * @param retryPolicy      decides whether to retry; must not be null
     */
    public DispatchOutboxMessageService(final OutboundMessagePublisher publisher,
                                        final OutboxRepository outboxRepository,
                                        final RetryPolicy retryPolicy) {
        this(publisher, outboxRepository, retryPolicy,
                attempt -> BackoffDelay.ZERO,
                new RetryScheduler() {
                    @Override
                    public <T> CompletionStage<T> schedule(
                            final Supplier<CompletionStage<T>> action,
                            final BackoffDelay delay) {
                        return action.get();
                    }
                });
    }

    /**
     * Create a dispatch service that attempts each message exactly once (no retry).
     *
     * @param publisher        the transport adapter; must not be null
     * @param outboxRepository the outbox port; must not be null
     */
    public DispatchOutboxMessageService(final OutboundMessagePublisher publisher,
                                        final OutboxRepository outboxRepository) {
        this(publisher, outboxRepository, new SimpleRetryPolicy(1));
    }

    /**
     * Dispatch one outbox message to the transport and update its publication status.
     *
     * <p>On each failure the {@link RetryPolicy} is consulted. If it returns retry, the
     * {@link BackoffStrategy} computes the next delay and the {@link RetryScheduler} submits
     * the next attempt after that delay — without blocking the calling thread. When the policy
     * returns stop, {@link OutboxRepository#markFailed} is called and the stage completes
     * exceptionally.
     *
     * @param message the event to dispatch; must not be null
     * @return a {@link CompletionStage} that completes normally after {@code markPublished},
     *         or exceptionally on final transport or persistence failure
     */
    public CompletionStage<Void> dispatch(final OutboxEvent message) {
        Require.notNull(message, "message");
        return attemptDispatch(message, 1)
                .thenRun(() -> outboxRepository.markPublished(message.eventId()));
    }

    private CompletionStage<Void> attemptDispatch(final OutboxEvent message, final int attempt) {
        final CompletionStage<Void> published;
        try {
            published = publisher.publish(message);
        } catch (final RuntimeException immediate) {
            final RetryDecision decision = retryPolicy.evaluate(attempt, immediate);
            if (decision.shouldRetry()) {
                final int next = attempt + 1;
                final BackoffDelay delay = backoffStrategy.nextDelay(next);
                return retryScheduler.schedule(() -> attemptDispatch(message, next), delay);
            }
            callMarkFailed(message, immediate);
            throw immediate;
        }

        return published.exceptionallyCompose(pubEx -> {
            final RetryDecision decision = retryPolicy.evaluate(attempt, pubEx);
            if (decision.shouldRetry()) {
                final int next = attempt + 1;
                final BackoffDelay delay = backoffStrategy.nextDelay(next);
                return retryScheduler.schedule(() -> attemptDispatch(message, next), delay);
            }
            callMarkFailed(message, pubEx);
            return CompletableFuture.failedFuture(pubEx);
        });
    }

    private void callMarkFailed(final OutboxEvent message, final Throwable pubEx) {
        try {
            outboxRepository.markFailed(message.eventId());
        } catch (final RuntimeException markFailedEx) {
            pubEx.addSuppressed(markFailedEx);
        }
    }
}
