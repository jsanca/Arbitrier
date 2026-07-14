package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboundMessagePublisher;
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
 * <h2>Success flow</h2>
 * <pre>
 * publisher.publish(event) → [broker ack] → outboxRepository.markPublished(eventId)
 * </pre>
 *
 * <h2>Failure flow</h2>
 * <pre>
 * publisher.publish(event) → [transport failure] → outboxRepository.markFailed(eventId)
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
 * claim/lock semantics, retry counters, and backoff are intentionally deferred to later
 * slices.
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
public class DispatchOutboxMessageService {

    private final OutboundMessagePublisher publisher;
    private final OutboxRepository outboxRepository;

    public DispatchOutboxMessageService(final OutboundMessagePublisher publisher,
                                        final OutboxRepository outboxRepository) {
        this.publisher = Require.notNull(publisher, "publisher");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
    }

    /**
     * Dispatch one outbox message to the transport and update its publication status.
     *
     * <ul>
     *   <li>On success: {@link OutboxRepository#markPublished(java.util.UUID)} is called
     *       after the transport confirms delivery. If {@code markPublished} throws, the
     *       returned stage completes exceptionally.</li>
     *   <li>On asynchronous failure: {@link OutboxRepository#markFailed(java.util.UUID)}
     *       is called and the original publication exception is preserved as the stage's
     *       cause. Any {@code markFailed} exception is attached as suppressed.</li>
     *   <li>On immediate failure (throws before returning a stage): {@code markFailed} is
     *       called and the original exception is rethrown. Any {@code markFailed} exception
     *       is attached as suppressed.</li>
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

        final CompletionStage<Void> published;
        try {
            published = publisher.publish(message);
        } catch (RuntimeException immediate) {
            callMarkFailed(message, immediate);
            throw immediate;
        }

        return published
                .exceptionallyCompose(pubEx -> {
                    callMarkFailed(message, pubEx);
                    return CompletableFuture.failedFuture(pubEx);
                })
                .thenRun(() -> outboxRepository.markPublished(message.eventId()));
    }

    private void callMarkFailed(final OutboxEvent message, final Throwable pubEx) {
        try {
            outboxRepository.markFailed(message.eventId());
        } catch (RuntimeException markFailedEx) {
            pubEx.addSuppressed(markFailedEx);
        }
    }
}
