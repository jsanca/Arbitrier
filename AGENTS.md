# AGENTS.md

## Build & Test

**Java 25** required. No lint/typecheck/formatter for server — `mvn -B verify` is the only quality gate.

`mvn test` runs both `*Test.java` and `*IT.java` (Surefire is configured to include `*IT.java` — no Failsafe plugin).

```bash
# All server modules
mvn -B verify --no-transfer-progress

# Fast feedback — all active modules (skips slow Testcontainers ITs)
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service,server/inventory-service,server/credit-service,server/orchestrator-service -Dtest='!*IT'

# Single module — always include server/contracts,server/platform before any service
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service -Dtest=ArchitectureTest
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service -Dtest=SubmitCorporateBulkOrderServiceTest#missing_customer_id_throws

# Client (React 19 + TypeScript mock-based prototype — no backend required)
cd client && npm ci && npm run build && npm run lint && npm test

# Client dev server (does not require backend or infra)
cd client && npm run dev   # login: brio@arbitrier.com / any password

# Client typecheck only (build = tsc -b + vite build)
cd client && npx tsc -b

# Client linter is oxlint (not eslint)
cd client && npm run lint

# Client tests are vitest run (single-run, not watch)
cd client && npm test

# Regenerate Avro types after schema changes (never commit generated sources)
mvn -B generate-sources --no-transfer-progress -pl server/contracts

# Infrastructure (Kafka, Postgres, Keycloak, Schema Registry)
infra/docker/start.sh             # auto-loads .env or .env.example; docker compose up -d --wait
infra/docker/health.sh
infra/docker/stop.sh              # docker compose down
infra/docker/stop.sh -v           # docker compose down -v (wipe volumes)
```

**Local stack ports**: PostgreSQL 5432, Kafka 9092, Schema Registry 8081, Keycloak 8180, Kafka UI 8088.

**`infra/docker/reset.sh`** wipes volumes and restarts (append `--seed` to load seed data).

**`-pl` requirement**: `server/contracts` and `server/platform` are SNAPSHOT dependencies — they **must** appear before any service in `-pl`. Without them, Maven fails to resolve the artifact.

## Module Build Order

```
platform (library) → contracts (Avro codegen) → [order-service | inventory-service | credit-service | orchestrator-service]
```

`platform` must never import business-domain types (`com.arbitrier.order.*`, `.inventory.*`, `.credit.*`, `.orchestrator.*`).

## Module Maturity (at a glance)

| Module | Domain | App Services | Adapters | Tests |
|--------|--------|-------------|----------|-------|
| order-service | Full | Full | REST + gRPC client + JPA (Kafka outbound = placeholder packages only) | Full |
| inventory-service | Full | Full | JPA persistence | Domain + app |
| credit-service | Full | Full | JPA persistence | Domain + app |
| orchestrator-service | Full | Full | JPA persistence | Domain + app |
| platform | N/A (library) | N/A | Full web infra | Full |
| contracts | 26 Avro schemas | N/A | N/A | Schema load |
| client | React 19 prototype (mock-based, no backend) | - | - | Vitest + jsdom (globals: true: no imports for describe/it/expect) |

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
- **Outbound serializer** is `JsonOutboundPayloadSerializer` (JSON passthrough, no Avro yet). Production Avro/Confluent serializer is deferred.
- **JPA is active** in all four services — `@Entity` classes exist with PostgreSQL schemas. When adding entities, register `RuntimeHintsRegistrar` for GraalVM native image.
- **JWT auth only in order-service**. `submittedByUserId` derived from JWT `authentication.getName()`, never from request body.
- **Spring Boot 4.1**: no `@WebMvcTest`, no `@DataJpaTest`, no bean override, no auto-configured `KafkaTemplate`. `@EntityScan` import moved to `org.springframework.boot.persistence.autoconfigure.EntityScan`.
- **REJECTED release is a no-op** — releasing a rejected credit/stock reservation does nothing.
- **Virtual threads** enabled in all services (`spring.threads.virtual.enabled: true`).
- **W3C Trace Context** only — never B3 headers (`X-B3-TraceId`). `X-Correlation-Id` is business-level.
- **Native Image** (ADR-0007): avoid `Class.forName`, dynamic proxies, runtime classpath scanning. Register `RuntimeHintsRegistrar` for `@Entity` or Avro types.
- **Resilience4j** — no Hystrix or Spring Retry.
- **IdempotencyStore** is port-only (interface in platform, no adapter yet).
- **OutboxPublisher** is a placeholder interface (no Kafka drainer yet). Application services write to `OutboxRepository` directly.
- **InboxRepository** is wired as a bean in all services but never injected — pre-built for future idempotent consumers.
- **Entity scanning uses both** `@EntityScan(basePackageClasses = ...)` on `*PersistenceConfiguration` and `spring.jpa.packages` in `application.yml` — these duplicate each other. Only `@EntityScan` is needed for test contexts with inner `@SpringBootConfiguration`. Do not add a third mechanism.
- **JPA adapter classes must not be `final`** — Spring Boot uses CGLIB proxying for `@Transactional`, which cannot subclass a final class.
- **`@Transactional` on every JPA adapter mutation method** — calls from background threads arrive without a transaction context; `@Modifying` queries throw `TransactionRequiredException` without it.
- **JPA adapters must be Spring-managed beans** in tests — a manually `new`'d adapter has no AOP proxy, so `@Transactional` has no effect.
- **`@ConditionalOnMissingBean`** gates every persistence repository bean in `*PersistenceConfiguration`. This lets `*ApplicationIT` tests supply in-memory adapters without triggering `BeanDefinitionOverrideException`.
- **Flyway is the sole schema owner** — `spring.jpa.hibernate.ddl-auto=validate`, `flyway.clean-disabled=true`. Each service owns migrations under `db/migration/<service>/`; platform owns shared tables in `db/migration/platform/`. `flyway.locations` lists both paths.

