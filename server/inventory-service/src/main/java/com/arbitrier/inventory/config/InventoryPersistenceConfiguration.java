package com.arbitrier.inventory.config;

import com.arbitrier.inventory.adapter.outbound.persistence.JpaStockReservationRepositoryAdapter;
import com.arbitrier.inventory.adapter.outbound.persistence.SpringDataStockReservationRepository;
import com.arbitrier.inventory.adapter.outbound.persistence.StockReservationPersistenceMapper;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
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
 * Wires JPA persistence beans for the inventory-service.
 *
 * <p>Conditional on no existing {@link StockReservationRepository} bean so that
 * context-load tests that supply a stub can run without PostgreSQL.
 * Testcontainers adapter tests leave the port absent, allowing this configuration
 * to provide the JPA adapter.
 *
 * <p>Layer: config
 * <p>Module: inventory-service
 */
@Configuration
@ConditionalOnMissingBean(StockReservationRepository.class)
@EntityScan(basePackageClasses = {SpringDataStockReservationRepository.class, OutboxEventEntity.class, InboxEventEntity.class})
@EnableJpaRepositories(basePackageClasses = {SpringDataStockReservationRepository.class, SpringDataOutboxRepository.class, SpringDataInboxRepository.class})
public class InventoryPersistenceConfiguration {

    @Bean
    public StockReservationPersistenceMapper stockReservationPersistenceMapper() {
        return new StockReservationPersistenceMapper();
    }

    @Bean
    public StockReservationRepository stockReservationRepository(
            SpringDataStockReservationRepository springDataRepository,
            StockReservationPersistenceMapper mapper) {
        return new JpaStockReservationRepositoryAdapter(springDataRepository, mapper);
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
