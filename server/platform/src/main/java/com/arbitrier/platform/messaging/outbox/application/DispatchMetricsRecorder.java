package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;

import java.time.Duration;

/**
 * Port for recording outbox dispatch lifecycle metrics.
 *
 * <p>Implementations translate the dispatcher's lifecycle events into the metrics
 * backend of choice (Micrometer, no-op, etc.). Implementations must not throw.
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
public interface DispatchMetricsRecorder {

    /**
     * A dispatch attempt has started (first attempt only, not retries).
     *
     * @param message the event being dispatched
     */
    void recordStarted(OutboxEvent message);

    /**
     * A dispatch completed successfully and the outbox record was marked published.
     *
     * @param message the dispatched event
     * @param elapsed total wall-clock time from first attempt to successful publication
     */
    void recordSucceeded(OutboxEvent message, Duration elapsed);

    /**
     * A retry was scheduled for a previously failed attempt.
     *
     * @param message the event being retried
     * @param attempt the attempt number that just failed (1-based)
     */
    void recordRetry(OutboxEvent message, int attempt);

    /**
     * The retry policy reached STOP: {@code markFailed} was recorded and the
     * {@link com.arbitrier.platform.messaging.retry.DeadMessageHandler} was invoked.
     *
     * <p>Implementations are expected to increment both a {@code failed} and a
     * {@code dead} counter to allow independent querying.
     *
     * @param message       the permanently failed event
     * @param totalAttempts total number of delivery attempts including the final one
     * @param elapsed       total wall-clock time from first attempt to final failure
     */
    void recordDead(OutboxEvent message, int totalAttempts, Duration elapsed);
}
