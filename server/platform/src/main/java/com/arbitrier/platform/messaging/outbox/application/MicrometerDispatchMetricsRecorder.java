package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.validation.Require;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

/**
 * Micrometer-backed {@link DispatchMetricsRecorder} for the outbox dispatch pipeline.
 *
 * <h2>Counters</h2>
 * <ul>
 *   <li>{@code messaging.dispatch.started} — one per {@link DispatchOutboxMessageService#dispatch} call</li>
 *   <li>{@code messaging.dispatch.succeeded} — one per successful publication + markPublished</li>
 *   <li>{@code messaging.dispatch.retry} — one per retry attempt scheduled</li>
 *   <li>{@code messaging.dispatch.failed} — one per terminal failure (STOP decision)</li>
 *   <li>{@code messaging.dispatch.dead} — one per dead-message handler invocation</li>
 * </ul>
 *
 * <h2>Timers</h2>
 * <ul>
 *   <li>{@code messaging.dispatch.duration} — wall-clock time from first attempt to terminal outcome;
 *       tagged {@code outcome=success} or {@code outcome=dead}</li>
 * </ul>
 *
 * <h2>Tags</h2>
 * <p>Every meter is tagged with {@code aggregateType} and {@code eventType}.
 * High-cardinality identifiers ({@code eventId}, {@code aggregateId}, {@code correlationId})
 * are intentionally excluded.
 *
 * <p>Layer: platform/messaging/outbox/application
 * <p>Module: platform
 */
public final class MicrometerDispatchMetricsRecorder implements DispatchMetricsRecorder {

    private static final String METRIC_STARTED   = "messaging.dispatch.started";
    private static final String METRIC_SUCCEEDED  = "messaging.dispatch.succeeded";
    private static final String METRIC_RETRY      = "messaging.dispatch.retry";
    private static final String METRIC_FAILED     = "messaging.dispatch.failed";
    private static final String METRIC_DEAD       = "messaging.dispatch.dead";
    private static final String METRIC_DURATION   = "messaging.dispatch.duration";

    private static final String TAG_AGGREGATE_TYPE = "aggregateType";
    private static final String TAG_EVENT_TYPE     = "eventType";
    private static final String TAG_OUTCOME        = "outcome";

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_DEAD    = "dead";

    private final MeterRegistry registry;

    /**
     * @param registry the Micrometer registry to publish to; must not be null
     */
    public MicrometerDispatchMetricsRecorder(final MeterRegistry registry) {
        this.registry = Require.notNull(registry, "registry");
    }

    @Override
    public void recordStarted(final OutboxEvent message) {
        counter(METRIC_STARTED, message).increment();
    }

    @Override
    public void recordSucceeded(final OutboxEvent message, final Duration elapsed) {
        counter(METRIC_SUCCEEDED, message).increment();
        timer(METRIC_DURATION, message, OUTCOME_SUCCESS).record(elapsed);
    }

    @Override
    public void recordRetry(final OutboxEvent message, final int attempt) {
        counter(METRIC_RETRY, message).increment();
    }

    @Override
    public void recordDead(final OutboxEvent message, final int totalAttempts,
                           final Duration elapsed) {
        counter(METRIC_FAILED, message).increment();
        counter(METRIC_DEAD, message).increment();
        timer(METRIC_DURATION, message, OUTCOME_DEAD).record(elapsed);
    }

    private Counter counter(final String name, final OutboxEvent message) {
        return Counter.builder(name)
                .tag(TAG_AGGREGATE_TYPE, message.aggregateType())
                .tag(TAG_EVENT_TYPE, message.eventType())
                .register(registry);
    }

    private Timer timer(final String name, final OutboxEvent message, final String outcome) {
        return Timer.builder(name)
                .tag(TAG_AGGREGATE_TYPE, message.aggregateType())
                .tag(TAG_EVENT_TYPE, message.eventType())
                .tag(TAG_OUTCOME, outcome)
                .register(registry);
    }
}
