package com.arbitrier.platform.infra;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that infra/docker/init-db.sql runs successfully against a fresh PostgreSQL instance
 * and produces the expected structural state: schemas, roles, databases, and no business tables.
 *
 * <p>Executes the repository SQL file directly via psql inside the container (the file uses
 * psql-specific syntax including \gexec and variable substitution).
 *
 * <p>Layer: test/infra
 * <p>Module: platform
 */
@Testcontainers
class InitDbSqlIT {

    private static final Path REPO_ROOT = findRepoRoot();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("arbitrier")
            .withUsername("postgres")
            .withPassword("postgres")
            .withFileSystemBind(
                    REPO_ROOT.resolve("infra/docker").toString(),
                    "/docker-init",
                    BindMode.READ_ONLY);

    @BeforeAll
    static void runInitScript() throws Exception {
        org.testcontainers.containers.Container.ExecResult result = POSTGRES.execInContainer(
                "psql", "-U", "postgres", "-d", "arbitrier",
                "-v", "ARBITRIER_SERVICE_PASSWORD=testpw",
                "-v", "KEYCLOAK_DB_PASSWORD=testpw",
                "-f", "/docker-init/init-db.sql");
        assertThat(result.getExitCode())
                .as("init-db.sql failed.\nstdout: %s\nstderr: %s",
                        result.getStdout(), result.getStderr())
                .isEqualTo(0);
    }

    @Test
    void service_schemas_exist() throws Exception {
        assertThat(schemaExistsWithOwner("order_service", "order_service"))
                .as("Schema order_service owned by order_service").isTrue();
        assertThat(schemaExistsWithOwner("inventory_service", "inventory_service"))
                .as("Schema inventory_service owned by inventory_service").isTrue();
        assertThat(schemaExistsWithOwner("credit_service", "credit_service"))
                .as("Schema credit_service owned by credit_service").isTrue();
        assertThat(schemaExistsWithOwner("orchestrator_service", "orchestrator_service"))
                .as("Schema orchestrator_service owned by orchestrator_service").isTrue();
        assertThat(schemaExistsWithOwner("platform", "platform"))
                .as("Schema platform owned by platform").isTrue();
    }

    @Test
    void service_roles_exist() throws Exception {
        assertThat(roleExists("order_service")).as("Role order_service").isTrue();
        assertThat(roleExists("inventory_service")).as("Role inventory_service").isTrue();
        assertThat(roleExists("credit_service")).as("Role credit_service").isTrue();
        assertThat(roleExists("orchestrator_service")).as("Role orchestrator_service").isTrue();
        assertThat(roleExists("platform")).as("Role platform").isTrue();
        assertThat(roleExists("keycloak")).as("Role keycloak").isTrue();
    }

    @Test
    void keycloak_database_exists() throws Exception {
        String count = queryOne(
                "SELECT COUNT(*) FROM pg_database WHERE datname = 'keycloak'");
        assertThat(Integer.parseInt(count)).isEqualTo(1);
    }

    @Test
    void init_db_creates_no_business_tables() throws Exception {
        String count = queryOne(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema IN ('order_service', 'inventory_service', " +
                "'credit_service', 'orchestrator_service') AND table_type = 'BASE TABLE'");
        assertThat(Integer.parseInt(count)).isEqualTo(0);
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

    private static String queryOne(String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static boolean roleExists(String roleName) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM pg_roles WHERE rolname = ?")) {
            stmt.setString(1, roleName);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private static boolean schemaExistsWithOwner(String schemaName, String expectedOwner)
            throws Exception {
        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT r.rolname FROM pg_namespace n " +
                     "JOIN pg_roles r ON r.oid = n.nspowner " +
                     "WHERE n.nspname = ?")) {
            stmt.setString(1, schemaName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return expectedOwner.equals(rs.getString(1));
            }
        }
    }
}
