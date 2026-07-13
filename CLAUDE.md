# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Operating Instructions

Work documentation-first. Read the relevant OKF, RF, RNF, ADR, and test-case files before coding. Do not invent business behavior; mark missing details as `OPEN QUESTION`. Keep changes scoped to the requested slice.

For non-trivial implementation tasks apply the execution-timebox skill: target 20–30 min, warn at 30 min, hard stop at 45 min with a recovery checkpoint. See [`.claude/skills/execution-timebox/SKILL.md`](.claude/skills/execution-timebox/SKILL.md).

Every non-trivial implementation, review, recovery, architecture, security, or documentation task must also apply the [engineering reporting protocol](.claude/skills/engineering-reporting/SKILL.md). Use its canonical task/report/review/checkpoint locations and consult [documentation ownership](docs/engineering/documentation-ownership.md) rather than duplicating the system narrative here. When a task completes, add a row to `ENGINEERING_LOG.md` linking task, report, review, and any fix artifacts.

---

## Build Commands

```bash
# Server — all modules
mvn -B verify --no-transfer-progress

# Server — modules with active implementation (fastest feedback loop)
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service,server/inventory-service,server/credit-service,server/orchestrator-service

# Server — single module
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service

# Server — single test class
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service -Dtest=SubmitCorporateBulkOrderServiceTest

# Server — single test method
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service -Dtest=SubmitCorporateBulkOrderServiceTest#missing_customer_id_throws

# Client
cd client && npm ci && npm run build && npm test

# E2E (Playwright, against running stack)
cd client && npx playwright test
cd client && npx playwright test e2e/uc01-confirmed.spec.ts

# Local infrastructure (Kafka, PostgreSQL, Keycloak, Schema Registry)
docker compose -f infra/docker/docker-compose.yml up -d
docker compose -f infra/docker/docker-compose.yml down -v
```

Local stack ports: PostgreSQL 5432, Kafka 9092, Schema Registry 8081, Keycloak 8180, Kafka UI 8088.

Dependent modules must always be included when running a service test. `server/contracts` and `server/platform` must appear before any service in the `-pl` list.

`*IT.java` files run under Surefire (`mvn test`), not Failsafe. This is intentional — do not add a Failsafe plugin.

---

## Repository Layout

```
server/
  order-service/        Order aggregate; saga entry point
  inventory-service/    Stock reservation and compensation
  credit-service/       B2B credit limit validation
  orchestrator-service/ Saga coordinator
  contracts/            Avro schemas + OpenAPI specs (shared)
  platform/             Library jar — security, observability, exceptions
client/                 React 19 / TypeScript portal
docs/
  agents/               Tasks, reports, reviews, checkpoints, templates (canonical — not docs/tasks/)
  engineering/          Role-independent process rules and documentation ownership
  implementation/       Capability description documents (architecture rationale, not task records)
  okf/                  Use-case narratives (UC-NN) and project index
  rf/                   Functional requirements (RF-UC-NN)
  rnf/                  Non-functional requirements
  adr/                  Architecture decision records
  test-cases/           Test case specs (TC-UC-NN and TC-UC-NN-NNN)
infra/
  docker/               Local docker-compose + init-db.sql
  k8s/                  Kubernetes manifests
  terraform/            GCP infrastructure
  strimzi/              Kafka on Kubernetes
```

---

## Hexagonal Architecture (every server service)

```
com.arbitrier.<service>/
  adapter/
    inbound/
      rest/        HTTP-driven adapters (Spring MVC controllers)
      kafka/       Event-driven adapters (Kafka consumers)
    outbound/
      persistence/ JPA-driven adapters (Spring Data repositories)
      kafka/       Kafka producer adapters
  application/
    port/
      inbound/     Use-case interfaces called by inbound adapters
      outbound/    Repository and messaging interfaces implemented by adapters
    service/       Use-case implementations — depend only on ports and domain
  domain/
    model/         Entities and value objects — zero Spring/JPA imports
    event/         Domain events
    command/       Commands
    exception/     Domain exceptions
  config/          Spring @Configuration — wires adapters to ports
  observability/   MDC helpers (sagaId, orderId, traceId) and OpenTelemetry spans
```

