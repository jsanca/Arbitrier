# ARB-021 — Outbox / Inbox Foundation

**Status: COMPLETE** (2026-07-11)

## Intention

Introduce the platform-wide transactional outbox and inbox skeleton so that every service persists domain events atomically with aggregate writes and can deduplicate consumed events. This slice delivers the shared building blocks; a later slice will wire the Kafka drainer that publishes pending outbox rows.

## Context

Prior to this slice each application service depended on service-specific `*EventPublisher` outbound ports whose adapters wrote directly to Kafka. That coupling risked losing events on rollback and pushed publication concerns into the application layer.

ARB-021 replaces every event publisher used inside application services with an `OutboxRepository` port. Events are serialized to JSON and stored as rows in an `outbox_events` table that lives in the same database schema as the aggregate. A parallel `inbox_events` table lets consumers deduplicate replayed messages.

## Decision or Requirement

- The `platform` module owns the outbox and inbox domain model, ports, JPA adapters, and the JSON serializer.
- Each service schema (order_service, credit_service, inventory_service, orchestrator_service) receives an unqualified Flyway migration (`V2__create_outbox_inbox_tables.sql`) supplied by the platform module.
- Application services write domain events to the outbox via `outboxRepository.save(mapper.map(event, aggregateId, aggregateType))` inside the same `@Transactional` boundary as the aggregate save.
- The existing service-specific `*EventPublisher` interfaces are no longer wired into services; the Kafka adapters remain in the codebase but are unused pending the outbox-drainer slice.
- Command publishers (`ReserveStockCommandPublisher`, `ReserveCreditCommandPublisher`, `ConfirmOrderCommandPublisher`, `ReleaseStockCommandPublisher`, `ReleaseCreditCommandPublisher`) stay unchanged — they carry saga commands, not domain events.

## Inputs

- Existing hexagonal ports, JPA persistence adapters, Flyway migrations, service schemas.
- Spring Boot 4.1.0 auto-configuration and conditional-bean support.

## Outputs

New platform packages under `server/platform/src/main/java/com/arbitrier/platform/messaging/`:

- `serialization/` — `EventSerializer`, `JacksonEventSerializer`
- `outbox/` — `PublishStatus`, `OutboxEvent` record, `OutboxRepository` port, placeholder `OutboxPublisher`
- `outbox/adapter/` — `OutboxEventEntity`, `SpringDataOutboxRepository`, `JpaOutboxRepositoryAdapter`
- `outbox/mapper/` — `DomainEventToOutboxMapper`
- `inbox/` — `ProcessingStatus`, `InboxEvent` record, `InboxRepository` port
- `inbox/adapter/` — `InboxEventEntity`, `SpringDataInboxRepository`, `JpaInboxRepositoryAdapter`
- `test/` — `InMemoryOutboxRepository`, `InMemoryInboxRepository`

Platform migration:

- `server/platform/src/main/resources/db/migration/platform/V2__create_outbox_inbox_tables.sql` — creates `outbox_events` and `inbox_events` with indexes and CHECK constraints. Table names are unqualified; Flyway resolves the target schema via `default-schema` in each service's `application.yml`.

Platform auto-configuration:

- `PlatformAutoConfiguration` now registers `EventSerializer` (Jackson) and `DomainEventToOutboxMapper` beans when `spring-data-jpa` is on the classpath.
- A canonical `messagingObjectMapper` bean provides the `ObjectMapper` used by `JacksonEventSerializer`, allowing centralized Jackson configuration (JavaTime, JSpecify, etc.) without exposing `ObjectMapper` to application code.
- The `EventSerializer` interface is the only messaging serialization contract visible to application and domain code; Jackson-specific types remain encapsulated in the `platform.messaging.serialization` package.

Service changes (order, credit, inventory, orchestrator):

- `application.yml` — Flyway `locations` now include `classpath:db/migration/platform` alongside the service-owned migrations; `hibernate.default_schema` is set per service; `spring.jpa.packages` explicitly lists the service's entity package and the platform outbox/inbox adapter packages.
- Main application classes — `@EntityScan` annotation removed (Spring Boot 4.x removed this annotation). Entity scanning is handled via `spring.jpa.packages` property.
- `*PersistenceConfiguration` — declare `outboxRepository` and `inboxRepository` beans backed by the JPA adapters, and enable `@EnableJpaRepositories(basePackageClasses = {...})` with marker classes for the service's Spring Data repository and the platform outbox/inbox repositories.
- Application services — every event publisher parameter replaced by `OutboxRepository` + `DomainEventToOutboxMapper`. Aggregate types are constant strings: `"Order"`, `"CreditReservation"`, `"StockReservation"`, `"Saga"`.
- Test configurations and unit tests — swap recording publishers for `InMemoryOutboxRepository` / `InMemoryInboxRepository` and assert on `outboxRepository.findAll()` records.

New integration test:

