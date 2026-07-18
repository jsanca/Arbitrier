package com.arbitrier.order.adapter.outbound.grpc.inventory;

import com.arbitrier.contracts.inventory.v1.InventoryAvailabilityServiceGrpc;
import com.arbitrier.inventory.InventoryServiceApplication;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryAvailabilityGrpcService;
import com.arbitrier.inventory.application.port.outbound.AllocationPlan;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;
import com.arbitrier.order.application.port.outbound.AvailabilityLineResponse;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-service gRPC integration test: Order adapter → Inventory gRPC server → PostgreSQL.
 *
 * <p>Proves the full production path:
 * <pre>
 *   GrpcInventoryAvailabilityAdapter  (Order's outbound adapter)
 *       ↓  in-process gRPC channel
 *   InventoryAvailabilityGrpcService  (Inventory's inbound adapter, Spring-managed)
 *       ↓
 *   CheckInventoryAvailabilityService  (Inventory application service)
 *       ↓
 *   JpaInventoryAvailabilityQueryAdapter  (production JPA adapter)
 *       ↓
 *   PostgreSQL  (Testcontainers, Flyway-migrated)
 * </pre>
 *
 * <p>The Inventory Spring context is booted directly. The production gRPC Netty server is
 * disabled ({@code grpc.server.port=-1}); an in-process server is constructed per test using
 * the Spring-wired gRPC service bean.
 *
 * <p>Layer: integration
 * <p>Module: order-service
 */
@SpringBootTest(
        classes = {InventoryServiceApplication.class, OrderToInventoryGrpcIntegrationIT.Config.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.properties.hibernate.default_schema=inventory_service",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.flyway.locations=classpath:db/migration/platform,classpath:db/migration/inventory_service",
                "spring.flyway.default-schema=inventory_service",
                "spring.flyway.schemas=inventory_service",
                "spring.flyway.validate-on-migrate=true",
                "spring.flyway.clean-disabled=true",
                "grpc.server.port=-1"
        }
)
@Testcontainers
class OrderToInventoryGrpcIntegrationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-inventory-service-schema.sql");

    @TestConfiguration
    static class Config {

        @Bean
        WarehouseAllocationPort warehouseAllocationPort() {
            return lines -> new AllocationPlan(List.of());
        }
    }

    @Autowired
    InventoryAvailabilityGrpcService grpcService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Server grpcServer;
    private ManagedChannel channel;
    private GrpcInventoryAvailabilityAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        jdbcTemplate.execute("DELETE FROM inventory_service.stock_allocations");
        jdbcTemplate.execute("DELETE FROM inventory_service.stock_reservation_lines");
        jdbcTemplate.execute("DELETE FROM inventory_service.stock_reservations");
        jdbcTemplate.execute("DELETE FROM inventory_service.inventory_stock");

        final String serverName = "order-to-inventory-grpc-it-" + UUID.randomUUID();
        grpcServer = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(grpcService)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();

        adapter = new GrpcInventoryAvailabilityAdapter(
                InventoryAvailabilityServiceGrpc.newBlockingStub(channel),
                Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        grpcServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void sufficient_stock_returns_all_lines_available() {
        insertStock("SKU-001", 10);
        insertActiveReservation("res-xsvc-1", "order-xsvc-1", "SKU-001", 3);

        final List<AvailabilityLineResponse> result = adapter.checkAvailability(
                List.of(new AvailabilityLineQuery("SKU-001", 7)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sku()).isEqualTo("SKU-001");
        assertThat(result.get(0).availableQuantity()).isEqualTo(7);
    }

    @Test
    void insufficient_stock_returns_server_available_quantity() {
        insertStock("SKU-001", 10);
        insertActiveReservation("res-xsvc-2", "order-xsvc-2", "SKU-001", 3);

        // on_hand=10, reserved=3 → available=7; request 8 → insufficient
        final List<AvailabilityLineResponse> result = adapter.checkAvailability(
                List.of(new AvailabilityLineQuery("SKU-001", 8)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sku()).isEqualTo("SKU-001");
        assertThat(result.get(0).availableQuantity()).isEqualTo(7);
    }

    @Test
    void unknown_sku_returns_zero_available_quantity() {
        final List<AvailabilityLineResponse> result = adapter.checkAvailability(
                List.of(new AvailabilityLineQuery("SKU-UNKNOWN-XYZ-XSVC", 1)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sku()).isEqualTo("SKU-UNKNOWN-XYZ-XSVC");
        assertThat(result.get(0).availableQuantity()).isZero();
    }

    private void insertStock(final String productId, final int onHand) {
        jdbcTemplate.update(
                "INSERT INTO inventory_service.inventory_stock (product_id, on_hand_quantity, version) VALUES (?, ?, 0)",
                productId, onHand);
    }

    private void insertActiveReservation(
            final String reservationId, final String orderId, final String sku, final int quantity) {
        jdbcTemplate.update(
                "INSERT INTO inventory_service.stock_reservations (id, order_id, status, version) VALUES (?, ?, 'RESERVED', 0)",
                reservationId, orderId);
        jdbcTemplate.update(
                "INSERT INTO inventory_service.stock_reservation_lines (reservation_id, sku_code, requested_quantity) VALUES (?, ?, ?)",
                reservationId, sku, quantity);
        final Long lineId = jdbcTemplate.queryForObject(
                "SELECT id FROM inventory_service.stock_reservation_lines WHERE reservation_id = ? AND sku_code = ?",
                Long.class, reservationId, sku);
        jdbcTemplate.update(
                "INSERT INTO inventory_service.stock_allocations (line_id, warehouse_id, sku, quantity) VALUES (?, 'wh-test', ?, ?)",
                lineId, sku, quantity);
    }
}