Dependency rule: `adapter → application → domain`. Nothing flows outward.  
`domain` must have **zero Spring, JPA, or Kafka imports**.

### `package-info.java` — required in every package

```java
/**
 * <One-sentence purpose.>
 *
 * <p>Layer: [domain/model | domain/event | application/port/inbound | adapter/inbound/rest | ...]
 * <p>Module: <service-name>
 */
package com.arbitrier.<service>.<layer>;
```

---

## UC-01 Context

Availability negotiation and a buyer’s partial-quantity decision occur before Order/Saga creation. Inventory owns warehouse selection. The active Saga coordinates authoritative inventory and credit work; see [RF-UC-01](docs/rf/RF-UC-01-corporate-bulk-order.md) and [ADR-0009](docs/adr/ADR-0009—GlobalInventoryAllocationOwnership.md) for current behavior.

---

## Documentation Conventions

All RF, RNF, ADR, and TC documents use the OKF template in this section order:

```
## Intention
## Context
## Decision or Requirement
## Inputs
## Outputs
## Preconditions
## Postconditions
## Failure Behavior
## Observability Expectations
## Test Evidence Placeholder
## Open Questions
```

Use `OPEN QUESTION:` (all caps) for unresolved items. Never invent values for open questions.

### Document naming

| Kind | Pattern | Example |
|------|---------|---------|
| Use case narrative | `docs/okf/UC-<NN>-<slug>.md` | `UC-01-corporate-bulk-order.md` |
| Functional requirement | `docs/rf/RF-UC-<NN>-<slug>.md` | `RF-UC-01-corporate-bulk-order.md` |
| Non-functional requirement | `docs/rnf/RNF-<NNNN>-<slug>.md` or `RNF-UC-<NN>-<slug>.md` | `RNF-UC-01-saga-runtime.md` |
| Architecture decision | `docs/adr/ADR-<NNNN>-<slug>.md` | `ADR-0002-orchestrated-saga-kafka.md` |
| Test case index | `docs/test-cases/TC-UC-<NN>-<slug>.md` | `TC-UC-01-corporate-bulk-order.md` |
| Test case detail | `docs/test-cases/TC-UC-<NN>-<NNN>-<slug>.md` | `TC-UC-01-001-create-pending-order.md` |

`docs/rf/RF-0001-corporate-bulk-order.md` is a legacy redirect from ARB-001. The canonical UC-01 RF is `RF-UC-01-corporate-bulk-order.md`.

---

## Naming Conventions

| Artifact | Pattern |
|----------|---------|
| Input port | `<Action><Subject>UseCase` (interface) |
| Output port | `<Subject>Repository`, `<Subject>Gateway` |
| Application service | `<Action><Subject>Service` |
| REST adapter | `<Subject>RestAdapter` |
| JPA adapter | `<Subject>JpaAdapter` |
| Kafka consumer adapter | `<Subject>KafkaConsumerAdapter` |
| Kafka producer adapter | `<Subject>KafkaProducerAdapter` |
| Domain event | `<Subject><PastTense>DomainEvent` |
| Avro schema file | `<subject>-<past-tense>-event-v<N>.avsc` |
| Kafka topic | `arbitrier.<domain>.<event>.v<N>` (e.g. `arbitrier.order.created.v1`) |

---

## Backend Style

- Java 25 · Spring Boot 4.1.0 · JPA for persistence adapters.
- `KafkaTemplate` for Kafka publishing when contracts exist.
- Resilience4j for retries, timeouts, and circuit breakers.
- SLF4J structured logs — every saga log line must include `sagaId`, `orderId`, `traceId`.
- OpenTelemetry SDK (no Java agent) — W3C Trace Context (`traceparent`/`tracestate`) is the only propagation standard (ADR-0008). Never introduce B3 headers (`X-B3-TraceId` etc.) as a first-class platform convention.
- `X-Correlation-Id` is a business-level identifier, distinct from `traceparent`. `CorrelationFilter` must not read or generate `traceparent`.
- Every public API has Javadocs.

