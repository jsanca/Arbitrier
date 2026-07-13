package com.arbitrier.credit.adapter.outbound.persistence;

import com.arbitrier.credit.adapter.outbound.ConfigurableCreditLimitPort;
import com.arbitrier.credit.adapter.outbound.RecordingCreditReservationEventPublisher;
import com.arbitrier.credit.application.port.outbound.CreditLimitPort;
import com.arbitrier.credit.application.port.outbound.CreditReservationEventPublisher;
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
 * the migrated credit_service schema.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: credit-service
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
            .withInitScript("test-db/create-credit-service-schema.sql");

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        CreditLimitPort creditLimitPort() {
            return new ConfigurableCreditLimitPort();
        }

        @Bean
        @Primary
        CreditReservationEventPublisher creditReservationEventPublisher() {
            return new RecordingCreditReservationEventPublisher();
        }
    }

    @Autowired
    DataSource dataSource;

    @Test
    void credit_reservations_table_exists_after_migration() throws Exception {
        assertTableExists("credit_service", "credit_reservations");
    }

    @Test
    void credit_reservations_version_column_exists() throws Exception {
        assertColumnExists("credit_service", "credit_reservations", "version");
    }

    @Test
    void credit_reservations_amount_value_column_exists() throws Exception {
        assertColumnExists("credit_service", "credit_reservations", "amount_value");
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
