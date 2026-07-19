package com.arbitrier.order.adapter.outbound.persistence;

import com.arbitrier.order.adapter.outbound.StubInventoryAvailabilityPort;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderLineCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderUseCase;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that a failure writing the Outbox record rolls back the Order save.
 *
 * <p>The {@link SubmitCorporateBulkOrderUseCase} is {@code @Transactional}: Order save and
 * Outbox save participate in the same transaction. If the Outbox throws, Spring rolls back
 * the entire unit of work — no order row must remain in {@code order_service.orders}.
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
class OrderSagaStartRollbackIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-order-service-schema.sql");

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        OutboxRepository throwingOutboxRepository() {
            return new OutboxRepository() {
                @Override public void save(OutboxEvent e) { throw new RuntimeException("simulated outbox failure"); }
                @Override public List<OutboxEvent> findPending() { return List.of(); }
                @Override public List<OutboxEvent> findPending(int limit) { return List.of(); }
                @Override public Optional<OutboxEvent> claimEvent(UUID eventId, String workerId, Instant claimedAt) { return Optional.empty(); }
                @Override public List<OutboxEvent> claimPending(String workerId, Instant claimedAt, int limit) { return List.of(); }
                @Override public void markPublished(UUID eventId) {}
                @Override public void markFailed(UUID eventId) {}
            };
        }

        @Bean
        @Primary
        InventoryAvailabilityPort inventoryAvailabilityPort() {
            StubInventoryAvailabilityPort stub = new StubInventoryAvailabilityPort();
            stub.setUnlimited("SKU-001");
            return stub;
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
    SubmitCorporateBulkOrderUseCase submitService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void outbox_failure_rolls_back_order_persistence() {
        int before = orderCount();

        assertThatThrownBy(() -> submitService.execute(validCommand()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated outbox failure");

        assertThat(orderCount()).isEqualTo(before);
    }

    private int orderCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_service.orders", Integer.class);
        return count != null ? count : 0;
    }

    private static SubmitCorporateBulkOrderCommand validCommand() {
        return new SubmitCorporateBulkOrderCommand(
                "CUST-ROLLBACK", "USER-ROLLBACK",
                List.of(new SubmitCorporateBulkOrderLineCommand("SKU-001", 5)));
    }
}
