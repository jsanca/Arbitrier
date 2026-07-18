package com.arbitrier.inventory.integration;

import com.arbitrier.contracts.inventory.v1.AvailabilityStatus;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityRequest;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityResponse;
import com.arbitrier.contracts.inventory.v1.InventoryAvailabilityServiceGrpc;
import com.arbitrier.contracts.inventory.v1.RequestedItem;
import com.arbitrier.contracts.inventory.v1.UnavailabilityReason;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryAvailabilityGrpcService;
import com.arbitrier.inventory.adapter.outbound.ConfigurableWarehouseAllocationPort;
import com.arbitrier.inventory.adapter.outbound.RecordingStockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
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
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end gRPC-to-PostgreSQL integration proof for {@code CheckAvailability}.
 *
 * <p>Verifies the complete production path:
 * <pre>
 *   in-process gRPC client
 *       ↓
 *   InventoryAvailabilityGrpcService  (Spring-managed, production wiring)
 *       ↓
 *   CheckInventoryAvailabilityUseCase
 *       ↓
 *   JpaInventoryAvailabilityQueryAdapter  (production adapter)
 *       ↓
 *   PostgreSQL (Testcontainers, Flyway-migrated)
 * </pre>
 *
 * <p>The production gRPC Netty server is disabled ({@code grpc.server.port=-1}) to avoid
 * port conflicts; an in-process server is constructed per test using the Spring-wired adapter.
 * {@code ConfigurableInventoryAvailabilityQueryAdapter} is never used in this test.
 *
 * <p>Layer: integration
 * <p>Module: inventory-service
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "grpc.server.port=-1"
        }
)
@Testcontainers
class InventoryAvailabilityGrpcPersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-inventory-service-schema.sql");

    /**
     * Stubs for non-persistence ports only. No {@code InventoryAvailabilityQueryPort} is
     * provided here — InventoryPersistenceConfiguration supplies the JPA production adapter.
     */
    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        WarehouseAllocationPort warehouseAllocationPort() {
            return new ConfigurableWarehouseAllocationPort();
        }

        @Bean
        @Primary
        StockReservationEventPublisher stockReservationEventPublisher() {
            return new RecordingStockReservationEventPublisher();
        }
    }

    @Autowired
    InventoryAvailabilityGrpcService grpcService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Server grpcServer;
    private ManagedChannel channel;
    private InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws IOException {
        jdbcTemplate.execute("DELETE FROM inventory_service.stock_allocations");
        jdbcTemplate.execute("DELETE FROM inventory_service.stock_reservation_lines");
        jdbcTemplate.execute("DELETE FROM inventory_service.stock_reservations");
        jdbcTemplate.execute("DELETE FROM inventory_service.inventory_stock");

        String serverName = "grpc-persistence-it-" + UUID.randomUUID();
        grpcServer = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(grpcService)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
        stub = InventoryAvailabilityServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        grpcServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    // ── AVAILABLE ─────────────────────────────────────────────────────────────

    @Test
    void available_scenario_persisted_stock_with_active_reservation() {
        insertStock("SKU-001", 10);
        insertActiveReservation("res-grpc-1", "order-grpc-1", "SKU-001", 3);

        CheckAvailabilityResponse response = stub.checkAvailability(
                CheckAvailabilityRequest.newBuilder()
                        .setRequestId("grpc-it-req-001")
                        .addItems(RequestedItem.newBuilder()
                                .setProductId("SKU-001")
                                .setQuantity(7)
                                .build())
                        .build());

        assertThat(response.getStatus()).isEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE);
        assertThat(response.getUnavailableItemsList()).isEmpty();
    }

    // ── INSUFFICIENT_STOCK ────────────────────────────────────────────────────

    @Test
    void insufficient_stock_returns_unavailable_with_correct_available_quantity() {
        insertStock("SKU-001", 10);
        insertActiveReservation("res-grpc-2", "order-grpc-2", "SKU-001", 3);

        CheckAvailabilityResponse response = stub.checkAvailability(
                CheckAvailabilityRequest.newBuilder()
                        .setRequestId("grpc-it-req-002")
                        .addItems(RequestedItem.newBuilder()
                                .setProductId("SKU-001")
                                .setQuantity(8) // available=7, needs 8 → insufficient
                                .build())
                        .build());

        assertThat(response.getStatus()).isEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE);
        assertThat(response.getUnavailableItemsList()).hasSize(1);

        var item = response.getUnavailableItems(0);
        assertThat(item.getProductId()).isEqualTo("SKU-001");
        assertThat(item.getReason()).isEqualTo(UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK);
        assertThat(item.getAvailableQuantity()).isEqualTo(7);
        assertThat(item.getRequestedQuantity()).isEqualTo(8);
    }

    // ── PRODUCT_NOT_FOUND ─────────────────────────────────────────────────────

    @Test
    void unknown_sku_returns_product_not_found() {
        CheckAvailabilityResponse response = stub.checkAvailability(
                CheckAvailabilityRequest.newBuilder()
                        .setRequestId("grpc-it-req-003")
                        .addItems(RequestedItem.newBuilder()
                                .setProductId("SKU-UNKNOWN-XYZ")
                                .setQuantity(1)
                                .build())
                        .build());

        assertThat(response.getStatus()).isEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE);
        assertThat(response.getUnavailableItemsList()).hasSize(1);

        var item = response.getUnavailableItems(0);
        assertThat(item.getProductId()).isEqualTo("SKU-UNKNOWN-XYZ");
        assertThat(item.getReason()).isEqualTo(UnavailabilityReason.UNAVAILABILITY_REASON_PRODUCT_NOT_FOUND);
        assertThat(item.getAvailableQuantity()).isZero();
    }

    // ── invalid request ───────────────────────────────────────────────────────

    @Test
    void invalid_request_returns_invalid_argument() {
        assertThatThrownBy(() -> stub.checkAvailability(
                CheckAvailabilityRequest.newBuilder()
                        .setRequestId("grpc-it-req-004")
                        // no items — validation failure
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("INVALID_ARGUMENT");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void insertStock(String productId, int onHand) {
        jdbcTemplate.update(
                "INSERT INTO inventory_service.inventory_stock (product_id, on_hand_quantity, version) VALUES (?, ?, 0)",
                productId, onHand);
    }

    private void insertActiveReservation(String reservationId, String orderId, String skuCode, int quantity) {
        jdbcTemplate.update(
                "INSERT INTO inventory_service.stock_reservations (id, order_id, status, version) VALUES (?, ?, 'RESERVED', 0)",
                reservationId, orderId);
        jdbcTemplate.update(
                "INSERT INTO inventory_service.stock_reservation_lines (reservation_id, sku_code, requested_quantity) VALUES (?, ?, ?)",
                reservationId, skuCode, quantity);
        Long lineId = jdbcTemplate.queryForObject(
                "SELECT id FROM inventory_service.stock_reservation_lines WHERE reservation_id = ? AND sku_code = ?",
                Long.class, reservationId, skuCode);
        jdbcTemplate.update(
                "INSERT INTO inventory_service.stock_allocations (line_id, warehouse_id, sku, quantity) VALUES (?, 'wh-test', ?, ?)",
                lineId, skuCode, quantity);
    }
}
