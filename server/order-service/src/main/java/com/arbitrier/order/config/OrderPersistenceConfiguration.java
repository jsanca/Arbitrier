package com.arbitrier.order.config;

import com.arbitrier.order.adapter.outbound.persistence.JpaOrderRepositoryAdapter;
import com.arbitrier.order.adapter.outbound.persistence.OrderPersistenceMapper;
import com.arbitrier.order.adapter.outbound.persistence.SpringDataOrderRepository;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
