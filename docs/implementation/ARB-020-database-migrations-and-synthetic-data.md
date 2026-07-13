# ARB-020 — Database Migrations & Synthetic Data

## Status

DONE

## Created and modified files

### New migration scripts

| File | Tables |
|------|--------|
| `server/order-service/src/main/resources/db/migration/order_service/V1__create_order_tables.sql` | `orders`, `order_lines` |
| `server/inventory-service/src/main/resources/db/migration/inventory_service/V1__create_inventory_tables.sql` | `stock_reservations`, `stock_reservation_lines`, `stock_allocations` |
| `server/credit-service/src/main/resources/db/migration/credit_service/V1__create_credit_tables.sql` | `credit_reservations` |
| `server/orchestrator-service/src/main/resources/db/migration/orchestrator_service/V1__create_saga_tables.sql` | `sagas` |

### New test files

| File | Purpose |
|------|---------|
| `server/order-service/src/test/.../FlywayMigrationIT.java` | Verifies migration + Hibernate validation for order_service |
| `server/inventory-service/src/test/.../FlywayMigrationIT.java` | Verifies migration + Hibernate validation for inventory_service |
| `server/credit-service/src/test/.../FlywayMigrationIT.java` | Verifies migration + Hibernate validation for credit_service |
| `server/orchestrator-service/src/test/.../FlywayMigrationIT.java` | Verifies migration + Hibernate validation for orchestrator_service |

### New local tooling

| File | Purpose |
|------|---------|
| `infra/docker/seed/seed.sql` | Optional local-only synthetic data for 4 scenarios |
| `infra/docker/reset.sh` | Wipes volumes, restarts stack, optionally loads seed |

### Modified files

| File | Change |
|------|--------|
| `server/*/pom.xml` (4 services) | Added `flyway-core` and `flyway-database-postgresql` dependencies |
| `server/*/src/main/resources/application.yml` (4 services) | Set `ddl-auto=validate`, added Flyway `locations`, `default-schema`, `schemas`, `validate-on-migrate`, `clean-disabled` |
| `server/order-service/src/test/.../JpaOrderRepositoryAdapterTest.java` | Added `@BeforeEach` cleanup; added `spring.flyway.enabled=false` property |
| `server/inventory-service/src/test/.../JpaStockReservationRepositoryAdapterTest.java` | Added `@BeforeEach` cleanup; added `spring.flyway.enabled=false` property |
| `server/credit-service/src/test/.../JpaCreditReservationRepositoryAdapterTest.java` | Added `@BeforeEach` cleanup; added `spring.flyway.enabled=false` property |
| `server/orchestrator-service/src/test/.../JpaSagaRepositoryAdapterTest.java` | Added `@BeforeEach` cleanup; added `spring.flyway.enabled=false` property |

---

## Schemas, tables, indexes, and constraints

All table names in migration scripts are schema-qualified (e.g. `order_service.orders`). See
"Implementation decisions" below for the reason.

### order_service

**`orders`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `VARCHAR(255)` | PK |
| `customer_id` | `VARCHAR(255)` | NOT NULL |
| `submitted_by` | `VARCHAR(255)` | NOT NULL |
| `status` | `VARCHAR(50)` | NOT NULL; CHECK IN ('PENDING', 'AWAITING_CUSTOMER_DECISION', 'CONFIRMED', 'PARTIALLY_CONFIRMED', 'CANCELLED') |
| `cancellation_reason` | `VARCHAR(50)` | nullable; CHECK IS NULL OR IN ('CUSTOMER_CANCELLED', 'CUSTOMER_DEFERRED', 'INSUFFICIENT_CREDIT', 'SYSTEM_TIMEOUT') |
| `version` | `BIGINT` | NOT NULL DEFAULT 0 |

**`order_lines`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `BIGSERIAL` | PK |
| `order_id` | `VARCHAR(255)` | FK → `order_service.orders(id)` |
| `sku` | `VARCHAR(255)` | NOT NULL |
| `quantity` | `INTEGER` | CHECK > 0 |

Indexes: `idx_orders_customer_id`, `idx_orders_status`, `idx_order_lines_order_id`.

---

### inventory_service

**`stock_reservations`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `VARCHAR(255)` | PK |
| `order_id` | `VARCHAR(255)` | NOT NULL |
| `status` | `VARCHAR(50)` | CHECK IN ('RESERVED', 'PARTIALLY_RESERVED', 'REJECTED', 'RELEASED') |
| `version` | `BIGINT` | NOT NULL DEFAULT 0 |

**`stock_reservation_lines`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `BIGSERIAL` | PK |
| `reservation_id` | `VARCHAR(255)` | FK → `inventory_service.stock_reservations(id)` |
| `sku_code` | `VARCHAR(255)` | NOT NULL |
| `requested_quantity` | `INTEGER` | CHECK > 0 |

