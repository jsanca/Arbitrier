package com.arbitrier.order.config;

import com.arbitrier.order.adapter.outbound.customer.AllowAllCustomerAccessAdapter;
import com.arbitrier.order.application.port.inbound.PrepareCorporateBulkOrderUseCase;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderUseCase;
import com.arbitrier.order.application.port.outbound.CustomerAccessPort;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.order.application.service.PrepareCorporateBulkOrderService;
import com.arbitrier.order.application.service.SubmitCorporateBulkOrderService;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
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
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper,
            CustomerAccessPort customerAccessPort) {
        return new SubmitCorporateBulkOrderService(orderRepository, outboxRepository, outboxMapper,
                customerAccessPort);
    }

    /**
     * Provides the {@link CustomerAccessPort} implementation.
     *
     * <p>Currently wired to {@link AllowAllCustomerAccessAdapter} — a permissive placeholder
     * until the customer-membership integration is implemented.
     */
    @Bean
    CustomerAccessPort customerAccessPort() {
        return new AllowAllCustomerAccessAdapter();
    }

    // ── ARB-017 pre-saga availability negotiation ────────────────────────────

    @Bean
    PrepareCorporateBulkOrderUseCase prepareCorporateBulkOrderUseCase(
            InventoryAvailabilityPort inventoryAvailabilityPort) {
        return new PrepareCorporateBulkOrderService(inventoryAvailabilityPort);
    }
}