- `server/order-service/src/test/java/com/arbitrier/order/adapter/outbound/persistence/TransactionalOutboxIT.java` — proves that an order aggregate and an outbox event share a single transaction against a Testcontainers PostgreSQL instance driven by real Flyway migrations.

## Preconditions

- The database schema for each service exists before Flyway migrates.
- The service classpath contains both `db/migration/platform` and `db/migration/<service>` resources.
- Every domain event referenced by a service is JSON-serializable via a default `ObjectMapper` (the platform serializer constructs its own `ObjectMapper`).

## Postconditions

- Application services write events into `outbox_events` inside the same transaction as the aggregate change. Rollback of the aggregate write also rolls back the outbox row.
- Consumers may record processed events into `inbox_events`; a Kafka drainer will translate pending outbox rows into published messages in a subsequent slice.
- The unit-test suite exercises the outbox pattern through `InMemoryOutboxRepository`. Integration and JPA adapter tests exercise it against Testcontainers PostgreSQL.

## Failure Behavior

- If serialization of a domain event fails, `JacksonEventSerializer` wraps the underlying `JsonProcessingException` in an `IllegalArgumentException` and the transaction rolls back — the aggregate is not persisted.
- If the outbox insert fails due to a database constraint (bad status, missing schema, etc.), the entire transaction rolls back.
- `JpaOutboxRepositoryAdapter.markPublished` / `markFailed` throw `IllegalArgumentException` when the referenced event ID is unknown — the future drainer must not call these methods for events it has not observed.

## Observability Expectations

- Every service continues to log aggregate transitions with `sagaId` / `orderId` MDC context. The outbox row carries `correlation_id` and `causation_id` columns for future propagation.
- OPEN QUESTION: how correlation and causation IDs are threaded from inbound REST/Kafka to outbox rows will be resolved when the drainer slice adds a request-scoped propagator.

## Test Evidence Placeholder

- Unit tests updated: `SubmitCorporateBulkOrderServiceTest`, `ReserveCreditServiceTest`, `ReleaseCreditServiceTest`, `ReserveStockServiceTest`, `ReleaseStockServiceTest`, and every orchestrator `Handle*ServiceTest` / `StartSagaServiceTest` / `AdvanceSagaServiceTest` / `CompensateSagaServiceTest`.
- Integration tests updated: service `*TestConfiguration` classes now bind in-memory outbox / inbox beans.
- Adapter integration: `TransactionalOutboxIT` (new) plus the existing JPA adapter tests continue to pass against Testcontainers PostgreSQL now that Flyway runs the platform migration in each service schema.
- Platform module tests added:
  - `OutboxEventTest` — validates OutboxEvent record creation, null/blank guards, and nullable field behavior.
  - `InboxEventTest` — validates InboxEvent record creation, null/blank guards, and nullable field behavior.
  - `JacksonEventSerializerTest` — validates round-trip serialization/deserialization, null guard, and JSON output format.
  - `DomainEventToOutboxMapperTest` — validates domain event → OutboxEvent mapping, unique event IDs, payload content, and null/blank guards.
  - `InMemoryOutboxRepositoryTest` — validates save, findPending, markPublished, markFailed, dedup-on-save, clear, findAll immutability.
  - `InMemoryInboxRepositoryTest` — validates save, findById, markProcessed, dedup-on-save, clear, findAll immutability.
  - `JpaOutboxRepositoryAdapterTest` — validates adapter against real PostgreSQL: save/find, markPublished, markFailed, increment attempts, unknown-ID guards. Uses `@SpringBootTest` + Testcontainers (Boot 4.1 removed `@DataJpaTest`).
  - `JpaInboxRepositoryAdapterTest` — validates adapter against real PostgreSQL: save/find, markProcessed, unknown-ID guards, multi-event storage. Uses `@SpringBootTest` + Testcontainers.
- Architecture test: `PlatformArchitectureTest` updated with two new rules covering messaging domain purity (no Spring/JPA in outbox/inbox/serialization packages) and adapter isolation (no business domain dependencies in outbox/inbox adapter packages), plus two rules verifying `ObjectMapper` is confined to Jackson infrastructure and messaging public API exposes only `EventSerializer`.
- Build gate: `mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service,server/credit-service,server/inventory-service,server/orchestrator-service -Dtest="!*IT" -Dsurefire.failIfNoSpecifiedTests=false` — all suites green (platform 83→156 tests).

## Open Questions

- OPEN QUESTION: the Kafka outbox drainer / dispatcher is out of scope for ARB-021; the `OutboxPublisher` interface is a marker for the next slice.
- OPEN QUESTION: correlation and causation IDs are currently written as `null`; wiring them from the correlation filter and Kafka consumer to the outbox mapper is deferred.
- OPEN QUESTION: schema-registry-backed Avro serialization for outbox payloads (`payload_format = "AVRO"`) will replace the JSON default when the contracts-first serializer is chosen (see ARB-011 open question).