**`stock_allocations`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `BIGSERIAL` | PK |
| `line_id` | `BIGINT` | FK → `inventory_service.stock_reservation_lines(id)` |
| `warehouse_id` | `VARCHAR(255)` | NOT NULL |
| `sku` | `VARCHAR(255)` | NOT NULL |
| `quantity` | `INTEGER` | CHECK >= 0 |

Allocation quantity allows zero because a warehouse may be listed with zero available after negotiation.

Indexes: `idx_stock_reservations_order_id`, `idx_stock_reservations_status`, `idx_srl_reservation_id`, `idx_sa_line_id`, `idx_sa_warehouse_sku`.

---

### credit_service

**`credit_reservations`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `VARCHAR(255)` | PK |
| `order_id` | `VARCHAR(255)` | NOT NULL |
| `amount_value` | `NUMERIC(19,4)` | NOT NULL; CHECK >= 0 |
| `amount_currency` | `VARCHAR(3)` | NOT NULL |
| `status` | `VARCHAR(50)` | CHECK IN ('APPROVED', 'REJECTED', 'RELEASED') |
| `version` | `BIGINT` | NOT NULL DEFAULT 0 |

`NUMERIC(19,4)` matches entity `@Column(precision=19, scale=4)`.

Indexes: `idx_credit_reservations_order_id`, `idx_credit_reservations_status`.

---

### orchestrator_service

**`sagas`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `VARCHAR(255)` | PK |
| `order_id` | `VARCHAR(255)` | NOT NULL |
| `customer_id` | `VARCHAR(255)` | NOT NULL |
| `status` | `VARCHAR(50)` | CHECK IN ('STARTED', 'WAITING_FOR_INVENTORY', 'WAITING_FOR_CREDIT', 'AWAITING_CUSTOMER_DECISION', 'COMPENSATING', 'COMPLETED', 'CANCELLED', 'FAILED_COMPENSATION') |
| `current_step` | `VARCHAR(50)` | CHECK IN ('ORDER_CREATED', 'RESERVE_INVENTORY', 'VALIDATE_CREDIT', 'AWAIT_CUSTOMER_DECISION', 'COMPLETE_ORDER', 'COMPENSATE_INVENTORY', 'COMPENSATE_CREDIT') |
| `customer_decision` | `VARCHAR(50)` | nullable; CHECK IS NULL OR IN ('ACCEPT_PARTIAL', 'WAIT_BACKORDER', 'CANCEL_ORDER') |
| `stock_reservation_id` | `VARCHAR(255)` | nullable |
| `credit_reservation_id` | `VARCHAR(255)` | nullable |
| `version` | `BIGINT` | NOT NULL DEFAULT 0 |

Indexes:
- `idx_sagas_order_id` — saga lookup by order
- `idx_sagas_status` — status polling
- `idx_sagas_current_step` — step-based queries
- `idx_sagas_waiting` — partial index on `status IN ('AWAITING_CUSTOMER_DECISION', 'WAITING_FOR_INVENTORY', 'WAITING_FOR_CREDIT')` for timeout scanning
- `idx_sagas_failed_comp` — partial index on `status = 'FAILED_COMPENSATION'` for alerting queries

---

## Application configuration changes

Each `application.yml` receives:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate        # Hibernate validates; Flyway owns schema evolution
    open-in-view: false
  flyway:
    locations: classpath:db/migration/<schema_name>
    default-schema: <schema_name>
    schemas: <schema_name>
    validate-on-migrate: true
    clean-disabled: true
