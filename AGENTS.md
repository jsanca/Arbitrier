# AGENTS.md

## Project Status

**ARB-004B** — Domain logic scaffolded, Avro contracts active, platform library complete. No JPA entities, no Kafka consumers/producers, no REST controllers, no client UI yet.

## Build & Test

```bash
mvn -B verify --no-transfer-progress                                        # all server modules
mvn -B verify --no-transfer-progress -pl server/order-service               # single module
mvn -B test -pl server/order-service -Dtest=DomainTest                      # single class
mvn -B test -pl server/order-service -Dtest=DomainTest#testMethod           # single method
mvn -B generate-sources --no-transfer-progress -pl server/contracts         # regenerate Avro types

docker compose -f infra/docker/docker-compose.yml up -d                     # Kafka, Postgres, Keycloak, Schema Registry
docker compose -f infra/docker/docker-compose.yml down -v
```

CI runs `mvn -B verify --no-transfer-progress` on push/PR to `main`.

## Module Build Order (Maven, enforced by `server/pom.xml`)

```
platform (library) → contracts (library) → order-service | inventory-service | credit-service | orchestrator-service
```

Each service depends on `platform` + `contracts`. `platform` must not import business-domain types (`com.arbitrier.order.*`, `.inventory.*`, `.credit.*`, `.orchestrator.*`).

## Current State (what is real vs what is still scaffold)

| Artifact | Status |
|----------|--------|
| Domain models (Order, StockReservation, CreditReservation) | **Active** — state machines with guards, factory methods |
| Domain tests (OrderTest, StockReservationTest, CreditReservationTest, SagaTest) | **Active** — JUnit 5 + AssertJ |
| ArchUnit tests per service | **Active** — domain→adapter, application→adapter, domain→Spring/JPA rules |
| Platform library (Require, Result, Idempotency, Correlation, etc.) | **Active** — 50+ classes, fully tested |
| Avro codegen in contracts/pom.xml | **Active** — 26 `.avsc` schemas, generates on `compile` |
| ContractsSchemaTest | **Active** — 30+ test methods validating all 26 schemas |
| `package-info.java` | **70 files** — every package has one |
| Context-load integration tests (4 services) | **Active** — `@SpringBootTest`, no Testcontainers yet |
| JPA + PostgreSQL dependencies (all service POMs) | **Commented out** — uncomment when adding first `@Entity` |
| Kafka + `spring-kafka` dependencies (all service POMs) | **Commented out** — uncomment when adding first Avro producer/consumer |
| REST controllers | **Not present** — adapter directories exist as package-info stubs |
| Client (`client/`) | **README only** — no scaffold |
| E2E tests | **Placeholder** — CI echoes a TODO |

## Key Pitfalls

- **Domain model discovery**: `Order.java` uses **immutable objects** (new instance per transition). Do not add setters or mutate state.
- **Avro imports**: All `.avsc` schemas listed in `contracts/pom.xml` `<imports>` block must be registered when adding a new schema that references common types.
- **No `@Entity` yet**: If you add one, also add JPA deps to `pom.xml` and register a `RuntimeHintsRegistrar` for GraalVM.
- **Resilience4j** is the chosen resilience library — don't add Hystrix or Spring Retry.
- **OpenCode config**: No repo-local `opencode.json`. Configure via env or `~/.config/opencode/`. 16 skill files live in `.opencode/skills/`.
- **CLAUDE.md** exists at repo root and mirrors much of this info for Claude Code users. Keep both in sync.

## Observability conventions (ARB-009 / ADR-0008)

- **W3C Trace Context only** — `traceparent` / `tracestate` are the distributed tracing headers. Never add `X-B3-TraceId`, `X-B3-SpanId`, or `X-B3-Sampled` as a default platform header.
- **`CorrelationFilter` scope** — reads/writes `X-Correlation-Id` and `X-Request-Id` only. It must not touch `traceparent` or `tracestate`.
- **Header taxonomy**: `traceparent`/`tracestate` = OTel technical tracing · `X-Correlation-Id` = business operation identity · `X-Request-Id` = single HTTP request.
- **MDC keys** — `correlationId` and `requestId` are populated by `CorrelationFilter` now. `traceId` and `spanId` will be auto-injected by the OTel MDC bridge when added. Never manually populate `traceId` from request headers.

## Native Image (cross-cutting — ADR-0007)

GraalVM Native Image is a supported runtime. Before adding reflection, proxies, classpath scanning, or resource loading, register hints in a `RuntimeHintsRegistrar`. Use OTel SDK mode (no agent), OTLP HTTP exporter (not gRPC). Document unresolvable incompatibilities as `OPEN QUESTION`.

See `docs/adr/ADR-0007-spring-aot-graalvm-native-image.md` · `docs/rnf/RNF-0002-native-image-runtime.md`.
