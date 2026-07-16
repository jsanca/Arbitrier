package com.arbitrier.platform.messaging.outbox.spring;

import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.application.DispatchOutboxMessageService;
import com.arbitrier.platform.messaging.outbox.application.OutboxPollingService;
import com.arbitrier.platform.messaging.outbox.application.SequentialPendingDispatchService;
import com.arbitrier.platform.spring.OutboxSchedulingAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link OutboxSchedulingAutoConfiguration} using
 * {@link ApplicationContextRunner}. No Kafka, no database.
 */
class OutboxSchedulingAutoConfigurationTest {

    private final SequentialPendingDispatchService mockDispatch = stubbedDispatch();

    /** Use a long initial delay so @Scheduled never fires during the test run. */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxSchedulingAutoConfiguration.class))
            .withPropertyValues("arbitrier.messaging.outbox.polling.initial-delay-ms=3600000")
            .withBean(SequentialPendingDispatchService.class, () -> mockDispatch);

    // ── presence when enabled ─────────────────────────────────────────────────

    @Test
    void scheduler_and_polling_service_registered_when_enabled_by_default() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(OutboxPollingService.class);
            assertThat(ctx).hasSingleBean(OutboxPollingScheduler.class);
        });
    }

    @Test
    void scheduler_registered_when_enabled_explicitly() {
        contextRunner
                .withPropertyValues("arbitrier.messaging.outbox.polling.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(OutboxPollingScheduler.class));
    }

    // ── absence when disabled ─────────────────────────────────────────────────

    @Test
    void scheduler_absent_when_disabled() {
        contextRunner
                .withPropertyValues("arbitrier.messaging.outbox.polling.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxPollingScheduler.class);
                    assertThat(ctx).doesNotHaveBean(OutboxPollingService.class);
                });
    }

    // ── absence without dispatch service ─────────────────────────────────────

    @Test
    void scheduler_absent_when_no_sequential_dispatch_service() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxSchedulingAutoConfiguration.class))
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxPollingScheduler.class);
                    assertThat(ctx).doesNotHaveBean(OutboxPollingService.class);
                });
    }

    // ── property binding ──────────────────────────────────────────────────────

    @Test
    void default_properties_are_bound() {
        contextRunner.run(ctx -> {
            var props = ctx.getBean(OutboxPollingProperties.class);
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getBatchSize()).isEqualTo(100);
            assertThat(props.getFixedDelayMs()).isEqualTo(10_000L);
        });
    }

    @Test
    void custom_batch_size_is_bound() {
        contextRunner
                .withPropertyValues("arbitrier.messaging.outbox.polling.batch-size=42")
                .run(ctx -> {
                    var props = ctx.getBean(OutboxPollingProperties.class);
                    assertThat(props.getBatchSize()).isEqualTo(42);
                });
    }

    // ── batch size propagation ────────────────────────────────────────────────

    @Test
    void batch_size_is_propagated_to_polling_service() {
        contextRunner
                .withPropertyValues("arbitrier.messaging.outbox.polling.batch-size=42")
                .run(ctx -> {
                    var scheduler = ctx.getBean(OutboxPollingScheduler.class);
                    scheduler.poll();
                    verify(mockDispatch).dispatchPending(42);
                });
    }

    @Test
    void default_batch_size_is_propagated_to_polling_service() {
        contextRunner.run(ctx -> {
            var scheduler = ctx.getBean(OutboxPollingScheduler.class);
            scheduler.poll();
            verify(mockDispatch).dispatchPending(100);
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static SequentialPendingDispatchService stubbedDispatch() {
        var mock = mock(SequentialPendingDispatchService.class,
                org.mockito.Mockito.withSettings().defaultAnswer(invocation -> null));
        when(mock.dispatchPending(anyInt())).thenReturn(CompletableFuture.completedFuture(null));
        return mock;
    }
}
