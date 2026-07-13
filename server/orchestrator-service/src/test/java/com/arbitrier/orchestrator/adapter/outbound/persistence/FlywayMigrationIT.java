package com.arbitrier.orchestrator.adapter.outbound.persistence;

import com.arbitrier.orchestrator.adapter.outbound.RecordingConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReleaseCreditCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReserveStockCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
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
 * the migrated orchestrator_service schema.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: orchestrator-service
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
            .withInitScript("test-db/create-orchestrator-service-schema.sql");

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        SagaEventPublisher sagaEventPublisher() {
            return new RecordingSagaEventPublisher();
        }

        @Bean
        @Primary
        ReserveStockCommandPublisher reserveStockCommandPublisher() {
            return new RecordingReserveStockCommandPublisher();
        }

        @Bean
        @Primary
        ReserveCreditCommandPublisher reserveCreditCommandPublisher() {
            return new RecordingReserveCreditCommandPublisher();
        }

        @Bean
        @Primary
        ConfirmOrderCommandPublisher confirmOrderCommandPublisher() {
            return new RecordingConfirmOrderCommandPublisher();
        }

        @Bean
        @Primary
        ReleaseStockCommandPublisher releaseStockCommandPublisher() {
            return new RecordingReleaseStockCommandPublisher();
        }

        @Bean
        @Primary
        ReleaseCreditCommandPublisher releaseCreditCommandPublisher() {
            return new RecordingReleaseCreditCommandPublisher();
        }
    }

    @Autowired
    DataSource dataSource;

    @Test
    void sagas_table_exists_after_migration() throws Exception {
        assertTableExists("orchestrator_service", "sagas");
    }

    @Test
    void sagas_version_column_exists() throws Exception {
        assertColumnExists("orchestrator_service", "sagas", "version");
    }

    @Test
    void sagas_stock_reservation_id_column_exists() throws Exception {
        assertColumnExists("orchestrator_service", "sagas", "stock_reservation_id");
    }

    @Test
    void sagas_credit_reservation_id_column_exists() throws Exception {
        assertColumnExists("orchestrator_service", "sagas", "credit_reservation_id");
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
