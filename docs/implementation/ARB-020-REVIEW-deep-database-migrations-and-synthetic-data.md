# ARB-020 REVIEW — Database Migrations & Synthetic Data

| Field | Value |
|-------|-------|
| **Reviewer** | Deep |
| **Date** | 2026-07-11 |
| **Task** | ARB-020 |
| **Status** | PLANNED |

---

## Verdict: **PASS WITH WARNINGS**

4 warnings — all classified as Maintainability or Future evolution. No architecture, correctness, or performance blockers.

---

## Summary

ARB-020 introduces Flyway-based schema management across all four services. Each service owns one V1 migration that creates its tables, constraints, and indexes within its own PostgreSQL schema. Hibernate is locked to `ddl-auto: validate` in all environments. The implementation correctly separates migration ownership (Flyway) from entity mapping verification (Hibernate), and cleanly isolates each bounded context's schema with no cross-schema foreign keys.

The seed strategy is safe — idempotent, never auto-loaded, and completely outside the application classpath. The test strategy uses a deliberate split: JPA adapter tests use `ddl-auto=create-drop` with Flyway disabled, while `FlywayMigrationIT` validates migration + Hibernate validation in a dedicated test.

Warnings relate to test coverage gaps (seed.sql and init-db.sql have no automated verification) and the absence of an end-to-end operational test through a migrated database.

---

## Architecture

| Check | Result |
|-------|--------|
| Flyway is single source of truth | PASS — `ddl-auto: validate` in all `application.yml` |
| No hidden schema creation | PASS — only `init-db.sql` (empty schemas) and test `CREATE SCHEMA IF NOT EXISTS` |
| Hibernate only validates | PASS — `create-drop` only in adapter tests with `flyway.enabled=false` |
| No cross-schema FKs | PASS — all FKs reference tables within the same schema |
| Schema ownership per service | PASS — `AUTHORIZATION` in `init-db.sql`, `@Table(schema=...)` on every entity |
| Migration duplication | PASS — single V1 per service, no overlapping definitions |
| `clean-disabled: true` | PASS — all four `application.yml` |

### JPA Adapter Test Split

JPA adapter tests use `ddl-auto=create-drop` with `flyway.enabled=false`. FlywayMigrationIT uses `ddl-auto=validate` with Flyway enabled and a test-only init script that creates the schema via `CREATE SCHEMA IF NOT EXISTS`. This is the standard Spring Boot recommended pattern: adapter tests verify entity-to-table mapping, migration tests verify migration quality. The two paths complement each other — neither attempts to replace the other. Clean separation.

---

## Migration Quality

### Naming & Versioning

All four services follow `V1__create_{domain}_tables.sql` within service-owned directories (`db/migration/<schema>/`). Single V1 per service avoids ordering ambiguity. Future migrations will use V2, V3, etc.

### SQL Quality

| Check | Result |
|-------|--------|
| Schema-qualified table names | PASS — intentional (see implementation report) |
| Consistent column naming | PASS — snake_case throughout |
| Readable constraint names | PASS — e.g. `orders_status_chk`, `fk_order_lines_order` |
| Comment headers | PASS — each migration starts with a purpose comment |
| PostgreSQL DDL portability | PASS — no non-standard extensions beyond schema-qualified names |

---

## Constraints

### CHECK Constraints

All enum-valued columns use `VARCHAR(50)` with CHECK constraints rather than PostgreSQL ENUM types. This is the correct choice — PostgreSQL ENUMs are painful to evolve (no `ALTER TYPE ADD VALUE` in transactions before PG 12, and removal requires a full table rewrite).

| Table | Column | Constraint | Status |
|-------|--------|------------|--------|
| `orders` | `status` | IN ('PENDING', 'AWAITING_CUSTOMER_DECISION', 'CONFIRMED', 'PARTIALLY_CONFIRMED', 'CANCELLED') | PASS |
| `orders` | `cancellation_reason` | IS NULL OR IN ('CUSTOMER_CANCELLED', 'CUSTOMER_DEFERRED', 'INSUFFICIENT_CREDIT', 'SYSTEM_TIMEOUT') | PASS |
| `order_lines` | `quantity` | > 0 | PASS |
| `stock_reservations` | `status` | IN ('RESERVED', 'PARTIALLY_RESERVED', 'REJECTED', 'RELEASED') | PASS |
| `stock_reservation_lines` | `requested_quantity` | > 0 | PASS |
| `stock_allocations` | `quantity` | >= 0 | PASS — zero allowed (documented) |
| `credit_reservations` | `status` | IN ('APPROVED', 'REJECTED', 'RELEASED') | PASS |
| `credit_reservations` | `amount_value` | >= 0 | PASS |
| `sagas` | `status` | IN (all 8 states) | PASS |
| `sagas` | `current_step` | IN (all 7 steps) | PASS |
| `sagas` | `customer_decision` | IS NULL OR IN ('ACCEPT_PARTIAL', 'WAIT_BACKORDER', 'CANCEL_ORDER') | PASS |

