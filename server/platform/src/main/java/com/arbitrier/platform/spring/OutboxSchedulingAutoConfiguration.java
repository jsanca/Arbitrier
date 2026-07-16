package com.arbitrier.platform.spring;

import com.arbitrier.platform.messaging.outbox.application.OutboxPollingService;
import com.arbitrier.platform.messaging.outbox.application.SequentialPendingDispatchService;
import com.arbitrier.platform.messaging.outbox.spring.OutboxPollingProperties;
import com.arbitrier.platform.messaging.outbox.spring.OutboxPollingScheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration that activates the Spring scheduling adapter for the Outbox polling runtime.
 *
 * <h2>Activation conditions</h2>
 * <ol>
 *   <li>{@code arbitrier.messaging.outbox.polling.enabled=true} (default) — can be set to
 *       {@code false} to disable the scheduler without removing any beans from the context.</li>
 *   <li>A {@link SequentialPendingDispatchService} bean must be present, which implies the full
 *       dispatch chain (repository, dispatcher, publisher) has been wired by the application.</li>
 * </ol>
 *
 * <h2>Beans registered</h2>
 * <ul>
 *   <li>{@link OutboxPollingService} — created with {@code batchSize} from
 *       {@link OutboxPollingProperties}.</li>
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
@ConditionalOnBean(SequentialPendingDispatchService.class)
public class OutboxSchedulingAutoConfiguration {

    /**
     * Creates the {@link OutboxPollingService} with the configured batch size.
     * Services may override this bean by supplying their own {@code OutboxPollingService}.
     */
    @Bean
    @ConditionalOnMissingBean
    OutboxPollingService outboxPollingService(
            final SequentialPendingDispatchService sequentialDispatch,
            final OutboxPollingProperties properties) {
        return new OutboxPollingService(sequentialDispatch, properties.getBatchSize());
    }

    /**
     * Creates the {@link OutboxPollingScheduler} bound to the polling service.
     * Services may override this bean by supplying their own {@code OutboxPollingScheduler}.
     */
    @Bean
    @ConditionalOnMissingBean
    OutboxPollingScheduler outboxPollingScheduler(final OutboxPollingService pollingService) {
        return new OutboxPollingScheduler(pollingService);
    }
}