## Frontend Style

- React 19 · TypeScript strict mode.
- Dashboard for saga status, event timeline, and pending customer decision.
- Playwright E2E for UC-01 visible workflows.
- Do not assume SSE or WebSocket until ADR-0006 is resolved.

## Testing Expectations

- Unit tests for domain and application rules.
- Integration tests for adapters, persistence, Kafka, and compensation behavior.
- Contract tests for Avro schemas after contracts exist.
- Playwright E2E for buyer and operator workflows.
- Idempotency tests for duplicate events and commands.
- TC-UC-01-001 through TC-UC-01-012 map to the test index in `docs/test-cases/TC-UC-01-corporate-bulk-order.md`.

## Scope Guardrails

- No production code before docs and tests define the slice.
- No Kafka consumers or producers before Avro contracts exist.
- No generated Avro sources committed — generated at build time only.
- No hidden business logic in adapters.
- No new final UC-01 states without updating the docs first.
- No bypassing Kafka/Avro decisions without an ADR.
- `platform` must have no business domain knowledge (no order/credit/inventory types).
- Callers of Inventory Service express only SKUs and quantities — never warehouse IDs. Warehouse selection is internal to Inventory (ADR-0009).
- `IdempotencyStore` has no production implementation yet — do not wire it as if it does.

## Native Image Compatibility (cross-cutting constraint — ADR-0007)

Native Image is a supported runtime variant. All future backend implementation must:

- Avoid unrestricted `Class.forName()`, dynamic proxy creation, or runtime classpath scanning without registering a `RuntimeHintsRegistrar` or equivalent native hint.
- Register all `@Entity` classes, Avro-generated classes, and Kafka message types for reflection at the time they are introduced.
- Register all runtime-read resources (`application.yml`, Flyway scripts, `.avsc` files) as native resources.
- Use OpenTelemetry SDK mode only (no Java agent) — the OTLP HTTP exporter must be used.
- Not add `native-maven-plugin` or GraalVM build configuration without a dedicated task.
- Document unresolved native incompatibilities as `OPEN QUESTION` in the implementation note.

See `docs/adr/ADR-0007-spring-aot-graalvm-native-image.md` and `docs/rnf/RNF-0002-native-image-runtime.md`.

---

## Spring Boot 4.x Patterns

These are non-obvious behaviors specific to Spring Boot 4.1.0 discovered during implementation.

### No `@WebMvcTest` — controller tests use `@SpringBootTest`

`spring-boot-test-autoconfigure` no longer includes a web servlet test slice. Controller tests must use:

```java
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@Import(OrderServiceTestConfiguration.class)
class MyControllerTest {
    @Autowired WebApplicationContext webApplicationContext;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }
}
```

### Bean override is disabled by default

Spring Boot 4.x rejects two bean definitions with the same name (`BeanDefinitionOverrideException`). Test configurations must not redefine beans already provided by main configuration. When a test context needs to substitute a bean that also exists in main config, mark the test bean `@Primary` — Spring picks the `@Primary` candidate for injection without triggering the override guard.

### No Kafka auto-configuration

Spring Boot 4.1.0 does not auto-configure `KafkaTemplate`. Services that publish to Kafka must define their own `ProducerFactory` and `KafkaTemplate` beans explicitly. Gate Kafka configuration with `@ConditionalOnProperty("spring.kafka.bootstrap-servers")` so the beans are absent in tests (where bootstrap-servers is not set) and test adapters fill the port instead.

### Virtual threads

All services set `spring.threads.virtual.enabled: true`. Do not create explicit thread pools for request handling.

### Flyway — schema-per-service

Each service owns its migrations under `src/main/resources/db/migration/<service>/` (e.g., `order_service/V1__create_order_tables.sql`). The platform owns shared tables in `platform/V2__create_outbox_inbox_tables.sql`. `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never creates or alters tables; Flyway is the sole schema owner. `spring.flyway.clean-disabled=true` always.

Flyway `locations` in `application.yml` lists both the platform path and the service path:
```yaml
spring.flyway.locations:
  - classpath:db/migration/platform
  - classpath:db/migration/order_service
