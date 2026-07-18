package com.arbitrier.platform.messaging.outbox.spring;

import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboundMessagePublisher;
import com.arbitrier.platform.messaging.outbox.application.ClaimBasedBatchDispatchService;
import com.arbitrier.platform.messaging.outbox.application.DispatchOutboxMessageService;
import com.arbitrier.platform.messaging.outbox.application.OutboxPollingService;
import com.arbitrier.platform.spring.OutboxSchedulingAutoConfiguration;
import com.arbitrier.platform.time.FixedTimeProvider;
import com.arbitrier.platform.time.TimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link OutboxSchedulingAutoConfiguration} using
 * {@link ApplicationContextRunner}. No Kafka, no database.
 */
class OutboxSchedulingAutoConfigurationTest {

    private final OutboxRepository mockRepository = mock(OutboxRepository.class);
    private final OutboundMessagePublisher mockPublisher = mock(OutboundMessagePublisher.class);
    private final TimeProvider fixedClock = FixedTimeProvider.of(Instant.parse("2026-07-17T10:00:00Z"));

    /** Use a long initial delay so @Scheduled never fires during the test run. */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxSchedulingAutoConfiguration.class))
            .withPropertyValues(
                    "arbitrier.messaging.outbox.polling.initial-delay-ms=3600000",
                    "arbitrier.messaging.outbox.polling.worker-id=test-worker-01"
            )
            .withBean(OutboxRepository.class, () -> mockRepository)
            .withBean(OutboundMessagePublisher.class, () -> mockPublisher)
            .withBean(TimeProvider.class, () -> fixedClock);

    // ── presence when enabled ─────────────────────────────────────────────────

    @Test
    void full_pipeline_registered_when_enabled_by_default() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(DispatchOutboxMessageService.class);
            assertThat(ctx).hasSingleBean(ClaimBasedBatchDispatchService.class);
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
    void pipeline_absent_when_disabled() {
        contextRunner
                .withPropertyValues("arbitrier.messaging.outbox.polling.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxPollingScheduler.class);
                    assertThat(ctx).doesNotHaveBean(OutboxPollingService.class);
                    assertThat(ctx).doesNotHaveBean(ClaimBasedBatchDispatchService.class);
                    assertThat(ctx).doesNotHaveBean(DispatchOutboxMessageService.class);
                });
    }

    // ── absence without required beans ───────────────────────────────────────

    @Test
    void pipeline_absent_when_no_outbox_repository() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxSchedulingAutoConfiguration.class))
                .withBean(OutboundMessagePublisher.class, () -> mockPublisher)
                .withBean(TimeProvider.class, () -> fixedClock)
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxPollingScheduler.class);
                    assertThat(ctx).doesNotHaveBean(ClaimBasedBatchDispatchService.class);
                });
    }

    @Test
    void pipeline_absent_when_no_publisher() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxSchedulingAutoConfiguration.class))
                .withBean(OutboxRepository.class, () -> mockRepository)
                .withBean(TimeProvider.class, () -> fixedClock)
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxPollingScheduler.class);
                    assertThat(ctx).doesNotHaveBean(ClaimBasedBatchDispatchService.class);
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

    @Test
    void explicit_worker_id_is_used() {
        contextRunner.run(ctx -> {
            var claimService = ctx.getBean(ClaimBasedBatchDispatchService.class);
            assertThat(claimService.workerId()).isEqualTo("test-worker-01");
        });
    }

    @Test
    void auto_generated_worker_id_is_non_blank_when_not_configured() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxSchedulingAutoConfiguration.class))
                .withPropertyValues("arbitrier.messaging.outbox.polling.initial-delay-ms=3600000")
                .withBean(OutboxRepository.class, () -> mockRepository)
                .withBean(OutboundMessagePublisher.class, () -> mockPublisher)
                .withBean(TimeProvider.class, () -> fixedClock)
                .run(ctx -> {
                    var claimService = ctx.getBean(ClaimBasedBatchDispatchService.class);
                    assertThat(claimService.workerId()).isNotBlank();
                });
    }

    // ── batch size propagation ────────────────────────────────────────────────

    @Test
    void batch_size_is_propagated_to_claim_dispatch_service() {
        when(mockRepository.claimPending(anyString(), any(), anyInt())).thenReturn(List.of());

        contextRunner
                .withPropertyValues("arbitrier.messaging.outbox.polling.batch-size=42")
                .run(ctx -> {
                    var scheduler = ctx.getBean(OutboxPollingScheduler.class);
                    scheduler.poll();
                    verify(mockRepository).claimPending(anyString(), any(), eq(42));
                });
    }

    @Test
    void default_batch_size_is_propagated_to_claim_dispatch_service() {
        when(mockRepository.claimPending(anyString(), any(), anyInt())).thenReturn(List.of());

        contextRunner.run(ctx -> {
            var scheduler = ctx.getBean(OutboxPollingScheduler.class);
            scheduler.poll();
            verify(mockRepository).claimPending(anyString(), any(), eq(100));
        });
    }
}
