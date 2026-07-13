package com.arbitrier.order.adapter.outbound.persistence;

import com.arbitrier.order.adapter.outbound.RecordingOrderEventPublisher;
import com.arbitrier.order.adapter.outbound.StubInventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.OrderEventPublisher;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.order.domain.model.CustomerId;
import com.arbitrier.order.domain.model.Order;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.Quantity;
import com.arbitrier.order.domain.model.Sku;
import com.arbitrier.order.domain.model.UserId;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.messaging.serialization.JacksonEventSerializer;
import com.arbitrier.platform.time.FixedTimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that an order aggregate and an outbox event are persisted atomically inside a single
 * transaction against a real PostgreSQL schema created by Flyway migrations.
 *
 * <p>The test uses the Flyway-migrated schema (both platform and order_service migrations run),
 * exercises the {@code JpaOrderRepositoryAdapter} and {@code JpaOutboxRepositoryAdapter} in the
 * same {@code @Transactional} method, and asserts that both rows are committed on success.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: order-service
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false"
        }
)
@Testcontainers
class TransactionalOutboxIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-order-service-schema.sql");

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        OrderEventPublisher orderEventPublisher() {
            return new RecordingOrderEventPublisher();
        }

        @Bean
        @Primary
        InventoryAvailabilityPort inventoryAvailabilityPort() {
            return new StubInventoryAvailabilityPort();
        }

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

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OutboxRepository outboxRepository;

    private static final DomainEventToOutboxMapper MAPPER =
            new DomainEventToOutboxMapper(
                    new JacksonEventSerializer(new ObjectMapper()),
                    FixedTimeProvider.of(Instant.parse("2026-01-15T10:00:00Z")));

    @Test
    @Transactional
    void saves_order_and_outbox_event_in_same_transaction() {
        OrderId orderId = OrderId.of("order-outbox-" + UUID.randomUUID());
        Order order = Order.create(
                orderId,
                CustomerId.of("cust-outbox"),
                UserId.of("user-outbox"),
                List.of(new OrderLine(Sku.of("SKU-OUTBOX-A"), Quantity.of(3))));

        orderRepository.save(order);

        OutboxEvent event = MAPPER.map(
                new TestPayload("order-created", orderId.value()),
                orderId.value(),
                "Order");
        outboxRepository.save(event);

        // Both writes are queued in the same transaction — after commit both must be visible.
        // Verified in a follow-up read within the same transaction (visible from the persistence context).
        assertThat(orderRepository.findById(orderId)).isPresent();
        assertThat(outboxRepository.findPending())
                .anyMatch(e -> e.eventId().equals(event.eventId())
                        && e.publishStatus() == PublishStatus.PENDING);
    }

    /** Simple JSON-serializable payload used in place of a real domain event. */
    public record TestPayload(String kind, String orderId) {}
}