```

`clean-disabled: true` prevents accidental `flyway clean` in non-reset contexts.

---

## Implementation decisions

### Schema-qualified table names in migrations

All CREATE TABLE, REFERENCES, CREATE INDEX, and CREATE INDEX ... WHERE statements use
fully qualified names (e.g. `order_service.orders`, not `orders`).

Flyway's `spring.flyway.default-schema` and `spring.flyway.schemas` are intended to set the
JDBC connection's `search_path` before migration execution. In practice, when tests use
`@ServiceConnection` from Testcontainers, the JDBC URL is dynamically assigned and does not
carry a `currentSchema` parameter. Flyway's schema management did not reliably apply
`search_path` before migration scripts ran — tables were created in `public` instead of the
service schema, causing Hibernate validation to fail at startup.

Using schema-qualified names is unconditional and portable. It requires no reliance on
session-level `search_path` behavior.

### Test data isolation in existing adapter tests

`Jpa*RepositoryAdapterTest` classes use static final test IDs (`order-tc-1`, `res-tc-1`, etc.)
and share one Spring context and one Postgres container per module. Adding `FlywayMigrationIT`
to the same module did not change context sharing, but test execution order caused multiple
test methods to attempt `INSERT` with the same PK in the same container lifecycle.

Fix: added `@BeforeEach void clean()` to each adapter test class, injecting the
`SpringData*Repository` directly and calling `deleteById(id)` before every test method.
`deleteById` is a no-op when the row does not exist.

---

## Seed strategy

Strategy B — standalone SQL file `infra/docker/seed/seed.sql`.

All inserts use `ON CONFLICT DO NOTHING` for idempotency.

| Seed ID | Scenario | Saga status | Notes |
|---------|----------|-------------|-------|
| `*-seed-001` | ACME confirmed order | `COMPLETED` | Multi-warehouse allocation (WH-NORTH + WH-SOUTH) |
| `*-seed-002` | ACME order awaiting decision | `AWAITING_CUSTOMER_DECISION` | Partial reservation (120/200 SKU-WIDGET-A; WH-NORTH + WH-EAST) |
| `*-seed-003` | BETA cancelled (insufficient credit) | `CANCELLED` | Credit reservation `REJECTED`; stock reservation `RELEASED` |
| `*-seed-004` | ACME failed compensation | `FAILED_COMPENSATION` | No credit reservation; stock reservation `RESERVED` but uncompensated |

Seed is never loaded automatically. Load explicitly:

```bash
./infra/docker/reset.sh --seed
# or
psql -U arbitrier_admin -d arbitrier -f infra/docker/seed/seed.sql
```

---

## Reset procedure

```bash
./infra/docker/reset.sh           # wipe volumes, restart stack
./infra/docker/reset.sh --seed    # reset + load seed data
```

The script stops the stack with `--remove-orphans -v`, brings it back up with `--wait`,
polls PostgreSQL for readiness, then optionally streams `seed.sql` into the container.
Flyway migrations run automatically when each Java service starts after the reset.

---

## Tests

### FlywayMigrationIT (4 new tests, one per service)

- Uses `@SpringBootTest(ddl-auto=validate)` + Testcontainers `@ServiceConnection`
- Init script creates the service schema (`CREATE SCHEMA IF NOT EXISTS <schema>`)
- Flyway runs migrations from `classpath:db/migration/<schema_name>/`
- Assertions via `DatabaseMetaData`:
  - tables exist in the correct schema
  - `version` column exists on aggregate root tables
  - application context loads and `DataSource` is non-null (confirms Hibernate validated)

### Existing Jpa*RepositoryAdapterTest (modified)

- Added `spring.flyway.enabled=false` property — these tests use `ddl-auto=create-drop` and own their schema creation
- Added `@BeforeEach` cleanup to prevent duplicate-key failures from shared static IDs

### Test run result

```
BUILD SUCCESS
Tests run: platform + contracts + 4 services
Failures: 0, Errors: 0, Skipped: 0
```

---

## Production safety

- `flyway.clean-disabled=true` in all `application.yml` files
- `ddl-auto=validate` — Hibernate cannot create or alter tables at runtime
- Seed SQL is never executed automatically
- No cross-schema foreign keys; each service migrates only its own schema
- Migration checksums enforced by `flyway.validate-on-migrate=true`

---

## Relation to other tasks

| Task | Relation |
|------|---------|
| **ARB-019** | Introduced JPA entities, repositories, and persistence adapters. Migrations replicate the schema those entities expect. |
| **ARB-027** | Provides `infra/docker/compose.sh` and `init-db.sql` that create schemas and users. `reset.sh` delegates to it. |
| **ARB-021** | Outbox/Inbox foundation is deferred. When introduced, it will add `V2__add_outbox_table.sql` to each relevant service. |

---

## Native Image note

Flyway migration scripts packaged under `classpath:db/migration/` are included in the
Spring Boot fat JAR as classpath resources. Spring Boot's AOT processing registers classpath
resources automatically. No `RuntimeHintsRegistrar` is needed for standard versioned
migration scripts.

OPEN QUESTION: Flyway's internal class scanning for migration resolvers may have native-image
implications depending on the Flyway version shipped with Spring Boot 4.1.0. Verify when
native builds are introduced (ADR-0007 / RNF-0002).

---

## Open questions

- **Native image**: Flyway resource scanning compatibility with GraalVM native image has not
  been exercised. Defer to the native build task (ARB-004B / ADR-0007).
- **Migration checksum policy**: No documented process yet for handling forced checksum repairs
  in production (e.g. `flyway repair`). Needed before first production deployment.
- **Seed data currency**: The seeded IDs (`order-seed-001` etc.) are arbitrary and local-only.
  If the local schema evolves (new NOT NULL columns, new enum values), seed.sql must be updated
  alongside the migration.

---

## ARB-020-FIX-001 — Persistence Validation Improvements

### Status

DONE

### Warnings resolved

| Warning | Resolution |
|---------|-----------|
| W1 — `seed.sql` not automatically verified | `SeedCompatibilityIT` (platform module) runs all Flyway migrations, executes `seed.sql` twice, and asserts all four documented scenarios |
| W2 — no business operation exercised against Flyway-migrated schema | `RepositoryRoundTripIT` (one per service) saves and loads a domain aggregate through the JPA adapter with `ddl-auto=validate` |
| W3 — `init-db.sql` not automatically verified | `InitDbSqlIT` (platform module) runs `init-db.sql` via psql inside a Testcontainer and asserts schemas, roles, keycloak DB, and absence of business tables |
| W4 — Flyway/GraalVM native-image compatibility | **Intentionally deferred** to ARB-004B / ADR-0007 |

### New files

| File | Purpose |
|------|---------|
| `server/order-service/src/test/.../persistence/RepositoryRoundTripIT.java` | Round-trip: save + load `Order` (with lines) against Flyway-migrated schema; validates status transition and optimistic-lock version |
| `server/inventory-service/src/test/.../persistence/RepositoryRoundTripIT.java` | Round-trip: save + load `StockReservation` with multi-warehouse `StockAllocation` entries |
| `server/credit-service/src/test/.../persistence/RepositoryRoundTripIT.java` | Round-trip: save + load `CreditReservation`; verifies `Money` amount/currency and status |
| `server/orchestrator-service/src/test/.../persistence/RepositoryRoundTripIT.java` | Round-trip: save + load `Saga`; verifies `AWAITING_CUSTOMER_DECISION` waiting state and reservation IDs |
| `server/platform/src/test/.../infra/InitDbSqlIT.java` | Runs `infra/docker/init-db.sql` inside a Testcontainer; asserts service schemas with correct ownership, service roles, keycloak DB, and no business tables |
| `server/platform/src/test/.../infra/SeedCompatibilityIT.java` | Migrates all four schemas, executes `infra/docker/seed/seed.sql` twice (idempotency); asserts all four seed scenarios and multi-warehouse allocation rows |

### Design decisions

**RepositoryRoundTripIT uses `withInitScript`** — each service's test container is bootstrapped
with a `test-db/create-<service>-schema.sql` init script (already present from `FlywayMigrationIT`)
that creates the schema before Flyway migrations run. `ddl-auto=validate` is set explicitly via
`@SpringBootTest(properties = …)` to confirm the migrated schema satisfies Hibernate's expectations.

**`InitDbSqlIT` uses psql exec** — `init-db.sql` contains psql-specific syntax (`\gexec`,
variable substitution with `-v`). JDBC cannot execute it directly. The test binds `infra/docker/`
into the container at `/docker-init` and uses `execInContainer("psql", …, "-f", "/docker-init/init-db.sql")`.

**`SeedCompatibilityIT` uses Flyway Java API + JDBC** — migrations are run programmatically via
`Flyway.configure().locations("filesystem:…").load().migrate()` so no Spring context is needed.
The seed file is executed by splitting on `;`, stripping comment lines, and calling
`Statement.execute()` on each non-empty statement. This avoids psql dependency in the platform
module and keeps the test self-contained.

**Path resolution** — both platform tests locate repository files via a `findRepoRoot()` helper
that walks up from `user.dir` until it finds a directory containing both `infra/` and `server/`.
This is deterministic across Maven module working directories.

### How to run

```bash
# All new tests run automatically with the standard build:
mvn -B verify --no-transfer-progress -pl server/contracts,server/platform,server/order-service,server/inventory-service,server/credit-service,server/orchestrator-service

# Platform tests only (InitDbSqlIT + SeedCompatibilityIT):
mvn -B verify --no-transfer-progress -pl server/contracts,server/platform

# Single service round-trip test:
mvn -B verify --no-transfer-progress -pl server/contracts,server/platform,server/order-service -Dtest=RepositoryRoundTripIT
```

All tests use Testcontainers and require no manually running local stack.

### Test results

```
BUILD SUCCESS
platform:              83 tests, 0 failures, 0 errors  (includes InitDbSqlIT + SeedCompatibilityIT)
order-service:         97 tests, 0 failures, 0 errors  (includes RepositoryRoundTripIT)
inventory-service:     80 tests, 0 failures, 0 errors  (includes RepositoryRoundTripIT)
credit-service:        66 tests, 0 failures, 0 errors  (includes RepositoryRoundTripIT)
orchestrator-service: 229 tests, 0 failures, 0 errors  (includes RepositoryRoundTripIT)
Total:                555 tests, 0 failures, 0 errors
```
