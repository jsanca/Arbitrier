package com.arbitrier.orchestrator.config;

import com.arbitrier.orchestrator.adapter.outbound.persistence.JpaSagaRepositoryAdapter;
import com.arbitrier.orchestrator.adapter.outbound.persistence.SagaPersistenceMapper;
import com.arbitrier.orchestrator.adapter.outbound.persistence.SpringDataSagaRepository;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
