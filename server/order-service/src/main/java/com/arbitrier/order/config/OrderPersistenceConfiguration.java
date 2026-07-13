package com.arbitrier.order.config;

import com.arbitrier.order.adapter.outbound.persistence.JpaOrderRepositoryAdapter;
import com.arbitrier.order.adapter.outbound.persistence.OrderPersistenceMapper;
import com.arbitrier.order.adapter.outbound.persistence.SpringDataOrderRepository;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.platform.messaging.inbox.InboxRepository;
import com.arbitrier.platform.messaging.inbox.adapter.JpaInboxRepositoryAdapter;
import com.arbitrier.platform.messaging.inbox.adapter.SpringDataInboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.adapter.JpaOutboxRepositoryAdapter;
import com.arbitrier.platform.messaging.outbox.adapter.SpringDataOutboxRepository;
import com.arbitrier.platform.time.TimeProvider;
import com.arbitrier.platform.messaging.inbox.adapter.InboxEventEntity;
import com.arbitrier.platform.messaging.outbox.adapter.OutboxEventEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Wires JPA persistence beans for the order-service.
 *
 * <p>Conditional on no existing {@link OrderRepository} bean so that context-load tests
 * that supply an in-memory repository continue to work without PostgreSQL.
 * Testcontainers adapter tests leave {@link OrderRepository} absent from their
 * {@code @TestConfiguration}, allowing this configuration to provide the JPA adapter.
 *
 * <p>Layer: config
 * <p>Module: order-service
 */
@Configuration
@ConditionalOnMissingBean(OrderRepository.class)
@EntityScan(basePackageClasses = {SpringDataOrderRepository.class, OutboxEventEntity.class, InboxEventEntity.class})
@EnableJpaRepositories(basePackageClasses = {SpringDataOrderRepository.class, SpringDataOutboxRepository.class, SpringDataInboxRepository.class})
public class OrderPersistenceConfiguration {

    @Bean
    public OrderPersistenceMapper orderPersistenceMapper() {
        return new OrderPersistenceMapper();
    }

    @Bean
    public OrderRepository orderRepository(SpringDataOrderRepository springDataRepository,
                                           OrderPersistenceMapper mapper) {
        return new JpaOrderRepositoryAdapter(springDataRepository, mapper);
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
