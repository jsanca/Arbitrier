package com.arbitrier.inventory.adapter.outbound.persistence;

import com.arbitrier.inventory.adapter.outbound.ConfigurableWarehouseAllocationPort;
import com.arbitrier.inventory.adapter.outbound.RecordingStockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.InventoryAvailabilityQueryPort;
import com.arbitrier.inventory.application.port.outbound.InventoryAvailabilitySnapshot;
import com.arbitrier.inventory.application.port.outbound.StockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JpaInventoryAvailabilityQueryAdapter} against a real PostgreSQL schema.
 *
 * <p>Verifies availability calculation across all scenarios:
 * stock-only, partial reservation, full reservation, over-reservation,
 * inactive reservation, unknown product, and multi-product batch.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: inventory-service
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false"
        }
)
@Testcontainers
class JpaInventoryAvailabilityQueryAdapterIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-inventory-service-schema.sql");

    /**
     * Stubs for non-persistence ports. StockReservationRepository and
     * InventoryAvailabilityQueryPort are intentionally absent so that
     * InventoryPersistenceConfiguration provides the JPA adapters.
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
    InventoryAvailabilityQueryPort queryPort;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM inventory_service.stock_allocations");
        jdbcTemplate.execute("DELETE FROM inventory_service.stock_reservation_lines");
        jdbcTemplate.execute("DELETE FROM inventory_service.stock_reservations");
        jdbcTemplate.execute("DELETE FROM inventory_service.inventory_stock");
    }

    // ── stock-only ────────────────────────────────────────────────────────────

    @Test
    void stock_only_no_reservations_returns_full_on_hand_quantity() {
        insertStock("SKU-001", 10);

        Map<String, InventoryAvailabilitySnapshot> result = queryPort.findAvailability(List.of("SKU-001"));

        assertThat(result).containsKey("SKU-001");
        assertThat(result.get("SKU-001").availableQuantity()).isEqualTo(10);
    }

    // ── partial reservation ───────────────────────────────────────────────────

    @Test
    void active_reservation_quantity_is_deducted() {
        insertStock("SKU-001", 10);
        insertActiveReservation("res-1", "order-1", "SKU-001", 3);

        Map<String, InventoryAvailabilitySnapshot> result = queryPort.findAvailability(List.of("SKU-001"));

        assertThat(result.get("SKU-001").availableQuantity()).isEqualTo(7);
    }

    // ── fully reserved ────────────────────────────────────────────────────────

    @Test
    void fully_reserved_product_returns_zero_availability() {
        insertStock("SKU-001", 10);
        insertActiveReservation("res-2", "order-2", "SKU-001", 10);

        Map<String, InventoryAvailabilitySnapshot> result = queryPort.findAvailability(List.of("SKU-001"));

        assertThat(result.get("SKU-001").availableQuantity()).isEqualTo(0);
    }

    // ── over-reserved (inconsistent data) ────────────────────────────────────

    @Test
    void over_reserved_product_clamps_availability_to_zero() {
        insertStock("SKU-001", 10);
        insertActiveReservation("res-3a", "order-3a", "SKU-001", 7);
        insertActiveReservation("res-3b", "order-3b", "SKU-001", 5); // total 12 > 10

        Map<String, InventoryAvailabilitySnapshot> result = queryPort.findAvailability(List.of("SKU-001"));

        assertThat(result.get("SKU-001").availableQuantity()).isEqualTo(0);
    }

    // ── inactive reservation ──────────────────────────────────────────────────

    @Test
    void released_reservation_does_not_reduce_availability() {
        insertStock("SKU-001", 10);
        insertReservationWithStatus("res-4", "order-4", "SKU-001", 4, "RELEASED");

        Map<String, InventoryAvailabilitySnapshot> result = queryPort.findAvailability(List.of("SKU-001"));

        assertThat(result.get("SKU-001").availableQuantity()).isEqualTo(10);
    }

    @Test
    void rejected_reservation_does_not_reduce_availability() {
        insertStock("SKU-001", 10);
        insertReservationWithStatus("res-5", "order-5", "SKU-001", 4, "REJECTED");

        Map<String, InventoryAvailabilitySnapshot> result = queryPort.findAvailability(List.of("SKU-001"));

        assertThat(result.get("SKU-001").availableQuantity()).isEqualTo(10);
    }

    @Test
    void partially_reserved_status_reduces_availability() {
        insertStock("SKU-001", 10);
        insertReservationWithStatus("res-6", "order-6", "SKU-001", 3, "PARTIALLY_RESERVED");

        Map<String, InventoryAvailabilitySnapshot> result = queryPort.findAvailability(List.of("SKU-001"));

        assertThat(result.get("SKU-001").availableQuantity()).isEqualTo(7);
    }

    // ── unknown product ───────────────────────────────────────────────────────

    @Test
    void unknown_product_is_absent_from_result() {
        Map<String, InventoryAvailabilitySnapshot> result = queryPort.findAvailability(List.of("SKU-UNKNOWN"));

        assertThat(result).doesNotContainKey("SKU-UNKNOWN");
    }

    // ── zero stock ────────────────────────────────────────────────────────────

    @Test
    void zero_stock_product_returns_zero_availability() {
        insertStock("SKU-ZERO", 0);

        Map<String, InventoryAvailabilitySnapshot> result = queryPort.findAvailability(List.of("SKU-ZERO"));

        assertThat(result).containsKey("SKU-ZERO");
        assertThat(result.get("SKU-ZERO").availableQuantity()).isEqualTo(0);
    }

    // ── multiple products ─────────────────────────────────────────────────────

    @Test
    void multiple_products_are_returned_in_single_batch() {
        insertStock("SKU-A", 10);
        insertStock("SKU-B", 5);
        insertStock("SKU-C", 0);
        insertActiveReservation("res-a", "order-a", "SKU-A", 3);

        Map<String, InventoryAvailabilitySnapshot> result =
                queryPort.findAvailability(List.of("SKU-A", "SKU-B", "SKU-C", "SKU-MISSING"));

        assertThat(result).containsKey("SKU-A");
        assertThat(result).containsKey("SKU-B");
        assertThat(result).containsKey("SKU-C");
        assertThat(result).doesNotContainKey("SKU-MISSING");

        assertThat(result.get("SKU-A").availableQuantity()).isEqualTo(7);
        assertThat(result.get("SKU-B").availableQuantity()).isEqualTo(5);
        assertThat(result.get("SKU-C").availableQuantity()).isEqualTo(0);
    }

    // ── read-only — no mutations ──────────────────────────────────────────────

    @Test
    void availability_query_does_not_mutate_stock() {
        insertStock("SKU-001", 10);

        queryPort.findAvailability(List.of("SKU-001"));
        queryPort.findAvailability(List.of("SKU-001")); // second call must return same result

        Map<String, InventoryAvailabilitySnapshot> result = queryPort.findAvailability(List.of("SKU-001"));
        assertThat(result.get("SKU-001").availableQuantity()).isEqualTo(10);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void insertStock(String productId, int onHand) {
        jdbcTemplate.update(
                "INSERT INTO inventory_service.inventory_stock (product_id, on_hand_quantity, version) VALUES (?, ?, 0)",
                productId, onHand);
    }

    private void insertActiveReservation(String reservationId, String orderId, String skuCode, int quantity) {
        insertReservationWithStatus(reservationId, orderId, skuCode, quantity, "RESERVED");
    }

    private void insertReservationWithStatus(String reservationId, String orderId, String skuCode,
                                              int quantity, String status) {
        jdbcTemplate.update(
                "INSERT INTO inventory_service.stock_reservations (id, order_id, status, version) VALUES (?, ?, ?, 0)",
                reservationId, orderId, status);
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
