package com.arbitrier.inventory.adapter.outbound.persistence;

import com.arbitrier.inventory.adapter.outbound.ConfigurableWarehouseAllocationPort;
import com.arbitrier.inventory.adapter.outbound.RecordingStockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Flyway migrations run cleanly and Hibernate validates against
 * the migrated inventory_service schema.
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
class FlywayMigrationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-inventory-service-schema.sql");

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
    DataSource dataSource;

    @Test
    void stock_reservations_table_exists_after_migration() throws Exception {
        assertTableExists("inventory_service", "stock_reservations");
    }

    @Test
    void stock_reservation_lines_table_exists_after_migration() throws Exception {
        assertTableExists("inventory_service", "stock_reservation_lines");
    }

    @Test
    void stock_allocations_table_exists_after_migration() throws Exception {
        assertTableExists("inventory_service", "stock_allocations");
    }

    @Test
    void stock_reservations_version_column_exists() throws Exception {
        assertColumnExists("inventory_service", "stock_reservations", "version");
    }

    @Test
    void inventory_stock_table_exists_after_v2_migration() throws Exception {
        assertTableExists("inventory_service", "inventory_stock");
    }

    @Test
    void inventory_stock_product_id_column_exists() throws Exception {
        assertColumnExists("inventory_service", "inventory_stock", "product_id");
    }

    @Test
    void inventory_stock_on_hand_quantity_column_exists() throws Exception {
        assertColumnExists("inventory_service", "inventory_stock", "on_hand_quantity");
    }

    @Test
    void context_loads_and_hibernate_validates() {
        assertThat(dataSource).isNotNull();
    }

    private void assertTableExists(String schema, String table) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, schema, table, new String[]{"TABLE"})) {
                assertThat(rs.next())
                        .as("Expected table %s.%s to exist after migration", schema, table)
                        .isTrue();
            }
        }
    }

    private void assertColumnExists(String schema, String table, String column) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, schema, table, column)) {
                assertThat(rs.next())
                        .as("Expected column %s on %s.%s to exist", column, schema, table)
                        .isTrue();
            }
        }
    }
}
