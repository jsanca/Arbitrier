package com.arbitrier.platform.messaging.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Default {@link DeadMessageHandler} that logs permanent failures and completes normally.
 *
 * <p>Intended for environments where no DLQ, persistent dead-letter store, or replay
 * infrastructure is yet available. Operators can observe failures through the structured
 * log output and react manually.
 *
 * <p>The log entry includes:
 * <ul>
 *   <li>the event ID and aggregate identity from the outbox event</li>
 *   <li>total attempt count</li>
 *   <li>the root cause exception</li>
 * </ul>
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 */
public final class LoggingDeadMessageHandler implements DeadMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingDeadMessageHandler.class);

    /**
     * {@inheritDoc}
     *
     * <p>Logs at ERROR level and always returns a normally-completed stage.
     */
    @Override
    public CompletionStage<Void> handle(final DeadMessageContext context) {
        log.error(
                "Outbox message permanently failed after {} attempt(s) — eventId={} aggregateId={} aggregateType={} eventType={} failedAt={}",
                context.totalAttempts(),
                context.message().eventId(),
                context.message().aggregateId(),
                context.message().aggregateType(),
                context.message().eventType(),
                context.failedAt(),
                context.cause());
        return CompletableFuture.completedFuture(null);
    }
}
