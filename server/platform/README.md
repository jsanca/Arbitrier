# platform

Cross-cutting library jar shared by all server-side services.

## Rules

- **No business domain knowledge.** Platform must not reference orders, credit, inventory, or sagas by name.
- **No business persistence implementations.** Service-specific JPA entities and adapters remain in their owning service; platform may expose domain-neutral Spring integration and ports.
- **No circular dependencies.** Services depend on platform; platform depends on nothing within this repo.
- **Pure Java where possible.** Avoid Spring annotations unless the abstraction genuinely requires a Spring contract.

## Packages (ARB-004)

| Package | Contents |
|---------|---------|
| `correlation` | `CorrelationId`, `CausationId`, `MessageId`, `RequestId` — typed UUID wrappers for message lineage |
| `time` | `TimeProvider` interface, `SystemClock` singleton, `FixedTimeProvider` for tests |
| `result` | `Result<T>` sealed type — `Success` and `Failure` for expressing expected error paths |
| `error` | `ProblemCode` interface, `PlatformProblemCode` enum, `ApplicationProblem` exception |
| `idempotency` | `IdempotencyKey`, `IdempotencyStatus`, `IdempotencyRecord`, `IdempotencyStore` port interface |
| `logging` | `SafeLoggable`, `SafeRenderable` interfaces; `StructuredLogFields` MDC key constants |
| `observability` | `ObservationNames` and `AttributeNames` constants for spans and telemetry attributes |
| `validation` | `Require` precondition helpers (null, blank, empty, boolean) |
| `test` | `TestIds` factory, `FixedClock` wrapper, `PlatformAssertions` — in main sources for use by service tests |

## Spring Integration (ARB-008)

| Package | Contents |
|---------|---------|
| `web` | `CorrelationHeaders`, `TraceContextHeaders` — header name constants; `CorrelationFilter` (MDC population/cleanup); `ProblemResponse` record; `PlatformExceptionHandler` (`@RestControllerAdvice`) |
| `spring` | `PlatformAutoConfiguration` — `@AutoConfiguration` activated in servlet web applications; registers the correlation filter, exception handler, and a default `TimeProvider` bean |

The auto-configuration is registered via
`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
and is picked up automatically by any service module that depends on `spring-boot-starter-web`.

## Observability Conventions (ARB-009)

### Header taxonomy

| Header | Owner | Purpose |
|--------|-------|---------|
| `traceparent` | OpenTelemetry SDK | W3C Trace Context — technical distributed tracing (ADR-0008) |
| `tracestate` | OpenTelemetry SDK | W3C vendor-specific trace state; forwarded unchanged |
| `X-Correlation-Id` | `CorrelationFilter` | Business-level operation identifier; propagated or generated |
| `X-Request-Id` | `CorrelationFilter` | Per-HTTP-request identifier; always generated fresh |

**Rule**: `CorrelationFilter` must never read, generate, or echo `traceparent`/`tracestate`. B3 headers (`X-B3-TraceId` etc.) are not a platform convention (ADR-0008).

### MDC key population

| MDC key (from `StructuredLogFields`) | Populated by | Status |
|--------------------------------------|-------------|--------|
| `correlationId` | `CorrelationFilter` | Active |
| `requestId` | `CorrelationFilter` | Active |
| `traceId` | OpenTelemetry MDC bridge | Deferred — auto-injected when `micrometer-tracing-bridge-otel` is on classpath |
| `spanId` | OpenTelemetry MDC bridge | Deferred |
| `sagaId`, `orderId` | Application/adapter code per service | Added at saga-step boundaries |
| `messageId`, `causationId` | Kafka inbound adapters | Deferred to Kafka phase |

### Actuator endpoints

Services that depend on `spring-boot-starter-actuator` expose:

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Kubernetes liveness / readiness |
| `/actuator/info` | Service metadata |
| `/actuator/metrics` | Micrometer metrics (Prometheus scrape via `management.metrics.export.prometheus`) |

Full Prometheus export configuration is deferred to the infrastructure wiring phase.

## Security Primitives (ARB-010)

| Class | Purpose |
|-------|---------|
| `security/AuthenticatedUser` | Immutable principal record: `userId` (JWT `sub`) + `authorities`. No Spring Security dependency. |
| `security/AuthenticatedUserProvider` | Interface for resolving the current user from any security context. Implemented in adapter layer. |

`ProblemCode.httpStatus()` (added in ARB-010) allows each problem code to declare its own HTTP status. `PlatformExceptionHandler` is now generic — it reads `ex.code().httpStatus()` rather than hard-coding 422. This lets order-service return 403 for `CUSTOMER_ACCESS_DENIED` without adding service-specific handler logic to the platform.

## Kafka Header & Topic Conventions (ARB-011)

| Class | Purpose |
|-------|---------|
| `kafka/KafkaHeaders` | Standard Kafka message header name constants: `messageId`, `correlationId`, `causationId`, `traceparent`, `tracestate`, `schemaVersion`. |
| `kafka/TopicNames` | Canonical topic name constants. Active: `arbitrier.order.created.v1`. Placeholders for stock and credit topics. |

**Rules**:
- `traceparent`/`tracestate` are defined for reference only. Populate them via OTel Kafka instrumentation, not application code (ADR-0008).
- B3 headers must not be added to Kafka messages.

## Packages Reserved for Future Tasks

| Package | When |
|---------|------|
| `exception` | If a shared exception hierarchy beyond `ApplicationProblem` is needed |

## Internal Dependency Graph

```
error        ← (nothing)
validation   ← (nothing)
time         ← (nothing)
logging      ← (nothing)
observability← (nothing)
kafka        ← (nothing)
correlation  ← validation
idempotency  ← validation
result       ← error
test         ← correlation, idempotency, time, result, error
```

No cycles. The `test` package may depend on all others since it exists to support them.

## Build

```bash
# From repo root — compile and run platform unit tests
mvn -B test --no-transfer-progress -pl server/platform

# Single test class
mvn -B test --no-transfer-progress -pl server/platform -Dtest=ResultTest
```

## Status

`ARB-011` — Kafka header and topic name constants added (`KafkaHeaders`, `TopicNames`).
`ARB-010` — Security primitives added (`AuthenticatedUser`, `AuthenticatedUserProvider`). `ProblemCode.httpStatus()` allows codes to self-declare HTTP status.
