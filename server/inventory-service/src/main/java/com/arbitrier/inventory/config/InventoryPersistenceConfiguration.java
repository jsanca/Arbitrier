package com.arbitrier.inventory.config;

import com.arbitrier.inventory.adapter.outbound.persistence.JpaStockReservationRepositoryAdapter;
import com.arbitrier.inventory.adapter.outbound.persistence.SpringDataStockReservationRepository;
import com.arbitrier.inventory.adapter.outbound.persistence.StockReservationPersistenceMapper;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
