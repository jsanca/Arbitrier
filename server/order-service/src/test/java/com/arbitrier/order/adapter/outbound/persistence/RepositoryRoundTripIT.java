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
import com.arbitrier.order.domain.model.OrderStatus;
import com.arbitrier.order.domain.model.Quantity;
import com.arbitrier.order.domain.model.Sku;
import com.arbitrier.order.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository round-trip test: save and load a domain aggregate against a Flyway-migrated schema.
 *
 * <p>Complements FlywayMigrationIT (schema assertions) and JpaOrderRepositoryAdapterTest
 * (ddl-auto=create-drop). This test proves that the Flyway-migrated schema supports real
 * business operations with Hibernate in validate mode.
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
class RepositoryRoundTripIT {

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
    SpringDataOrderRepository springDataOrderRepository;

    private static final OrderId ORDER_ID = OrderId.of("order-rt-1");
    private static final CustomerId CUSTOMER_ID = CustomerId.of("cust-rt-1");
    private static final UserId USER_ID = UserId.of("user-rt-1");
    private static final List<OrderLine> LINES = List.of(
            new OrderLine(Sku.of("SKU-RT-A"), Quantity.of(10)),
            new OrderLine(Sku.of("SKU-RT-B"), Quantity.of(5)));

    @BeforeEach
    void clean() {
        springDataOrderRepository.deleteById(ORDER_ID.value());
    }

    @Test
    void order_round_trip_through_flyway_migrated_schema() {
        orderRepository.save(Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES));

        Order loaded = orderRepository.findById(ORDER_ID).orElseThrow();

        assertThat(loaded.id()).isEqualTo(ORDER_ID);
        assertThat(loaded.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(loaded.submittedBy()).isEqualTo(USER_ID);
        assertThat(loaded.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(loaded.lines()).hasSize(2);
        assertThat(loaded.lines()).extracting(l -> l.sku().value())
                .containsExactlyInAnyOrder("SKU-RT-A", "SKU-RT-B");
        assertThat(loaded.lines()).extracting(l -> l.quantity().value())
                .containsExactlyInAnyOrder(10, 5);
        assertThat(loaded.version()).isNotNull();
        assertThat(loaded.cancellationReason()).isNull();
    }

    @Test
    void order_status_transition_persisted_through_flyway_migrated_schema() {
        orderRepository.save(Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES));

        Order loaded = orderRepository.findById(ORDER_ID).orElseThrow();
        orderRepository.save(loaded.confirm());

        Order reloaded = orderRepository.findById(ORDER_ID).orElseThrow();

        assertThat(reloaded.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(reloaded.version()).isGreaterThan(0L);
    }
}
