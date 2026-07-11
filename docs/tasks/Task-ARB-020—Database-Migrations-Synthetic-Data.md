Task: ARB-020 — Database Migrations & Synthetic Data

Status:
[PLANNED]

Owner:
Clio

Context:

ARB-019 Persistence Adapters is DONE.

The project now has:

- JPA entities
- Spring Data repositories
- persistence mappers
- PostgreSQL schemas
- optimistic locking
- transactional application services
- local PostgreSQL runtime from ARB-027

What is still missing is a reproducible, versioned database schema.

ARB-020 introduces database migrations and optional local synthetic business data.

Goal:

Create Flyway migrations for all persistence models and provide clearly separated,
development-only synthetic data for local testing.

Primary modules:

- server/order-service
- server/inventory-service
- server/credit-service
- server/orchestrator-service

Supporting changes allowed:

- root/server POMs
- application configuration
- infra/docker
- docs

--------------------------------------------------

1. Migration technology

Use Flyway.

Do not use Hibernate ddl-auto to create or mutate production schemas.

Set:

spring.jpa.hibernate.ddl-auto=validate

for persistence-enabled runtime profiles.

Flyway is the source of truth for schema evolution.

--------------------------------------------------

2. Schema ownership

Use the existing PostgreSQL schemas:

- order_service
- inventory_service
- credit_service
- orchestrator_service

Each service owns its schema.

Do not create cross-schema foreign keys.

Do not allow one service migration to alter another service schema.

--------------------------------------------------

3. Migration layout

Prefer one migration location per service.

Example:

server/order-service/src/main/resources/db/migration/order_service/

server/inventory-service/src/main/resources/db/migration/inventory_service/

server/credit-service/src/main/resources/db/migration/credit_service/

server/orchestrator-service/src/main/resources/db/migration/orchestrator_service/

Use clear versioned names:

V1__create_order_tables.sql

V1__create_inventory_tables.sql

V1__create_credit_tables.sql

V1__create_saga_tables.sql

If multiple migrations are needed, keep them incremental and ordered.

--------------------------------------------------

4. Order Service migrations

Create tables required by current JPA model.

Expected concepts:

- orders
- order_lines

Persist:

- order id
- customer id
- submitted-by user id
- status
- money amount
- currency
- cancellation reason
- optimistic lock version
- timestamps if modeled

Requirements:

- primary keys
- order-line ownership
- line ordering if needed
- indexes for business queries
- enum storage strategy aligned with entity mappings
- no cross-service foreign keys

--------------------------------------------------

5. Inventory Service migrations

Create tables for:

- stock_reservations
- stock_reservation_lines
- stock_allocations

Persist:

- reservation id
- order id
- status
- requested quantities
- warehouse allocations
- optimistic lock version
- timestamps if modeled

Requirements:

- one reservation owns many lines
- one line owns many allocations
- allocation quantity constraints
- useful indexes for reservation lookup and warehouse/SKU queries
- do not duplicate derived reservedQuantity unless already required by entity mapping

If duplicated, document why and add a consistency constraint where possible.

--------------------------------------------------

6. Credit Service migrations

Create table(s) for:

- credit_reservations

Persist:

- credit reservation id
- order id
- customer id if present
- amount
- currency
- status
- optimistic lock version
- timestamps if modeled

Do not create a credit-limit table unless the current JPA model requires it.

CreditLimitPort remains external.

--------------------------------------------------

7. Orchestrator Service migrations

Create table(s) for:

- sagas

Persist all saga state currently mapped by JPA:

- saga id
- order id
- customer id
- status
- current step
- customer decision
- stock reservation id
- credit reservation id
- version
- timestamps if modeled

Add indexes for:

- saga status
- current step
- order id
- waiting states
- failed compensation states

These indexes should support future timeout scanning and operations dashboard queries.

--------------------------------------------------

8. Constraints

Add database constraints where they reinforce domain invariants without duplicating
complex business logic.

Examples:

- non-null identifiers
- positive quantities
- non-negative monetary values
- non-negative allocation quantities
- valid enum values if stored as text
- uniqueness where required

Do not attempt to encode the full saga state machine in SQL constraints.

