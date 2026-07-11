# AGENTS.md

## Build & Test

**Java 25** required. No lint/typecheck/formatter for server — `mvn -B verify` is the only quality gate.

```bash
# All server modules
mvn -B verify --no-transfer-progress

# Fast feedback — all active modules
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service,server/inventory-service,server/credit-service,server/orchestrator-service

# Single module — always include server/contracts,server/platform before any service
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service -Dtest=ArchitectureTest
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service -Dtest=SubmitCorporateBulkOrderServiceTest#missing_customer_id_throws

# Client (React 19 + TypeScript mock-based prototype — no backend required)
cd client && npm ci && npm run build && npm run lint && npm test

# Regenerate Avro types after schema changes (never commit generated sources)
mvn -B generate-sources --no-transfer-progress -pl server/contracts

# Infrastructure (Kafka, Postgres, Keycloak, Schema Registry)
infra/docker/start.sh             # preferred — auto-loads .env or .env.example
infra/docker/health.sh
infra/docker/down -v              # wipe volumes
```

**`-pl` requirement**: `server/contracts` and `server/platform` are SNAPSHOT dependencies — they **must** appear before any service in `-pl`. Without them, Maven fails to resolve the artifact.

## Module Build Order

```
platform (library) → contracts (Avro codegen) → [order-service | inventory-service | credit-service | orchestrator-service]
```

`platform` must never import business-domain types (`com.arbitrier.order.*`, `.inventory.*`, `.credit.*`, `.orchestrator.*`).

## Module Maturity (at a glance)

| Module | Domain | App Services | Adapters | Tests |
|--------|--------|-------------|----------|-------|
| order-service | Full | Full | REST + Kafka outbound + JPA | Full |
| inventory-service | Full | Full | JPA persistence | Domain + app |
| credit-service | Full | Full | JPA persistence | Domain + app |
| orchestrator-service | Full | Full | JPA persistence | Domain + app |
| platform | N/A (library) | N/A | Full web infra | Full |
| contracts | 27 Avro schemas | N/A | N/A | Schema load |
| client | React 19 prototype (mock-based, no backend) | - | - | Vitest + jsdom |

## Test Patterns

- **Domain tests**: pure JUnit 5 + AssertJ, no Spring context.
- **Application service tests**: hand-wired (no Spring), using `InMemory*Repository`, `Recording*EventPublisher`, `Configurable*Port` test adapters.
- **Controller tests**: `@SpringBootTest(webEnvironment = MOCK)` + `@Import(*ServiceTestConfiguration.class)` + `@MockitoBean` + manually built `MockMvc`.
- **`@MockitoBean`** import: `org.springframework.test.context.bean.override.mockito.MockitoBean` (Spring Boot 4.1 API, replaces `@MockBean`).
- **No bean override** in Boot 4.x — mark test beans `@Primary` instead.
- **ArchitectureTest** (ArchUnit 1.3.0) enforces hexagonal layering + package-info.java in every service.

## Critical Constraints

- **Domain is pure Java** — zero Spring/JPA/Kafka imports. Models are immutable (new instance per transition, no setters).
- **Application services** follow: validate (in command constructors via `Require`) → derive → execute domain → persist → publish → return. Outcome always from aggregate `.status()`.
- **Kafka activation** gated by `@ConditionalOnProperty("spring.kafka.bootstrap-servers")`. Tests skip Kafka silently via test adapters.
- **Avro serializer** is `ByteArraySerializer` (placeholder). Choose Confluent `KafkaAvroSerializer` before production.
- **JPA is active** in all four services — `@Entity` classes exist with PostgreSQL schemas. When adding entities, register `RuntimeHintsRegistrar` for GraalVM native image.
- **JWT auth only in order-service**. `submittedByUserId` derived from JWT `authentication.getName()`, never from request body.
- **Spring Boot 4.1**: no `@WebMvcTest`, no bean override, no auto-configured `KafkaTemplate`.
- **REJECTED release is a no-op** — releasing a rejected credit/stock reservation does nothing.
- **Virtual threads** enabled in all services (`spring.threads.virtual.enabled: true`).
- **W3C Trace Context** only — never B3 headers (`X-B3-TraceId`). `X-Correlation-Id` is business-level.
- **Native Image** (ADR-0007): avoid `Class.forName`, dynamic proxies, runtime classpath scanning. Register `RuntimeHintsRegistrar` for `@Entity` or Avro types.
- **Resilience4j** — no Hystrix or Spring Retry.
- **IdempotencyStore** is port-only (interface in platform, no adapter yet).

## Naming Conventions

| Artifact | Pattern |
|----------|---------|
| Input port | `<Action><Subject>UseCase` (interface) |
| Output port | `<Subject>Repository` / `<Subject>Gateway` |
| Application service | `<Action><Subject>Service` |
| REST adapter | `<Subject>RestAdapter` |
| JPA adapter | `<Subject>JpaAdapter` |
| Kafka producer adapter | `<Subject>KafkaProducerAdapter` |
| Domain event | `<Subject><PastTense>DomainEvent` |
| Avro schema file | `<subject>-<past-tense>-event-v<N>.avsc` |
| Kafka topic | `arbitrier.<domain>.<event>.v<N>` |

## UC-01 Saga States (for context)

```
STARTED → CREDIT_RESERVATION_REQUESTED
  → CREDIT_RESERVED → INVENTORY_RESERVATION_REQUESTED
      → INVENTORY_FULLY_RESERVED                      → CONFIRMED
      → INVENTORY_PARTIALLY_RESERVED → AWAITING_CUSTOMER_DECISION
          → (accept) → PARTIALLY_CONFIRMED
          → (reject) → CANCELLED [compensations]
      → INVENTORY_RESERVATION_FAILED → CANCELLED
  → CREDIT_RESERVATION_DENIED → CANCELLED
```

## References

- **CLAUDE.md** — detailed style guide, Spring Boot 4.1 patterns, application service design, testing expectations.
- **docs/adr/** — Architecture Decision Records (trace context, outbox, Avro, schema-per-service, GraalVM).
- **docs/rnf/** — Non-functional requirements (technical baseline, native image runtime).
- **docs/test-cases/** — Test case specs for UC-01 (TC-UC-01-001 through TC-UC-01-012).
- **docs/okf/**, **docs/rf/** — Use-case narratives and functional requirements.
- **`~/.claude/skills/`** — OpenCode skill files for Java, Spring Boot, testing, and platform patterns.