## Naming Conventions

| Artifact | Pattern |
|----------|---------|
| Input port | `<Action><Subject>UseCase` (interface) |
| Output port | `<Subject>Repository` / `<Subject>Gateway` |
| Application service | `<Action><Subject>Service` |
| REST adapter | `<Subject>RestAdapter` |
| JPA adapter | `<Subject>JpaAdapter` |
| Kafka consumer adapter | `<Subject>KafkaConsumerAdapter` |
| Kafka producer adapter | `<Subject>KafkaProducerAdapter` |
| Domain event | `<Subject><PastTense>DomainEvent` |
| Avro schema file | `<subject>-<past-tense>-event-v<N>.avsc` |
| Kafka topic | `arbitrier.<domain>.<event>.v<N>` |

## UC-01 Review Context

Availability negotiation and the buyer’s partial-quantity decision happen before an Order or Saga exists. Inventory owns warehouse allocation. Review active behavior against [RF-UC-01](docs/rf/RF-UC-01-corporate-bulk-order.md) and [ADR-0009](docs/adr/ADR-0009—GlobalInventoryAllocationOwnership.md), not historical task narratives.

## Execution Policy

All non-trivial tasks: **target 20–30 min · warning 30 min · hard stop 45 min → recovery checkpoint**. Split oversized work before starting.

- Timebox: `docs/engineering/agent-execution-timebox.md` + `.opencode/skills/process/osk-execution-timebox/SKILL.md`
- Reporting: `.opencode/skills/process/osk-engineering-reporting/SKILL.md` — tasks `docs/agents/tasks/`, reports `docs/agents/reports/`, reviews `docs/agents/reviews/`, checkpoints `docs/agents/checkpoints/`. Read any OPEN checkpoint before continuing. Note: `docs/agents/` holds the historical/active record; the OSK model (`docs/OSK.md`) routes new engineering records to `docs/engineering/agents/` — both exist, do not merge or migrate them silently.
- Ownership: `docs/engineering/documentation-ownership.md` (AGENTS.md = review/auxiliary; CLAUDE.md = implementation)

## Key Docs

- **CLAUDE.md** — detailed style guide, Spring Boot 4.1 patterns, application service design, testing expectations.
- **docs/adr/** — Architecture Decision Records (trace context, outbox, Avro, schema-per-service, GraalVM).
- **docs/rnf/** — Non-functional requirements (technical baseline, native image runtime).
- **`docs/test-cases/`** — UC-01 test case specs (TC-UC-01-001 through TC-UC-01-012).
- **`docs/okf/`, `docs/rf/`** — Use-case narratives and functional requirements.
- **`.claude/skills/`** — Java, Spring Boot, testing, and platform skill files.

<!-- OSK:BEGIN -->

## OSK Workspace

Read:

- `docs/PROJECT.md`
- `docs/OSK.md`

<!-- OSK:END -->

<!-- OSK:SKILLS:BEGIN -->

## OSK Installed Skills

- [diagram-design](.osk/skills/diagram-design/SKILL.md)
- [Code Documentation Publication](.osk/skills/osk-code-docs/SKILL.md)
- [Knowledge Curator](.osk/skills/osk-knowledge-curator/SKILL.md)
<!-- OSK:SKILLS:END -->
