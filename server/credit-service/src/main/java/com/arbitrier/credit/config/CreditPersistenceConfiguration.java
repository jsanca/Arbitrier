package com.arbitrier.credit.config;

import com.arbitrier.credit.adapter.outbound.persistence.CreditReservationPersistenceMapper;
import com.arbitrier.credit.adapter.outbound.persistence.JpaCreditReservationRepositoryAdapter;
import com.arbitrier.credit.adapter.outbound.persistence.SpringDataCreditReservationRepository;
import com.arbitrier.credit.application.port.outbound.CreditReservationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