--------------------------------------------------

9. Flyway configuration

Configure each service to run only its own migration location and schema.

Preferred settings:

- flyway.default-schema
- flyway.schemas
- flyway.locations
- flyway.validate-on-migrate=true
- flyway.clean-disabled=true outside test/local reset contexts

Do not let Flyway manage Keycloak tables.

--------------------------------------------------

10. Hibernate validation

Ensure Hibernate validates mappings against migrated schemas.

Use:

spring.jpa.hibernate.ddl-auto=validate

Add tests proving:

- migrations run
- JPA mappings validate
- application context starts against PostgreSQL Testcontainers

Do not use create-drop as the production/default strategy.

--------------------------------------------------

11. Synthetic data

Provide optional, local-only synthetic business data.

Keep schema migrations and data seeding separate.

Preferred options:

A.
Flyway repeatable/local-profile migrations

or

B.
Dedicated SQL seed scripts under:

infra/docker/seed/

Choose the smallest clean approach.

Synthetic data may include:

- ACME customer/company identifiers
- sample orders
- sample inventory stock/allocation data if a persistence model exists
- sample credit reservations
- sample sagas in useful statuses

Important:

- do not seed real customer data
- do not seed production credentials
- do not run synthetic data automatically in production
- clearly gate seed execution to local/dev/test profile

--------------------------------------------------

12. Useful local scenarios

Seed enough data to exercise:

- one confirmed order
- one active/waiting saga
- one cancelled order
- one failed compensation saga
- one inventory reservation split across multiple warehouses
- one approved credit reservation
- one rejected credit reservation

Do not overbuild a large catalog.

--------------------------------------------------

13. Idempotency and reset

Migrations must be idempotent through Flyway version tracking.

Seed reset must be documented.

Coordinate with ARB-027 reset scripts.

Preferred local reset flow:

infra/docker/reset.sh
↓
docker compose up
↓
Flyway migrations
↓
optional seed load

--------------------------------------------------

14. Testing

Add migration integration tests using PostgreSQL Testcontainers.

Cover per service:

- clean database migration
- Hibernate validation
- repository save/load after migration
- schema ownership
- expected tables/columns/indexes exist
- optimistic-lock version column exists

Add at least one schema assertion for each service.

Test failure cases where useful:

- migration checksum mismatch documentation
- missing migration validation
- invalid schema configuration

Do not require the manually running Docker Compose stack.

--------------------------------------------------

15. Native Image / AOT

No special native-image work is expected.

Document any Flyway resource inclusion implications for native images.

Ensure migration resources are packaged.

Do not add RuntimeHints unless required and demonstrated.

--------------------------------------------------

16. Documentation

Create:

docs/implementation/ARB-020-database-migrations-and-synthetic-data.md

Update:

- server/order-service/README.md
- server/inventory-service/README.md
- server/credit-service/README.md
- server/orchestrator-service/README.md
- infra/docker/README.md
- docs/okf/index.md
- relevant RNF/ADR references

Document:

- migration ownership
- schema locations
- profile behavior
- seed strategy
- reset procedure
- production safety
- relation to ARB-019 and ARB-027

--------------------------------------------------

Out of scope:

- No Kafka adapters.
- No Outbox/Inbox.
- No REST controllers.
- No Keycloak integration changes.
- No Kubernetes.
- No Terraform.
- No CI/CD.
- No production business data.
- No cross-schema joins.
- No ARB-021 work.

--------------------------------------------------

Acceptance Criteria:

- Flyway migrations exist for all four services.
- Each service migrates only its own schema.
- Hibernate runs with ddl-auto=validate.
- JPA mappings validate against PostgreSQL.
- Optimistic lock columns exist.
- Inventory multi-warehouse allocations persist correctly.
- Saga indexes support status/waiting queries.
- Local-only synthetic data is optional and isolated.
- Testcontainers migration tests pass.
- Existing 536+ tests remain green.
- No production schema is created by Hibernate.
- Documentation is complete.
- Ready for Deep review.

After completion:

Report:

- created migration files
- schemas/tables/indexes/constraints
- seed strategy
- application config changes
- tests run
- reset procedure
- open questions

Do not start ARB-021.