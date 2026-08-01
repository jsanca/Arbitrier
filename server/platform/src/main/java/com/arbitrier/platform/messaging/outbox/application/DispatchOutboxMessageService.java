package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboundMessagePublisher;
import com.arbitrier.platform.messaging.retry.RetryDecision;
import com.arbitrier.platform.messaging.retry.RetryPolicy;
import com.arbitrier.platform.messaging.retry.SimpleRetryPolicy;
import com.arbitrier.platform.validation.Require;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Application-level service that dispatches a single pending {@link OutboxEvent} to its
 * transport and updates the outbox status according to the publication result.
 *
 * <p>This service coordinates two ports:
 * <ol>
 *   <li>{@link OutboundMessagePublisher} — hands the message to the active transport adapter.</li>
 *   <li>{@link OutboxRepository} — records the publication outcome.</li>
 * </ol>
 *
 * <h2>Retry behaviour</h2>
 * <p>Each attempt is evaluated by the injected {@link RetryPolicy}. The policy decides whether
 * to retry based on the attempt number and the failure; it does not introduce delays. Delay and
 * backoff are deferred to ARB-022.6.2.
 *
 * <h2>Success flow</h2>
 * <pre>
 * publisher.publish(event) → [broker ack] → outboxRepository.markPublished(eventId)
 * </pre>
 *
 * <h2>Failure flow (all retries exhausted)</h2>
 * <pre>
 * publisher.publish(event) → [transport failure] → retryPolicy.evaluate(attempt, ex) → STOP
 *                                                → outboxRepository.markFailed(eventId)
 *                                                → stage completes exceptionally
 * </pre>
 *
 * <h2>Transaction decision</h2>
 * <p>This service is intentionally <em>not</em> {@code @Transactional}. Keeping a database
 * connection open while waiting for Kafka acknowledgement would exhaust the connection pool
 * under load. {@link OutboxRepository#markPublished(java.util.UUID)} and
 * {@link OutboxRepository#markFailed(java.util.UUID)} each execute in their own short,
 * independent transaction managed by the repository implementation.
 *
 * <h2>Scope</h2>
 * <p>This service handles exactly one message per invocation. Polling, batching, scheduling,
 * claim/lock semantics, and backoff are intentionally deferred to later slices.
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
public class DispatchOutboxMessageService {

    private final OutboundMessagePublisher publisher;
    private final OutboxRepository outboxRepository;
    private final RetryPolicy retryPolicy;

    /**
     * Create a dispatch service with a custom retry policy.
     *
     * @param publisher        the transport adapter; must not be null
     * @param outboxRepository the outbox port; must not be null
     * @param retryPolicy      the retry policy; must not be null
     */
    public DispatchOutboxMessageService(final OutboundMessagePublisher publisher,
                                        final OutboxRepository outboxRepository,
                                        final RetryPolicy retryPolicy) {
        this.publisher = Require.notNull(publisher, "publisher");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.retryPolicy = Require.notNull(retryPolicy, "retryPolicy");
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
     * <p>Retries are governed by the configured {@link RetryPolicy}. On each failure the policy
     * is consulted; if it returns {@link RetryDecision#shouldRetry()} the publish call is
     * repeated immediately (no delay in this slice). When the policy returns stop, or on the
     * first successful attempt, the outcome is recorded via {@link OutboxRepository}.
     *
     * <ul>
     *   <li>On success: {@link OutboxRepository#markPublished(java.util.UUID)} is called after
     *       the transport confirms delivery.</li>
     *   <li>On final failure: {@link OutboxRepository#markFailed(java.util.UUID)} is called and
     *       the original publication exception is preserved as the stage's cause. Any
     *       {@code markFailed} exception is attached as suppressed.</li>
     *   <li>On immediate failure (throws before returning a stage): {@code markFailed} is called
     *       and the original exception is rethrown.</li>
     * </ul>
     *
     * <p>This method never calls {@code get()} or {@code join()} on any future; it is
     * non-blocking.
     *
     * @param message the event to dispatch; must not be null
     * @return a {@link CompletionStage} that completes normally after {@code markPublished},
     *         or exceptionally on transport or persistence failure
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
        } catch (RuntimeException immediate) {
            final RetryDecision decision = retryPolicy.evaluate(attempt, immediate);
            if (decision.shouldRetry()) {
                return attemptDispatch(message, attempt + 1);
            }
            callMarkFailed(message, immediate);
            throw immediate;
        }

        return published.exceptionallyCompose(pubEx -> {
            final RetryDecision decision = retryPolicy.evaluate(attempt, pubEx);
            if (decision.shouldRetry()) {
                return attemptDispatch(message, attempt + 1);
            }
            callMarkFailed(message, pubEx);
            return CompletableFuture.failedFuture(pubEx);
        });
    }

    private void callMarkFailed(final OutboxEvent message, final Throwable pubEx) {
        try {
            outboxRepository.markFailed(message.eventId());
        } catch (RuntimeException markFailedEx) {
            pubEx.addSuppressed(markFailedEx);
        }
    }
}
