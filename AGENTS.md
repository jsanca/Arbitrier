# AGENTS.md

## Project Status

**ARB-003 — Architecture skeleton.** No business logic, no JPA entities, no Kafka consumers/producers. All service `pom.xml` files have JPA and Kafka starters **commented out** (activate when adding the first entity/`.avsc`). Avro codegen plugin is commented out in `contracts/pom.xml`.

## First Reads

- `docs/okf/index.md` — top-level navigation into all docs
- `README.md` — repo layout, tech stack, hexagonal structure
- `server/README.md` — module relationships, build order

## Build & Test

```bash
# All server modules (from repo root)
mvn -B verify --no-transfer-progress

# Single module
mvn -B verify --no-transfer-progress -pl server/order-service

# Single test class or method
mvn -B test -pl server/order-service -Dtest=ArchitectureTest
mvn -B test -pl server/order-service -Dtest=ArchitectureTest#domain_must_not_depend_on_adapter

# Client (once scaffolded — currently only README.md)
cd client && npm ci && npm run build && npm test

# Local infra (Kafka, PostgreSQL, Keycloak, Schema Registry)
docker compose -f infra/docker/docker-compose.yml up -d
docker compose -f infra/docker/docker-compose.yml down -v

# E2E (Playwright, against running stack)
cd client && npx playwright test
```

## Module Build Order

```
root pom → server (aggregator)
  → platform (library jar, no Spring Boot plugin)
  → contracts (library jar, Avro codegen commented out)
  → order-service | inventory-service | credit-service | orchestrator-service
```

Each service depends on `platform` + `contracts`. Maven respects the declared module order.

## Hexagonal Architecture — Every Server Service

```
com.arbitrier.<service>/
  adapter/inbound/rest/       Spring MVC controllers
  adapter/inbound/kafka/      Kafka consumers
  adapter/outbound/persistence/  JPA repositories
  adapter/outbound/kafka/     Kafka producers
  application/port/inbound/   Use-case interfaces
  application/port/outbound/  Repository/messaging interfaces
  application/service/        Use-case implementations
  domain/model/               Entities, VOs — zero Spring/JPA imports
  domain/event/               Domain events
  domain/command/             Commands
  domain/exception/           Domain exceptions
  config/                     Spring @Configuration
  observability/              MDC helpers, OpenTelemetry spans
```

**Dependency rule:** `adapter → application → domain`. Domain must have zero Spring/JPA/Kafka imports. Verified by ArchUnit tests (currently commented no-ops in `unit/ArchitectureTest.java` per service).

## `package-info.java` — Required in Every Package

```java
/**
 * <One-sentence purpose.>
 *
 * <p>Layer: domain/model | application/port/inbound | adapter/inbound/rest | ...
 * <p>Module: <service-name>
 */
package com.arbitrier.<service>.<layer>;
```

56 exist today across all modules. The ArchUnit test `PlatformArchitectureTest` enforces that `platform` has zero business-domain references.

## Testing Conventions

- **Unit:** JUnit 5 + Mockito in `src/test/java/.../unit/`
- **Integration:** `@SpringBootTest` in `src/test/java/.../integration/` (currently only context-load tests)
- **Contract:** `ContractsSchemaTest` placeholder for Avro compatibility
- **E2E:** Playwright in `client/e2e/` (once scaffolded)
- **Test docs:** `docs/test-cases/TC-UC-01-001-*.md` through `-012` map to UC-01 flows
- ArchUnit tests exist per service but rules are commented no-ops — activate when domain classes are added

## Naming Conventions

| Artifact | Pattern |
|----------|---------|
| Input port | `<Action><Subject>UseCase` (interface) |
| Output port | `<Subject>Repository`, `<Subject>Gateway` |
| Service impl | `<Action><Subject>Service` |
| REST adapter | `<Subject>RestAdapter` |
| JPA adapter | `<Subject>JpaAdapter` |
| Kafka consumer | `<Subject>KafkaConsumerAdapter` |
| Kafka producer | `<Subject>KafkaProducerAdapter` |
| Domain event | `<Subject><PastTense>Event` |
| Avro schema file | `<subject>-<past-tense>-event-v<N>.avsc` |
| Kafka topic | `<domain>.<event>.v<N>` (e.g. `order.placed.v1`) |

## UC-01 Saga States

```
STARTED → reservation requested → fully reserved → CONFIRMED
                                → partially reserved → AWAITING_CUSTOMER_DECISION
                                    → accept → PARTIALLY_CONFIRMED
                                    → reject/wait/cancel → CANCELLED
                                    → system failure → CANCELLED
```

Only three final states: `CONFIRMED`, `PARTIALLY_CONFIRMED`, `CANCELLED`. The waiting state is `AWAITING_CUSTOMER_DECISION`. Documented in `docs/okf/seeds/UC-01-corporate-bulk-order.md`.

## Documentation-First Rules

- Read OKF, RF, RNF, ADR, and test-case docs **before** writing code.
- If a rule, transition, SLA, topic name, role, or schema detail is absent, write `OPEN QUESTION`.
- Do not silently resolve open questions in code.
- Do not bypass Kafka/Avro contract decisions without an ADR.
- Do not introduce hidden business logic in adapters.
- `platform` must have no business-domain types (no order/inventory/credit/credit imports).
- Update `docs/okf/index.md` when adding a new major document.

## Logging & Observability

- Every saga log line must carry `sagaId`, `orderId`, `traceId`.
- Logs at: controller, application service, repository, Kafka adapter layers.
- OpenTelemetry traces + metrics for important transitions.
