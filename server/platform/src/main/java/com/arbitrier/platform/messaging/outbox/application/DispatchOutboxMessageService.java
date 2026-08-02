package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboundMessagePublisher;
import com.arbitrier.platform.messaging.retry.BackoffDelay;
import com.arbitrier.platform.messaging.retry.BackoffStrategy;
import com.arbitrier.platform.messaging.retry.DeadMessageContext;
import com.arbitrier.platform.messaging.retry.DeadMessageHandler;
import com.arbitrier.platform.messaging.retry.LoggingDeadMessageHandler;
import com.arbitrier.platform.messaging.retry.RetryDecision;
import com.arbitrier.platform.messaging.retry.RetryPolicy;
import com.arbitrier.platform.messaging.retry.RetryScheduler;
import com.arbitrier.platform.messaging.retry.SimpleRetryPolicy;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Application-level service that dispatches a single pending {@link OutboxEvent} to its
 * transport and updates the outbox status according to the publication result.
 *
 * <p>This service orchestrates the full retry pipeline:
 * <ol>
 *   <li>{@link OutboundMessagePublisher} — hands the message to the active transport adapter.</li>
 *   <li>{@link OutboxRepository} — records the publication outcome.</li>
 *   <li>{@link RetryPolicy} — decides whether another attempt should occur after a failure.</li>
 *   <li>{@link BackoffStrategy} — computes the delay before the next attempt.</li>
 *   <li>{@link RetryScheduler} — submits the next attempt after the computed delay.</li>
 *   <li>{@link DeadMessageHandler} — receives permanently failed messages when the policy stops.</li>
 * </ol>
 *
 * <h2>Retry pipeline</h2>
 * <pre>
 * Dispatch Failure
 *   → RetryPolicy.evaluate()
 *       RETRY → BackoffStrategy.nextDelay(nextAttempt)
 *                 → RetryScheduler.schedule(nextAttempt, delay)
 *                     → [after delay] repeat
 *       STOP  → outboxRepository.markFailed()
 *                 → DeadMessageHandler.handle(context)
 *                     → complete exceptionally with original cause
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

    private static final Logger log = LoggerFactory.getLogger(DispatchOutboxMessageService.class);

    private final OutboundMessagePublisher publisher;
    private final OutboxRepository outboxRepository;
    private final RetryPolicy retryPolicy;
    private final BackoffStrategy backoffStrategy;
    private final RetryScheduler retryScheduler;
    private final DeadMessageHandler deadMessageHandler;

    /**
     * Create a fully configured dispatch service.
     *
     * @param publisher          the transport adapter; must not be null
     * @param outboxRepository   the outbox port; must not be null
     * @param retryPolicy        decides whether to retry after a failure; must not be null
     * @param backoffStrategy    computes the delay before the next attempt; must not be null
     * @param retryScheduler     submits attempts after the computed delay; must not be null
     * @param deadMessageHandler receives messages that permanently fail; must not be null
     */
    public DispatchOutboxMessageService(final OutboundMessagePublisher publisher,
                                        final OutboxRepository outboxRepository,
                                        final RetryPolicy retryPolicy,
                                        final BackoffStrategy backoffStrategy,
                                        final RetryScheduler retryScheduler,
                                        final DeadMessageHandler deadMessageHandler) {
        this.publisher = Require.notNull(publisher, "publisher");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.retryPolicy = Require.notNull(retryPolicy, "retryPolicy");
        this.backoffStrategy = Require.notNull(backoffStrategy, "backoffStrategy");
        this.retryScheduler = Require.notNull(retryScheduler, "retryScheduler");
        this.deadMessageHandler = Require.notNull(deadMessageHandler, "deadMessageHandler");
    }

    /**
     * Create a dispatch service with custom retry infrastructure and the default
     * {@link LoggingDeadMessageHandler}.
     *
     * @param publisher        the transport adapter; must not be null
     * @param outboxRepository the outbox port; must not be null
     * @param retryPolicy      decides whether to retry; must not be null
     * @param backoffStrategy  computes the delay before the next attempt; must not be null
     * @param retryScheduler   submits attempts after the computed delay; must not be null
     */
    public DispatchOutboxMessageService(final OutboundMessagePublisher publisher,
                                        final OutboxRepository outboxRepository,
                                        final RetryPolicy retryPolicy,
                                        final BackoffStrategy backoffStrategy,
                                        final RetryScheduler retryScheduler) {
        this(publisher, outboxRepository, retryPolicy, backoffStrategy, retryScheduler,
                new LoggingDeadMessageHandler());
    }

    /**
     * Create a dispatch service with a custom retry policy, no delay between retries, and
     * the default {@link LoggingDeadMessageHandler}.
     *
     * <p>Retries execute immediately (no scheduling). Useful in tests and configurations
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
     * <p>On each failure the {@link RetryPolicy} is consulted. On RETRY the
     * {@link BackoffStrategy} and {@link RetryScheduler} defer the next attempt. On STOP,
     * {@link OutboxRepository#markFailed} is recorded and the {@link DeadMessageHandler}
     * is invoked before the stage completes exceptionally with the original cause.
     *
     * @param message the event to dispatch; must not be null
     * @return a {@link CompletionStage} that completes normally after {@code markPublished},
     *         or exceptionally on final transport or persistence failure
     */
    public CompletionStage<Void> dispatch(final OutboxEvent message) {
        Require.notNull(message, "message");
        log.debug("Dispatching outbox message — eventId={} aggregateType={} aggregateId={} eventType={} correlationId={}",
                message.eventId(), message.aggregateType(), message.aggregateId(),
                message.eventType(), corr(message));
        return attemptDispatch(message, 1)
                .thenRun(() -> outboxRepository.markPublished(message.eventId()))
                .thenRun(() -> log.debug(
                        "Outbox message dispatched — eventId={} aggregateType={} aggregateId={} eventType={} correlationId={}",
                        message.eventId(), message.aggregateType(), message.aggregateId(),
                        message.eventType(), corr(message)));
    }

    private CompletionStage<Void> attemptDispatch(final OutboxEvent message, final int attempt) {
        final CompletionStage<Void> published;
        try {
            published = publisher.publish(message);
        } catch (final RuntimeException immediate) {
            return handleRetryOrStop(message, attempt, immediate);
        }

        return published.exceptionallyCompose(pubEx -> handleRetryOrStop(message, attempt, pubEx));
    }

    private CompletionStage<Void> handleRetryOrStop(final OutboxEvent message,
                                                     final int attempt,
                                                     final Throwable failure) {
        final RetryDecision decision = retryPolicy.evaluate(attempt, failure);
        if (decision.shouldRetry()) {
            final int next = attempt + 1;
            final BackoffDelay delay = backoffStrategy.nextDelay(next);
            log.debug("Retry scheduled for outbox message — attempt={} nextAttempt={} delayMs={} eventId={} aggregateType={} aggregateId={} eventType={} correlationId={}",
                    attempt, next, delay.value().toMillis(),
                    message.eventId(), message.aggregateType(), message.aggregateId(),
                    message.eventType(), corr(message));
            return retryScheduler.schedule(() -> attemptDispatch(message, next), delay);
        }
        log.warn("Outbox message retry attempts exhausted after {} attempt(s) — eventId={} aggregateType={} aggregateId={} eventType={} correlationId={}",
                attempt,
                message.eventId(), message.aggregateType(), message.aggregateId(),
                message.eventType(), corr(message));
        callMarkFailed(message, failure);
        return deadMessageHandler.handle(deadContext(message, attempt, failure))
                .handle((ignored, handlerEx) -> (Void) null)
                .thenCompose(ignored -> CompletableFuture.failedFuture(failure));
    }

    private static String corr(final OutboxEvent message) {
        return message.correlationId() != null ? message.correlationId() : "-";
    }

    private void callMarkFailed(final OutboxEvent message, final Throwable pubEx) {
        try {
            outboxRepository.markFailed(message.eventId());
        } catch (final RuntimeException markFailedEx) {
            pubEx.addSuppressed(markFailedEx);
        }
    }

    private DeadMessageContext deadContext(final OutboxEvent message,
                                           final int totalAttempts,
                                           final Throwable cause) {
        return new DeadMessageContext(message, totalAttempts, cause, Instant.now());
    }
}