```

### Security configuration

Every service is stateless: `SessionCreationPolicy.STATELESS`, CSRF disabled. `/actuator/**` is permit-all for Kubernetes health probes. REST controllers extract the caller's identity from the JWT subject — `authentication.getName()` — rather than accepting it as a request field (prevents identity spoofing).

### `@EntityScan` package moved in Spring Boot 4.1

In Spring Boot 4.1, `@EntityScan` lives in `org.springframework.boot.persistence.autoconfigure` (in the `spring-boot-persistence` artifact), **not** `org.springframework.boot.autoconfigure.domain`. Using the old package causes a compile error. Every `*PersistenceConfiguration` class must use:

```java
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@EntityScan(basePackageClasses = {ServiceEntity.class, OutboxEventEntity.class, InboxEventEntity.class})
```

This matters in JPA adapter Testcontainer tests that boot an inner `@SpringBootConfiguration` — `spring.jpa.packages` in `application.yml` is not effective in those contexts; explicit `@EntityScan` on the persistence configuration class is required.

### Including `server/contracts` when compiling service tests

`arbitrier-contracts` is a snapshot dependency. Always include `server/contracts` in the `-pl` list when building or testing a service module, otherwise Maven cannot resolve the artifact:

```bash
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service
```

---

## Platform Utilities

The `platform` module provides cross-cutting utilities that all services must use:

| Utility | Location | Purpose |
|---------|----------|---------|
| `Require` | `platform.validation` | Precondition checks in value-object/record constructors — throws `IllegalArgumentException` / `NullPointerException`, not business failures |
| `Result<T>` | `platform.result` | Expected business failures — `Result.success(value)` / `Result.failure(code, message)`. Use `valueOrThrow()` in adapters to convert to `ApplicationProblemException` |
| `TimeProvider` / `FixedTimeProvider` | `platform.time` | Never call `Instant.now()` directly; inject `TimeProvider` and bind `SystemClock.INSTANCE` in production config |
| `IdempotencyStore` | `platform.idempotency` | Port contract for event deduplication — no production implementation yet |
| `SafeLoggable` | `platform.logging` | Marker interface for objects safe to render in structured log fields |
| `OutboxRepository` / `InboxRepository` | `platform.messaging` | Outbox/inbox ports — write domain events to outbox inside the same transaction as the aggregate save; `InMemoryOutboxRepository` / `InMemoryInboxRepository` are available for tests |
| `EventSerializer` / `JacksonEventSerializer` | `platform.messaging` | Serialize domain events for the outbox record |
| `DomainEventToOutboxMapper` | `platform.messaging` | Converts a domain event + aggregateId + aggregateType to an `OutboxEvent` with correlation/causation IDs |
| `MessageNature` | `platform.messaging.outbox` | Enum — `EVENT` (record of what happened) or `COMMAND` (directed instruction). Required field on every `OutboxEvent`; chosen over "type" (collides with `eventType`) and "kind" (too informal) |
| `OutboundRoutingStrategy` | `platform.messaging.outbox` | Port interface — resolves `OutboxEvent` → Kafka topic name. The returned string is used directly as the topic by `KafkaOutboundMessagePublisher` |
| `OutboundPayloadSerializer` | `platform.messaging.outbox` | Port interface — serializes `OutboxEvent` → transport payload string. Transport-neutral; default impl is `JsonOutboundPayloadSerializer` (passthrough). Override with a custom bean to swap formats (e.g. Avro) |
| `JsonOutboundPayloadSerializer` | `platform.messaging.serialization` | Default `OutboundPayloadSerializer` — returns `OutboxEvent.payload()` unchanged. Auto-configured; Avro impl is a future task |
| `CorrelationId`, `CausationId`, `MessageId`, `RequestId` | `platform.correlation` | Distinct value objects for message tracing — do not conflate with `traceparent` |

---

## Test Layers

Three distinct test layers exist in every service, and they must not be conflated:

### 1. Unit tests (`src/test/.../domain/` and `.../application/service/`)

No Spring context. Test domain rules and use-case services directly. Instantiate in-memory adapters manually. Fast.

### 2. Application integration tests (`*ApplicationIT.java`)

`@SpringBootTest` with JPA autoconfiguration excluded, Kafka absent. All outbound ports are replaced by in-memory adapters declared in `*TestConfiguration`. Verify the full Spring wiring (DI, security, controller routing) without a database.

```java
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
    "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
    "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@Import(InventoryServiceTestConfiguration.class)
class InventoryServiceApplicationIT { ... }
```

### 3. JPA adapter tests (`Jpa*RepositoryAdapterTest`)

`@SpringBootTest` + `@Testcontainers` with `@ServiceConnection` on a `PostgreSQLContainer`. Test the persistence adapter in isolation against a real schema. Competing beans from `*TestConfiguration` are marked `@Primary` to win over main-config beans without triggering `BeanDefinitionOverrideException`.

### Test `JwtDecoder` override

`*TestConfiguration` provides a mock `JwtDecoder` that accepts any token without cryptographic verification, creating a JWT with `sub=test-user`. Mark it `@Primary` to shadow the resource-server auto-configured decoder.

### ArchUnit enforcement

Every service has `src/test/java/.../unit/ArchitectureTest.java`. It enforces hexagonal boundaries at compile time: domain must not import Spring/JPA/Avro/Kafka; application must not import Avro/Kafka/Spring Data; `@Entity` classes must reside only in `..adapter.outbound.persistence..`. Always run `mvn test` after adding a new entity or dependency to catch violations early.

---

## JPA Persistence Adapter Pattern

Every aggregate that requires persistence needs four artifacts in `adapter/outbound/persistence/`:

| Class | Role |
|-------|------|
| `*Entity` | JPA `@Entity` — flat DB representation, no domain logic |
| `*PersistenceMapper` | Translates domain aggregate ↔ JPA entity |
| `Jpa*RepositoryAdapter` | Implements the outbound port using `SpringData*Repository` |
| `SpringData*Repository` | `JpaRepository<*Entity, UUID>` — Spring Data interface |

ArchUnit enforces that `@Entity` classes stay in `..adapter.outbound.persistence..` — run `mvn test` after adding any new entity to confirm.

### Persistence mapper — three-method contract

Every `*PersistenceMapper` must implement:

| Method | When used |
|--------|-----------|
| `toEntity(Aggregate)` | Insert — creates a new entity |
| `updateEntity(Entity, Aggregate)` | Update — mutates the managed entity, preserves `version` field for optimistic locking |
| `toDomain(Entity)` | Reconstruction — rebuilds the aggregate, validating via domain constructors |

Preserve `entity.getVersion()` during `updateEntity`; never overwrite it. Use `list.clear()` + re-add for replacing child collections atomically.

### Persistence configuration — `@ConditionalOnMissingBean`

`*PersistenceConfiguration` classes gate every repository bean with `@ConditionalOnMissingBean`. This lets `*ApplicationIT` tests supply in-memory repositories from `*TestConfiguration` without triggering `BeanDefinitionOverrideException` and without needing a real database.

---

## Application Service Design

Application services should read as business stories. The preferred step order:

1. validate (handled in command constructors via `Require`)
2. derive / allocate
3. execute domain factory / aggregate method
4. persist
5. publish event
6. return result

Each step should be delegated to a well-named private method so the main use-case method fits on one screen. The outcome is always derived from the created/updated aggregate's `.status()` — never tracked in a parallel variable.

**Avoid:**
- Business algorithms inside the main method
- Duplicated state (e.g., tracking both `StockReservation` and `StockReservationStatus` in the same scope when one derives from the other)
- Long if/else trees in the main method body
- Mixing persistence decisions with domain decisions

**Transactionality:** Application services are `@Transactional`. DB + Kafka consistency uses the Outbox pattern (ADR-0005): within the same transaction, persist the aggregate and then call `OutboxRepository.save(DomainEventToOutboxMapper.map(event, aggregateId, aggregateType))`. Never publish directly to Kafka from a service method.
