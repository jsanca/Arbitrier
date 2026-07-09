# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Operating Instructions

Work documentation-first. Read the relevant OKF, RF, RNF, ADR, and test-case files before coding. Do not invent business behavior; mark missing details as `OPEN QUESTION`. Keep changes scoped to the requested slice.

---

## Build Commands

```bash
# Server — all modules
mvn -B verify --no-transfer-progress

# Server — modules with active implementation (fastest feedback loop)
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service,server/inventory-service

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

Dependent modules must always be included when running a service test. `server/contracts` and `server/platform` must appear before any service in the `-pl` list.

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

## UC-01 Saga States

```
STARTED → CREDIT_RESERVATION_REQUESTED
  → CREDIT_RESERVED → INVENTORY_RESERVATION_REQUESTED
      → INVENTORY_FULLY_RESERVED                              → CONFIRMED
      → INVENTORY_PARTIALLY_RESERVED → AWAITING_CUSTOMER_DECISION
            → (customer accepts)                              → PARTIALLY_CONFIRMED
            → (customer rejects) [compensations run]         → CANCELLED
      → INVENTORY_RESERVATION_FAILED [compensate credit]     → CANCELLED
  → CREDIT_RESERVATION_DENIED                                 → CANCELLED
```

The waiting state is `AWAITING_CUSTOMER_DECISION` — not "human approver."

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

### Including `server/contracts` when compiling service tests

`arbitrier-contracts` is a snapshot dependency. Always include `server/contracts` in the `-pl` list when building or testing a service module, otherwise Maven cannot resolve the artifact:

```bash
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service
```

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

**Transactionality note:** Application services will become `@Transactional` when JPA persistence is introduced. DB + Kafka consistency will be handled by the Outbox pattern (ADR-0005) — events are written to an outbox table inside the DB transaction rather than published directly in the service method.
