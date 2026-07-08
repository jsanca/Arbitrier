package com.arbitrier.order.config;

import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderUseCase;
import com.arbitrier.order.application.port.outbound.OrderEventPublisher;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.order.application.service.SubmitCorporateBulkOrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for the order-service application layer.
 *
 * <p>Layer: config
 * <p>Module: order-service
 */
@Configuration
public class OrderServiceConfiguration {

    @Bean
    SubmitCorporateBulkOrderUseCase submitCorporateBulkOrderUseCase(
            OrderRepository orderRepository,
            OrderEventPublisher eventPublisher) {
        return new SubmitCorporateBulkOrderService(orderRepository, eventPublisher);
    }
}
