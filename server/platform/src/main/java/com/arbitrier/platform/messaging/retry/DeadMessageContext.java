package com.arbitrier.platform.messaging.retry;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.validation.Require;

import java.time.Instant;

/**
 * Immutable diagnostic context produced when an {@link OutboxEvent} exhausts all retry attempts
 * and is handed to a {@link DeadMessageHandler}.
 *
 * <p>Contains the information a handler needs to diagnose, log, or route the failure without
 * querying external systems.
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 *
 * @param message       the outbox event that permanently failed; never null
 * @param totalAttempts the 1-indexed number of dispatch attempts that were made
 * @param cause         the exception from the final attempt; never null
 * @param failedAt      the instant at which the permanent failure was recorded; never null
 */
public record DeadMessageContext(
        OutboxEvent message,
        int totalAttempts,
        Throwable cause,
        Instant failedAt) {

    public DeadMessageContext {
        Require.notNull(message, "DeadMessageContext.message");
        Require.notNull(cause, "DeadMessageContext.cause");
        Require.notNull(failedAt, "DeadMessageContext.failedAt");
        if (totalAttempts < 1) {
            throw new IllegalArgumentException(
                    "DeadMessageContext.totalAttempts must be >= 1, got: " + totalAttempts);
        }
    }
}
