Task: ARB-020-FIX-001 — Persistence Validation Improvements

Status:
[PLANNED]

Owner:
Clio

Context:

ARB-020 Database Migrations & Synthetic Data received:

PASS WITH WARNINGS

Deep found no architecture, correctness, or performance blockers.

The remaining actionable warnings are:

- seed.sql compatibility is not automatically verified
- init-db.sql is not automatically verified
- no business operation is exercised through a Flyway-migrated database

The GraalVM/Flyway native-image concern remains intentionally deferred to ARB-004B.

Goal:

Add automated persistence/runtime validation that protects the local database workflow and proves that migrated schemas support real repository operations.

Scope:

- migration integration tests
- local runtime SQL verification tests/scripts
- documentation updates

No production behavior changes.

--------------------------------------------------

1. Migrated-database repository round-trip tests

For each service:

- order-service
- inventory-service
- credit-service
- orchestrator-service

Extend the Flyway migration integration test or add a dedicated integration test that performs:

clean PostgreSQL Testcontainer
↓
create service schema
↓
run Flyway migrations
↓
Hibernate validate
↓
save a real domain aggregate through the JPA repository adapter
↓
load it back through the repository port
↓
assert complete domain round-trip

Required scenarios:

Order:
- save and load an Order with lines
- verify status, customer/user IDs, quantities, and version

Inventory:
- save and load a StockReservation
- include multi-warehouse StockAllocation entries
- verify derived reserved quantities after reconstruction

Credit:
- save and load CreditReservation
- verify Money amount/currency and status

Orchestrator:
- save and load Saga
- verify waiting/compensating state and reservation IDs

Use the migrated schema.

Do not use ddl-auto=create-drop in these tests.

--------------------------------------------------

2. seed.sql compatibility test

Add an automated integration test or validation script that:

- starts a fresh PostgreSQL Testcontainer
- creates the required service schemas
- runs all Flyway migrations
- executes infra/docker/seed/seed.sql
- verifies execution succeeds
- verifies the four documented seed scenarios exist

Minimum assertions:

- confirmed order scenario exists
- waiting saga scenario exists
- cancelled/credit-rejected scenario exists
- failed-compensation scenario exists
- multi-warehouse allocation rows exist

The test must fail if seed.sql becomes incompatible with future migrations.

Do not duplicate the seed data inside Java test code.

Execute the repository file directly.

--------------------------------------------------

3. init-db.sql verification

Add an automated integration test or script that runs:

infra/docker/init-db.sql

against a fresh PostgreSQL instance and verifies:

- arbitrier database exists
- keycloak database exists
- expected roles/users exist
- schemas exist:
    - order_service
    - inventory_service
    - credit_service
    - orchestrator_service
    - platform
- schema ownership is correct
- public schema creation privileges are restricted as documented
- no business tables are created by init-db.sql

Prefer a Testcontainers-based test or a small CI-safe shell verification.

Do not require the manually running Docker Compose stack.

--------------------------------------------------

4. Test organization

Keep concerns separated:

- FlywayMigrationIT:
  migration + Hibernate validation

- RepositoryRoundTripIT:
  migrated schema + real adapter operation

- LocalRuntimeSqlIT or equivalent:
  init-db.sql + seed.sql compatibility

Avoid creating one oversized integration test.

Reuse container setup where practical without making test ordering significant.

--------------------------------------------------

5. File-path handling

Tests must reference the real repository files:

- infra/docker/init-db.sql
- infra/docker/seed/seed.sql

Do not copy these SQL files into test resources.

Resolve paths robustly from the Maven module/repository root.

If path resolution is non-trivial, create a small shared test utility with explicit failure messages.

--------------------------------------------------

6. Idempotency checks

Verify where appropriate:

- init-db.sql can be applied according to its documented lifecycle or fails in a documented and expected way
- seed.sql can be executed twice without duplicate-key failure
- seed uses ON CONFLICT behavior correctly

Do not force init-db.sql to be rerunnable if PostgreSQL role/database creation semantics make that inappropriate; document the intended reset lifecycle instead.

--------------------------------------------------

7. CI behavior

Ensure the new tests:

- run with Maven verify
- use Testcontainers
- require no manually started Docker Compose stack
- have deterministic cleanup
- do not depend on test execution order
- do not leave containers or temporary files behind

The existing Surefire JVM shutdown warnings may remain if tests pass, but do not introduce hangs.

--------------------------------------------------

8. Documentation

Update:

- docs/implementation/ARB-020-database-migrations-and-synthetic-data.md
- infra/docker/README.md
- relevant service README files if test commands change

Document:

- repository round-trip coverage
- seed compatibility validation
- init-db.sql validation
- how to run the tests
- Native Image warning remains deferred

Add a review-fix section:

ARB-020-FIX-001

Resolve:

- W1 seed.sql compatibility
- W2 migrated-database business operation
- W3 init-db.sql verification

Keep W4 open:

- Flyway/GraalVM compatibility to be verified in ARB-004B

--------------------------------------------------

Out of scope:

- No migration schema changes unless a test reveals a real defect.
- No new business data.
- No Kafka.
- No Outbox/Inbox.
- No REST.
- No Keycloak application integration.
- No Docker Compose redesign.
- No Native Image build.
- No RuntimeHints.
- No ARB-021 work.

--------------------------------------------------

Acceptance Criteria:

- each service performs a repository round trip against a Flyway-migrated PostgreSQL schema
- seed.sql runs successfully against fresh migrated schemas
- seed.sql is verified idempotent
- all documented seed scenarios are asserted
- init-db.sql is automatically verified
- schema ownership and restrictions are asserted
- no business tables are created by init-db.sql
- all tests run without a manually started local stack
- full Maven build passes
- ARB-020 warnings W1–W3 are resolved
- Native Image warning remains explicitly deferred
- documentation is updated

After completion:

Report:

- created/modified test files
- SQL files executed
- assertions added
- test totals
- execution time
- remaining warnings/open questions

Do not start ARB-021.