package com.arbitrier.credit.config;

import com.arbitrier.credit.adapter.outbound.persistence.CreditReservationEntity;
import com.arbitrier.credit.adapter.outbound.persistence.CreditReservationPersistenceMapper;
import com.arbitrier.credit.adapter.outbound.persistence.JpaCreditReservationRepositoryAdapter;
import com.arbitrier.credit.adapter.outbound.persistence.SpringDataCreditReservationRepository;
import com.arbitrier.credit.application.port.outbound.CreditReservationRepository;
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
 * Wires JPA persistence beans for the credit-service.
 *
 * <p>Conditional on no existing {@link CreditReservationRepository} bean.
 * Testcontainers adapter tests leave the port absent, allowing this configuration
 * to provide the JPA adapter.
 *
 * <p>Layer: config
 * <p>Module: credit-service
 */
@Configuration
@ConditionalOnMissingBean(CreditReservationRepository.class)
@EntityScan(basePackageClasses = {CreditReservationEntity.class, OutboxEventEntity.class, InboxEventEntity.class})
@EnableJpaRepositories(basePackageClasses = {SpringDataCreditReservationRepository.class, SpringDataOutboxRepository.class, SpringDataInboxRepository.class})
public class CreditPersistenceConfiguration {

    @Bean
    public CreditReservationPersistenceMapper creditReservationPersistenceMapper() {
        return new CreditReservationPersistenceMapper();
    }

    @Bean
    public CreditReservationRepository creditReservationRepository(
            SpringDataCreditReservationRepository springDataRepository,
            CreditReservationPersistenceMapper mapper) {
        return new JpaCreditReservationRepositoryAdapter(springDataRepository, mapper);
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