### NULL Constraints

All required columns are NOT NULL. Columns that represent optional saga state (`cancellation_reason`, `customer_decision`, `stock_reservation_id`, `credit_reservation_id`) are correctly nullable. This matches the domain model: a saga starts without reservation IDs and acquires them as it progresses.

### Entity-to-Migration Consistency

| Entity | `@Table(schema)` | Migration DDL | Match |
|--------|-----------------|---------------|-------|
| `OrderEntity` | `order_service` | `order_service.orders` | PASS |
| `OrderLineEntity` | `order_service` | `order_service.order_lines` | PASS |
| `StockReservationEntity` | `inventory_service` | `inventory_service.stock_reservations` | PASS |
| `StockReservationLineEntity` | `inventory_service` | `inventory_service.stock_reservation_lines` | PASS |
| `StockAllocationEntity` | `inventory_service` | `inventory_service.stock_allocations` | PASS |
| `CreditReservationEntity` | `credit_service` | `credit_service.credit_reservations` | PASS |
| `SagaEntity` | `orchestrator_service` | `orchestrator_service.sagas` | PASS |

The `CreditReservationEntity.amountValue` field has `@Column(precision=19, scale=4)` — the migration uses `NUMERIC(19,4)`. Consistent.

---

## Inventory Model

The three-table hierarchy correctly models the domain:

```
stock_reservations (1) ──→ (N) stock_reservation_lines (1) ──→ (N) stock_allocations
```

- **Multi-warehouse**: Each `stock_allocations` row carries `warehouse_id` — one row per warehouse per line. The `idx_sa_warehouse_sku` composite index supports warehouse-level availability queries.
- **Partial reservation**: `PARTIALLY_RESERVED` status + allocations with quantity less than requested.
- **Release**: `RELEASED` status preserves history without deleting rows.
- **Zero-quantity allocations**: `CHECK (quantity >= 0)` — documented allowance for warehouse negotiation scenarios.

JPA cascade configuration (`CascadeType.ALL`, `orphanRemoval=true`, `FetchType.EAGER`) on both `@OneToMany` relationships ensures correct lifecycle management when the persistence adapter saves or replaces the entity graph.

---

## Saga Persistence

Flat single-row design with explicit columns — no serialized workflow blobs. All saga state is queryable via standard SQL.

### Index Strategy

| Index | Type | Purpose | Assessment |
|-------|------|---------|------------|
| `idx_sagas_order_id` | Full | Lookup saga by order | PASS — standard FK pattern |
| `idx_sagas_status` | Full | Status polling, dashboard | PASS |
| `idx_sagas_current_step` | Full | Step-based queries | PASS |
| `idx_sagas_waiting` | **Partial** (`WHERE status IN (...)` ) | Timeout scanning | PASS — excellent design |
| `idx_sagas_failed_comp` | **Partial** (`WHERE status = 'FAILED_COMPENSATION'`) | Alerting queries | PASS — excellent design |

The partial indexes are notably well-designed. `idx_sagas_waiting` covers the three waiting states (`AWAITING_CUSTOMER_DECISION`, `WAITING_FOR_INVENTORY`, `WAITING_FOR_CREDIT`) in a small index. A timeout scanner queries `WHERE status IN (...)` — the index is an exact match and never bloats with completed/cancelled rows. `idx_sagas_failed_comp` similarly keeps the failed-compensation index small, serving operational alerting queries efficiently.

---

## Seed Strategy

### Design

Strategy B — standalone SQL file at `infra/docker/seed/seed.sql`. Four scenarios:
- `seed-001`: Completed order (multi-warehouse, APPROVED credit)
- `seed-002`: Awaiting customer decision (partial reservation, 120/200 SKU-WIDGET-A)
- `seed-003`: Cancelled (REJECTED credit, RELEASED stock)
- `seed-004`: Failed compensation (stock RESERVED but uncompensated, no credit)

### Safety Assessment

| Check | Result |
|-------|--------|
| Never auto-loaded | PASS — explicit `reset.sh --seed` required |
| Not on classpath | PASS — `infra/docker/seed/`, not under `src/main/resources` |
| Not referenced by application code | PASS |
| Idempotent | PASS — all inserts use `ON CONFLICT DO NOTHING` |
| Production-safe | PASS — no path to production execution |

### Warning — Maintainability

No automated test validates that `seed.sql` remains compatible with the current schema. If a future migration adds a NOT NULL column, `seed.sql` will silently fail on the next `reset --seed`. A low-cost CI check that runs `seed.sql` against a fresh migrated database would catch this regression before it blocks development.

---

## Test Strategy

### FlywayMigrationIT (4 tests)

Each test:
1. Starts a Testcontainers PostgreSQL container
2. Runs `test-db/create-<service>-schema.sql` as init script (creates the schema)
3. Boots Spring with `ddl-auto=validate` — Flyway runs migrations
4. Asserts tables exist, version columns exist, context loads

