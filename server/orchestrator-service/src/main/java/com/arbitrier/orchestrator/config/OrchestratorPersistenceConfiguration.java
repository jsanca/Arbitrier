package com.arbitrier.orchestrator.config;

import com.arbitrier.orchestrator.adapter.outbound.persistence.JpaSagaRepositoryAdapter;
import com.arbitrier.orchestrator.adapter.outbound.persistence.SagaPersistenceMapper;
import com.arbitrier.orchestrator.adapter.outbound.persistence.SpringDataSagaRepository;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.platform.messaging.inbox.InboxRepository;
import com.arbitrier.platform.messaging.inbox.adapter.JpaInboxRepositoryAdapter;
import com.arbitrier.platform.messaging.inbox.adapter.SpringDataInboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.adapter.JpaOutboxRepositoryAdapter;
import com.arbitrier.platform.messaging.outbox.adapter.SpringDataOutboxRepository;
import com.arbitrier.platform.messaging.inbox.adapter.InboxEventEntity;
import com.arbitrier.platform.messaging.outbox.adapter.OutboxEventEntity;
import com.arbitrier.platform.time.TimeProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Wires JPA persistence beans for the orchestrator-service.
 *
 * <p>Conditional on no existing {@link SagaRepository} bean.
 * Testcontainers adapter tests leave the port absent, allowing this configuration
 * to provide the JPA adapter.
 *
 * <p>Layer: config
 * <p>Module: orchestrator-service
 */
@Configuration
@ConditionalOnMissingBean(SagaRepository.class)
@EntityScan(basePackageClasses = {SpringDataSagaRepository.class, OutboxEventEntity.class, InboxEventEntity.class})
@EnableJpaRepositories(basePackageClasses = {SpringDataSagaRepository.class, SpringDataOutboxRepository.class, SpringDataInboxRepository.class})
public class OrchestratorPersistenceConfiguration {

    @Bean
    public SagaPersistenceMapper sagaPersistenceMapper() {
        return new SagaPersistenceMapper();
    }

    @Bean
    public SagaRepository sagaRepository(SpringDataSagaRepository springDataRepository,
                                         SagaPersistenceMapper mapper) {
        return new JpaSagaRepositoryAdapter(springDataRepository, mapper);
    }

    @Bean
    @ConditionalOnMissingBean(OutboxRepository.class)
    public OutboxRepository outboxRepository(SpringDataOutboxRepository springDataOutboxRepository,
                                              TimeProvider timeProvider) {
        return new JpaOutboxRepositoryAdapter(springDataOutboxRepository, timeProvider);
    }

    @Bean
    @ConditionalOnMissingBean(InboxRepository.class)
    public InboxRepository inboxRepository(SpringDataInboxRepository springDataInboxRepository,
                                            TimeProvider timeProvider) {
        return new JpaInboxRepositoryAdapter(springDataInboxRepository, timeProvider);
    }
}
