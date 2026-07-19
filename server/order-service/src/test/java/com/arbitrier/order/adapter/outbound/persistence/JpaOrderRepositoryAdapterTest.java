package com.arbitrier.order.adapter.outbound.persistence;

import com.arbitrier.order.adapter.outbound.StubInventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.order.domain.model.CancellationReason;
import com.arbitrier.order.domain.model.CustomerId;
import com.arbitrier.order.domain.model.Order;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.OrderStatus;
import com.arbitrier.order.domain.model.Quantity;
import com.arbitrier.order.domain.model.Sku;
import com.arbitrier.order.domain.model.UserId;
import com.arbitrier.platform.error.ApplicationProblemException;
import com.arbitrier.platform.error.PersistenceProblemCode;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the JPA order persistence adapter using Testcontainers PostgreSQL.
 *
 * <p>Verifies save/load round-trip, status update, line replacement, and
 * optimistic-lock conflict detection.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=false"
        }
)
@Testcontainers
class JpaOrderRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-order-service-schema.sql");

    /** Stubs for non-persistence ports — OrderRepository is provided by JPA config. */
    @TestConfiguration
    static class Config {

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

    private static final OrderId ORDER_ID = OrderId.of("order-tc-1");

    @BeforeEach
    void clean() {
        springDataOrderRepository.deleteById(ORDER_ID.value());
    }
    private static final CustomerId CUSTOMER_ID = CustomerId.of("cust-1");
    private static final UserId USER_ID = UserId.of("user-1");
    private static final List<OrderLine> LINES = List.of(
            new OrderLine(Sku.of("SKU-A"), Quantity.of(10)),
            new OrderLine(Sku.of("SKU-B"), Quantity.of(5)));

    // ── save and load ─────────────────────────────────────────────────────────

    @Test
    void save_and_load_new_order_with_lines() {
        orderRepository.save(Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES));

        Optional<Order> loaded = orderRepository.findById(ORDER_ID);

        assertThat(loaded).isPresent();
        Order result = loaded.get();
        assertThat(result.id()).isEqualTo(ORDER_ID);
        assertThat(result.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.lines()).hasSize(2);
        assertThat(result.version()).isNotNull();
    }

    @Test
    void save_preserves_all_order_lines() {
        orderRepository.save(Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES));

        Order loaded = orderRepository.findById(ORDER_ID).orElseThrow();

        assertThat(loaded.lines()).extracting(l -> l.sku().value())
                .containsExactlyInAnyOrder("SKU-A", "SKU-B");
    }

    // ── status update ─────────────────────────────────────────────────────────

    @Test
    void save_updates_order_status() {
        orderRepository.save(Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES));

        Order loaded = orderRepository.findById(ORDER_ID).orElseThrow();
        orderRepository.save(loaded.confirm());

        Order updated = orderRepository.findById(ORDER_ID).orElseThrow();
        assertThat(updated.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void save_updates_cancellation_reason() {
        orderRepository.save(Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES));

        Order loaded = orderRepository.findById(ORDER_ID).orElseThrow();
        orderRepository.save(loaded.cancel(CancellationReason.CUSTOMER_CANCELLED));

        Order updated = orderRepository.findById(ORDER_ID).orElseThrow();
        assertThat(updated.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(updated.cancellationReason()).isEqualTo(CancellationReason.CUSTOMER_CANCELLED);
    }

    // ── optimistic lock conflict ──────────────────────────────────────────────

    @Test
    void optimistic_lock_conflict_throws_typed_exception() {
        orderRepository.save(Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES));

        Order staleLoad = orderRepository.findById(ORDER_ID).orElseThrow();
        // Advance DB version by saving once with the current version
        orderRepository.save(staleLoad.confirm());

        // staleLoad still carries version 0; DB is now at version 1
        assertThatThrownBy(() -> orderRepository.save(staleLoad.confirm()))
                .isInstanceOf(ApplicationProblemException.class)
                .satisfies(ex -> assertThat(((ApplicationProblemException) ex).code())
                        .isEqualTo(PersistenceProblemCode.OPTIMISTIC_LOCK_CONFLICT));
    }

    // ── findById not found ────────────────────────────────────────────────────

    @Test
    void findById_returns_empty_for_unknown_id() {
        Optional<Order> result = orderRepository.findById(OrderId.of("not-existing"));

        assertThat(result).isEmpty();
    }

    // ── line replacement ──────────────────────────────────────────────────────

    @Test
    void save_replaces_lines_atomically_on_update() {
        orderRepository.save(Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES));

        Order loaded = orderRepository.findById(ORDER_ID).orElseThrow();
        List<OrderLine> newLines = List.of(new OrderLine(Sku.of("SKU-C"), Quantity.of(2)));
        Order updated = Order.reconstruct(loaded.id(), loaded.customerId(), loaded.submittedBy(),
                newLines, loaded.status(), null, loaded.version());
        orderRepository.save(updated);

        Order reloaded = orderRepository.findById(ORDER_ID).orElseThrow();
        assertThat(reloaded.lines()).hasSize(1);
        assertThat(reloaded.lines().get(0).sku().value()).isEqualTo("SKU-C");
    }
}
