package com.arbitrier.order.adapter.outbound.persistence;

import com.arbitrier.order.adapter.outbound.RecordingOrderEventPublisher;
import com.arbitrier.order.adapter.outbound.StubInventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.OrderEventPublisher;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Flyway migrations run cleanly and Hibernate validates against
 * the migrated order_service schema.
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
class FlywayMigrationIT {

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
    DataSource dataSource;

    @Test
    void orders_table_exists_after_migration() throws Exception {
        assertTableExists("order_service", "orders");
    }

    @Test
    void order_lines_table_exists_after_migration() throws Exception {
        assertTableExists("order_service", "order_lines");
    }

    @Test
    void orders_version_column_exists() throws Exception {
        assertColumnExists("order_service", "orders", "version");
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
