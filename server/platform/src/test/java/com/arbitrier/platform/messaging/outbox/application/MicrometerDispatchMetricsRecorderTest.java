package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Verifies that {@link MicrometerDispatchMetricsRecorder} increments the correct counters and
 * records timers using a {@link SimpleMeterRegistry}.
 */
class MicrometerDispatchMetricsRecorderTest {

    private SimpleMeterRegistry registry;
    private MicrometerDispatchMetricsRecorder recorder;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        recorder = new MicrometerDispatchMetricsRecorder(registry);
    }

    @Test
    void rejects_null_registry() {
        assertThatNullPointerException()
                .isThrownBy(() -> new MicrometerDispatchMetricsRecorder(null));
    }

    // ── recordStarted ─────────────────────────────────────────────────────────

    @Test
    void recordStarted_increments_started_counter() {
        recorder.recordStarted(event());

        assertThat(counter("messaging.dispatch.started").count()).isEqualTo(1.0);
    }

    @Test
    void recordStarted_twice_counts_two() {
        recorder.recordStarted(event());
        recorder.recordStarted(event());

        assertThat(counter("messaging.dispatch.started").count()).isEqualTo(2.0);
    }

    // ── recordSucceeded ───────────────────────────────────────────────────────

    @Test
    void recordSucceeded_increments_succeeded_counter() {
        recorder.recordSucceeded(event(), Duration.ofMillis(50));

        assertThat(counter("messaging.dispatch.succeeded").count()).isEqualTo(1.0);
    }

    @Test
    void recordSucceeded_records_duration_timer_with_success_outcome() {
        recorder.recordSucceeded(event(), Duration.ofMillis(100));

        Timer timer = registry.find("messaging.dispatch.duration")
                .tag("outcome", "success").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(100.0);
    }

    // ── recordRetry ───────────────────────────────────────────────────────────

    @Test
    void recordRetry_increments_retry_counter() {
        recorder.recordRetry(event(), 1);

        assertThat(counter("messaging.dispatch.retry").count()).isEqualTo(1.0);
    }

    @Test
    void recordRetry_accumulates_across_attempts() {
        recorder.recordRetry(event(), 1);
        recorder.recordRetry(event(), 2);

        assertThat(counter("messaging.dispatch.retry").count()).isEqualTo(2.0);
    }

    // ── recordDead ────────────────────────────────────────────────────────────

    @Test
    void recordDead_increments_failed_counter() {
        recorder.recordDead(event(), 3, Duration.ofMillis(500));

        assertThat(counter("messaging.dispatch.failed").count()).isEqualTo(1.0);
    }

    @Test
    void recordDead_increments_dead_counter() {
        recorder.recordDead(event(), 3, Duration.ofMillis(500));

        assertThat(counter("messaging.dispatch.dead").count()).isEqualTo(1.0);
    }

    @Test
    void recordDead_records_duration_timer_with_dead_outcome() {
        recorder.recordDead(event(), 3, Duration.ofMillis(200));

        Timer timer = registry.find("messaging.dispatch.duration")
                .tag("outcome", "dead").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(200.0);
    }

    // ── tag consistency ───────────────────────────────────────────────────────

    @Test
    void all_counters_carry_aggregateType_tag() {
        var msg = event();
        recorder.recordStarted(msg);

        Counter c = registry.find("messaging.dispatch.started")
                .tag("aggregateType", "Order").counter();
        assertThat(c).isNotNull();
        assertThat(c.count()).isEqualTo(1.0);
    }

    @Test
    void all_counters_carry_eventType_tag() {
        var msg = event();
        recorder.recordStarted(msg);

        Counter c = registry.find("messaging.dispatch.started")
                .tag("eventType", "OrderCreatedDomainEvent").counter();
        assertThat(c).isNotNull();
        assertThat(c.count()).isEqualTo(1.0);
    }

    @Test
    void success_and_dead_timers_are_distinct() {
        recorder.recordSucceeded(event(), Duration.ofMillis(10));
        recorder.recordDead(event(), 1, Duration.ofMillis(300));

        Timer success = registry.find("messaging.dispatch.duration").tag("outcome", "success").timer();
        Timer dead    = registry.find("messaging.dispatch.duration").tag("outcome", "dead").timer();
        assertThat(success).isNotNull();
        assertThat(dead).isNotNull();
        assertThat(success.count()).isEqualTo(1);
        assertThat(dead.count()).isEqualTo(1);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Counter counter(final String name) {
        Counter c = registry.find(name).counter();
        assertThat(c).as("counter %s must exist", name).isNotNull();
        return c;
    }

    private OutboxEvent event() {
        return new OutboxEvent(
                UUID.randomUUID(), "order-001", "Order", "OrderCreatedDomainEvent",
                "{}", "JSON", Instant.now(), null, PublishStatus.PENDING, 0, null,
                null, null, MessageNature.EVENT, null, null);
    }
}