Coverage is adequate for the migration path. Every table and the `version` column are verified via `DatabaseMetaData`. The `context_loads_and_hibernate_validates` assertion confirms Hibernate accepted the migrated schema.

### Warning — Maintainability

No integration test exercises a business operation through the migrated database. FlywayMigrationIT checks metadata and Hibernate validation but never calls `repository.save()` or an application service against a migrated schema. A single end-to-end test per service — migrate → create aggregate → read back — would close this gap at minimal cost.

### Warning — Maintainability

`infra/docker/init-db.sql` (schema creation + role grants) and `infra/docker/seed/seed.sql` have zero automated test coverage. Both are critical to the local development workflow (`reset.sh` depends on them) but are only exercised manually. A CI job that creates a fresh Postgres, runs `init-db.sql`, and asserts schemas/roles exist would prevent regression.

---

## Performance

All 14 indexes are justified by known access patterns:

| Service | Index | Justification |
|---------|-------|---------------|
| order | `idx_orders_customer_id` | Customer-facing order listing |
| order | `idx_orders_status` | Status-based queries |
| order | `idx_order_lines_order_id` | FK join |
| inventory | `idx_stock_reservations_order_id` | Lookup by order |
| inventory | `idx_stock_reservations_status` | Status filtering |
| inventory | `idx_srl_reservation_id` | FK join |
| inventory | `idx_sa_line_id` | FK join |
| inventory | `idx_sa_warehouse_sku` | Warehouse availability queries |
| credit | `idx_credit_reservations_order_id` | Lookup |
| credit | `idx_credit_reservations_status` | Status filtering |
| orchestrator | `idx_sagas_order_id` | Lookup |
| orchestrator | `idx_sagas_status` | Status polling |
| orchestrator | `idx_sagas_current_step` | Step-based queries |
| orchestrator | `idx_sagas_waiting` (partial) | Timeout scanning |
| orchestrator | `idx_sagas_failed_comp` (partial) | Alerting |

No additional indexes recommended at this stage. The partial indexes are space-efficient and directly serve known query patterns (timeout scanning, operational alerting).

---

## Future Evolution

| Task | Impact Assessment |
|------|-------------------|
| **ARB-021 (Kafka)** | No schema constraints on topic design. Each service adds Kafka config independently. |
| **ARB-022 (Outbox)** | Adding `V2__add_outbox_table.sql` per service is trivial — each owns its schema. |
| **ARB-023 (REST)** | Status columns and indexes already support dashboard and order-listing queries. |
| **ARB-024 (Keycloak)** | `submitted_by` and `customer_id` are generic `VARCHAR(255)` — naturally hold Keycloak UUIDs. The `keycloak` database/user/schema exist in `init-db.sql`. |
| **ARB-025 (Observability)** | Partial indexes on waiting/failed states directly support metrics queries without schema changes. |
| **ARB-004B (Native Image)** | See warning below. |

---

## Native Image

### Warning — Future evolution

Flyway migration scripts are classpath resources and handled automatically by Spring Boot AOT processing. However, Flyway's internal class scanning for `JavaMigration` resolvers may require a `RuntimeHintsRegistrar` for GraalVM native image. This is documented as an OPEN QUESTION in the implementation report and deferred to the native build task (ARB-004B / ADR-0007). Not a current blocker but should be verified before native image production deployment.

---

## Documentation Consistency

| Source | Check | Result |
|--------|-------|--------|
| Implementation report tables/columns | Match migration SQL | PASS |
| Implementation report indexes | Match migration SQL | PASS |
| Implementation report configuration changes | Match `application.yml` | PASS |
| `README.md` infra commands | Match `infra/docker/` scripts | PASS |
| Migration file comments | Match actual DDL | PASS |

The implementation report accurately reflects the source code. All schema details, index definitions, and configuration values are consistent.

---

## Warning Summary

| # | Classification | Finding |
|---|---------------|---------|
| W1 | Maintainability | No automated test for `seed.sql` compatibility with current schema — a migration adding a NOT NULL column would silently break `reset --seed` |
| W2 | Maintainability | No integration test exercising business operations through a migrated database — `FlywayMigrationIT` checks metadata only |
| W3 | Maintainability | `init-db.sql` and `seed.sql` have zero automated test coverage despite being critical to local development workflow |
| W4 | Future evolution | Flyway `RuntimeHintsRegistrar` may be needed for GraalVM native image (already documented as OPEN QUESTION; verify at ARB-004B) |

---

## Relation to Other Reviews

| Task | Relation |
|------|---------|
| **ARB-019** | Provided JPA entities; migrations replicate their expected schema. Adapter tests use `ddl-auto=create-drop` — verified consistent with migration DDL. |
| **ARB-019-FIX-001** | Transaction boundaries moved to app services; no schema impact. |
| **ARB-027** | `init-db.sql` creates schemas and roles; migrations depend on schema existing. Confirmed no overlap or conflict. |
