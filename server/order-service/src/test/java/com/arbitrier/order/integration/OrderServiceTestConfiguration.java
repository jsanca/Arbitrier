package com.arbitrier.order.integration;

import com.arbitrier.order.adapter.outbound.InMemoryOrderRepository;
import com.arbitrier.order.adapter.outbound.StubInventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.platform.messaging.inbox.InboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.test.InMemoryInboxRepository;
import com.arbitrier.platform.messaging.test.InMemoryOutboxRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;

/**
 * Test-only beans for the order-service Spring context.
 *
 * <p>Provides in-memory adapters and a mock {@link JwtDecoder} that accepts any token
 * without signature verification — for use in {@code @SpringBootTest} only.
 *
 * <p>{@code CustomerAccessPort} is NOT overridden here — the production
 * {@link com.arbitrier.order.config.OrderServiceConfiguration} provides
 * {@link com.arbitrier.order.adapter.outbound.customer.AllowAllCustomerAccessAdapter},
 * which is appropriate for context-load tests.
 */
@TestConfiguration
public class OrderServiceTestConfiguration {

    @Bean
    OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    @Primary
    OutboxRepository outboxRepository() {
        return new InMemoryOutboxRepository();
    }

    @Bean
    @Primary
    InboxRepository inboxRepository() {
        return new InMemoryInboxRepository();
    }

    @Bean
    @Primary
    InventoryAvailabilityPort inventoryAvailabilityPort() {
        return new StubInventoryAvailabilityPort();
    }

    /**
     * Mock {@link JwtDecoder} that accepts any token value without cryptographic validation.
     *
     * <p>Allows the Spring Security resource-server filter chain to initialise in tests
     * without a real Keycloak instance. Never use outside of test scope.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
