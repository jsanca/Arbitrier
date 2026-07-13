package com.arbitrier.platform.infra;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that infra/docker/seed/seed.sql executes successfully against Flyway-migrated schemas
 * and that all documented scenarios are present. Seed is executed twice to verify ON CONFLICT DO
 * NOTHING idempotency.
 *
 * <p>Runs migrations programmatically via Flyway Java API using filesystem migration locations,
 * then executes the seed file directly via JDBC.
 *
 * <p>Layer: test/infra
 * <p>Module: platform
 */
@Testcontainers
class SeedCompatibilityIT {

    private static final Path REPO_ROOT = findRepoRoot();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/all-schemas.sql");

    @BeforeAll
    static void runMigrationsAndSeed() throws Exception {
        String url = POSTGRES.getJdbcUrl();
        String user = POSTGRES.getUsername();
        String password = POSTGRES.getPassword();

        migrate(url, user, password,
                "filesystem:" + REPO_ROOT.resolve(
                        "server/order-service/src/main/resources/db/migration/order_service"),
                "order_service");
        migrate(url, user, password,
                "filesystem:" + REPO_ROOT.resolve(
                        "server/inventory-service/src/main/resources/db/migration/inventory_service"),
                "inventory_service");
        migrate(url, user, password,
                "filesystem:" + REPO_ROOT.resolve(
                        "server/credit-service/src/main/resources/db/migration/credit_service"),
                "credit_service");
        migrate(url, user, password,
                "filesystem:" + REPO_ROOT.resolve(
                        "server/orchestrator-service/src/main/resources/db/migration/orchestrator_service"),
                "orchestrator_service");

        Path seedPath = REPO_ROOT.resolve("infra/docker/seed/seed.sql");
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            executeSqlFile(conn, seedPath);
            executeSqlFile(conn, seedPath);
        }
    }

    @Test
    void confirmed_order_scenario_seeded() throws Exception {
        assertThat(countRows("order_service", "orders",
                "id = 'order-seed-001' AND status = 'CONFIRMED'"))
                .isEqualTo(1);
    }

    @Test
    void waiting_saga_scenario_seeded() throws Exception {
        assertThat(countRows("orchestrator_service", "sagas",
                "status = 'AWAITING_CUSTOMER_DECISION'"))
                .isEqualTo(1);
    }

    @Test
    void cancelled_order_scenario_seeded() throws Exception {
        assertThat(countRows("order_service", "orders",
                "status = 'CANCELLED' AND cancellation_reason = 'INSUFFICIENT_CREDIT'"))
                .isEqualTo(1);
    }

    @Test
    void failed_compensation_saga_seeded() throws Exception {
        assertThat(countRows("orchestrator_service", "sagas",
                "status = 'FAILED_COMPENSATION'"))
                .isEqualTo(1);
    }

    @Test
    void multi_warehouse_allocation_rows_seeded() throws Exception {
        assertThat(countRows("inventory_service", "stock_allocations",
                "warehouse_id IN ('WH-NORTH', 'WH-SOUTH')"))
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void approved_and_rejected_credit_reservations_seeded() throws Exception {
        assertThat(countRows("credit_service", "credit_reservations",
                "status = 'APPROVED'"))
                .isEqualTo(2);
        assertThat(countRows("credit_service", "credit_reservations",
                "status = 'REJECTED'"))
                .isEqualTo(1);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Path findRepoRoot() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("infra")) && Files.isDirectory(dir.resolve("server"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "Cannot find repository root (expected directory containing 'infra/' and 'server/')");
    }

    private static void migrate(String url, String user, String password,
                                String location, String schema) {
        Flyway.configure()
                .dataSource(url, user, password)
                .locations(location)
                .defaultSchema(schema)
                .schemas(schema)
                .load()
                .migrate();
    }

    private static void executeSqlFile(Connection conn, Path file) throws Exception {
        String content = Files.readString(file);
        try (Statement stmt = conn.createStatement()) {
            for (String raw : content.split(";")) {
                String trimmed = raw.lines()
                        .filter(line -> !line.stripLeading().startsWith("--"))
                        .reduce("", (a, b) -> a + "\n" + b)
                        .trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static int countRows(String schema, String table, String whereClause)
            throws Exception {
        String sql = "SELECT COUNT(*) FROM " + schema + "." + table;
        if (whereClause != null && !whereClause.isEmpty()) {
            sql = sql + " WHERE " + whereClause;
        }
        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
