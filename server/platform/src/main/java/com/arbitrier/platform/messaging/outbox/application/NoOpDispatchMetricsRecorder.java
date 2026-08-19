package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;

import java.time.Duration;

/**
 * No-op {@link DispatchMetricsRecorder} used when no metrics backend is configured.
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
final class NoOpDispatchMetricsRecorder implements DispatchMetricsRecorder {

    static final NoOpDispatchMetricsRecorder INSTANCE = new NoOpDispatchMetricsRecorder();

    private NoOpDispatchMetricsRecorder() {
    }

    @Override
    public void recordStarted(final OutboxEvent message) {
    }

    @Override
    public void recordSucceeded(final OutboxEvent message, final Duration elapsed) {
    }

    @Override
    public void recordRetry(final OutboxEvent message, final int attempt) {
    }

    @Override
    public void recordDead(final OutboxEvent message, final int totalAttempts,
                           final Duration elapsed) {
    }
}
