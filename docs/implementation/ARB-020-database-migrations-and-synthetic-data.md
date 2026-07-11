# ARB-020 — Database Migrations & Synthetic Data

## Summary

Flyway is the source of truth for schema evolution across all four services.
Hibernate runs with `ddl-auto=validate`; it never creates or alters tables in production.

## Migration ownership

| Service | Schema | Location |
|---------|--------|----------|
| order-service | `order_service` | `classpath:db/migration/order_service/` |
| inventory-service | `inventory_service` | `classpath:db/migration/inventory_service/` |
| credit-service | `credit_service` | `classpath:db/migration/credit_service/` |
| orchestrator-service | `orchestrator_service` | `classpath:db/migration/orchestrator_service/` |

Each service migrates only its own schema. No cross-schema foreign keys.

## Tables created

### order_service

- `orders` — order aggregate root; `@Version` column `version`
- `order_lines` — owned by `orders`, replaced atomically on update

### inventory_service

- `stock_reservations` — reservation aggregate root; `@Version` column `version`
- `stock_reservation_lines` — one per SKU requested; owned by `stock_reservations`
- `stock_allocations` — warehouse-level allocation detail; owned by `stock_reservation_lines`

### credit_service

- `credit_reservations` — credit reservation aggregate; `@Version` column `version`
- `amount_value` stored as `NUMERIC(19,4)` matching entity `precision=19, scale=4`

### orchestrator_service

- `sagas` — saga aggregate; all state in explicit columns; `@Version` column `version`
- Partial indexes for waiting states and failed compensation support dashboard queries

## Flyway configuration

Configured per-service in `application.yml`:

```yaml
spring:
  flyway:
    locations: classpath:db/migration/<schema_name>
    default-schema: <schema_name>
    schemas: <schema_name>
    validate-on-migrate: true
    clean-disabled: true
```

`clean-disabled: true` prevents `flyway clean` in any non-reset context.
Schema is created by `infra/docker/init-db.sql` (local) or Testcontainers init script (tests).

## Hibernate validation

`spring.jpa.hibernate.ddl-auto=validate` is set in each `application.yml`.
At startup, Hibernate validates all `@Entity` mappings against the migrated schema.
Any mismatch prevents the service from starting.

## Test strategy

### Existing Testcontainers adapter tests

`Jpa*RepositoryAdapterTest` tests add `spring.flyway.enabled=false` and keep `ddl-auto=create-drop`.
These tests focus on JPA adapter behavior; schema creation is intentionally handled by Hibernate.

### Migration integration tests

`FlywayMigrationIT` (one per service) verifies:

- Flyway migrates cleanly against a fresh PostgreSQL Testcontainers container
- Hibernate validates mappings (`ddl-auto=validate`)
- Tables, version columns, and key columns exist via `DatabaseMetaData`
- Application context starts successfully

## Synthetic data

Strategy: B — standalone SQL file `infra/docker/seed/seed.sql`.

Scenarios seeded:

| ID | Scenario |
|----|----------|
| seed-001 | ACME confirmed order (COMPLETED saga, multi-warehouse allocation) |
| seed-002 | ACME order waiting for customer decision (partial reservation, 2 warehouses) |
| seed-003 | BETA cancelled order (INSUFFICIENT_CREDIT) |
| seed-004 | ACME failed compensation saga |

Seed data uses `INSERT ... ON CONFLICT DO NOTHING` for idempotency.
Not loaded automatically. Load explicitly via `reset.sh --seed` or:

```bash
psql -U arbitrier_admin -d arbitrier -f infra/docker/seed/seed.sql
```

## Reset procedure

```bash
./infra/docker/reset.sh           # wipe volumes, restart stack
./infra/docker/reset.sh --seed    # reset + load seed data
```

Flyway migrations run automatically when each service starts after the reset.

## Production safety

- `flyway.clean-disabled=true` in all `application.yml` files
- Seed data is never loaded automatically
- `ddl-auto=validate` prevents Hibernate from altering production schema
- No cross-service data or foreign keys

## Relation to other tasks

- **ARB-019**: Introduced JPA entities, repositories, and persistence adapters that these migrations support
- **ARB-027**: Local runtime stack (`infra/docker`) that these migrations complement
- **ARB-021**: Outbox/Inbox pattern deferred; when introduced it will add V2 migrations

## Native Image note

Flyway migration scripts packaged under `classpath:db/migration/` are included in the Spring Boot fat JAR and are available at runtime. No additional `RuntimeHintsRegistrar` is required for standard classpath migrations.

OPEN QUESTION: Verify Flyway + GraalVM native image compatibility when native builds are introduced (ADR-0007).
