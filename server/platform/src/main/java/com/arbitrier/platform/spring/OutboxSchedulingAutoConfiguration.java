package com.arbitrier.platform.spring;

import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboundMessagePublisher;
import com.arbitrier.platform.messaging.outbox.application.ClaimBasedBatchDispatchService;
import com.arbitrier.platform.messaging.outbox.application.DispatchOutboxMessageService;
import com.arbitrier.platform.messaging.outbox.application.OutboxPollingService;
import com.arbitrier.platform.messaging.outbox.spring.OutboxPollingProperties;
import com.arbitrier.platform.messaging.outbox.spring.OutboxPollingScheduler;
import com.arbitrier.platform.time.TimeProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Auto-configuration that activates the Spring scheduling adapter for the Outbox polling runtime.
 *
 * <h2>Activation conditions</h2>
 * <ol>
 *   <li>{@code arbitrier.messaging.outbox.polling.enabled=true} (default) — set to {@code false}
 *       to disable the scheduler without removing any beans from the context.</li>
 *   <li>Both an {@link OutboxRepository} bean and an {@link OutboundMessagePublisher} bean must
 *       be present, which implies the full persistence and transport chain has been wired by the
 *       application.</li>
 * </ol>
 *
 * <h2>Beans registered</h2>
 * <ul>
 *   <li>{@link DispatchOutboxMessageService} — coordinates a single-event publish + status update.</li>
 *   <li>{@link ClaimBasedBatchDispatchService} — atomically claims a batch of PENDING events and
 *       dispatches them sequentially via {@code DispatchOutboxMessageService}. Worker identity is
 *       taken from {@link OutboxPollingProperties#getWorkerId()}; if blank, a value is generated
 *       from the local hostname and a random suffix.</li>
 *   <li>{@link OutboxPollingService} — created with {@code batchSize} from
 *       {@link OutboxPollingProperties}; provides overlap prevention within the JVM.</li>
 *   <li>{@link OutboxPollingScheduler} — scheduling adapter that calls {@code pollOnce()}.</li>
 * </ul>
 *
 * <p>{@link EnableScheduling} is scoped to this configuration so that scheduling activation
 * is tied to the outbox polling runtime — no scheduling is enabled in contexts that never
 * start the outbox pipeline.
 *
 * <p>Layer: platform/spring
 * <p>Module: platform
 */
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(OutboxPollingProperties.class)
@ConditionalOnProperty(
        prefix = "arbitrier.messaging.outbox.polling",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnBean({OutboxRepository.class, OutboundMessagePublisher.class})
public class OutboxSchedulingAutoConfiguration {

    /**
     * Creates the single-event dispatch service that coordinates publish and status update.
     * Services may override by supplying their own {@code DispatchOutboxMessageService} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    DispatchOutboxMessageService dispatchOutboxMessageService(
            final OutboundMessagePublisher publisher,
            final OutboxRepository outboxRepository) {
        return new DispatchOutboxMessageService(publisher, outboxRepository);
    }

    /**
     * Creates the claim-based batch dispatch service.
     * The worker ID is taken from properties; if blank it is generated from the local hostname
     * and a random suffix so each JVM instance gets a stable unique identity within its lifetime.
     * Services may override by supplying their own {@code ClaimBasedBatchDispatchService} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    ClaimBasedBatchDispatchService claimBasedBatchDispatchService(
            final OutboxRepository outboxRepository,
            final DispatchOutboxMessageService dispatchService,
            final TimeProvider timeProvider,
            final OutboxPollingProperties properties) {
        String workerId = properties.getWorkerId();
        if (workerId == null || workerId.isBlank()) {
            workerId = generateWorkerId();
        }
        return new ClaimBasedBatchDispatchService(outboxRepository, dispatchService, timeProvider, workerId);
    }

    /**
     * Creates the {@link OutboxPollingService} with the configured batch size.
     * Services may override by supplying their own {@code OutboxPollingService} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    OutboxPollingService outboxPollingService(
            final ClaimBasedBatchDispatchService dispatcher,
            final OutboxPollingProperties properties) {
        return new OutboxPollingService(dispatcher, properties.getBatchSize());
    }

    /**
     * Creates the {@link OutboxPollingScheduler} bound to the polling service.
     * Services may override by supplying their own {@code OutboxPollingScheduler} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    OutboxPollingScheduler outboxPollingScheduler(final OutboxPollingService pollingService) {
        return new OutboxPollingScheduler(pollingService);
    }

    private static String generateWorkerId() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostname = "unknown";
        }
        return hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
