# ARB-004 — Platform Foundation Implementation Note

| Field  | Value                   |
|--------|-------------------------|
| Task   | ARB-004                 |
| Status | Implemented             |
| Date   | 2026-07-07              |

## New Packages

| Package | Classes Created |
|---------|----------------|
| `correlation` | `CorrelationId`, `CausationId`, `MessageId`, `RequestId` |
| `time` | `TimeProvider`, `SystemClock`, `FixedTimeProvider` |
| `result` | `Result<T>` (sealed: `Success`, `Failure`) |
| `error` | `ProblemCode`, `PlatformProblemCode`, `ApplicationProblem` |

## Filled Placeholder Packages

| Package | Classes Created |
|---------|----------------|
| `idempotency` | `IdempotencyKey`, `IdempotencyStatus`, `IdempotencyRecord`, `IdempotencyStore` |
| `logging` | `SafeLoggable`, `SafeRenderable`, `StructuredLogFields` |
| `observability` | `ObservationNames`, `AttributeNames` |
| `validation` | `Require` |
| `test` | `TestIds`, `FixedClock`, `PlatformAssertions` |

## Unit Tests

| Test Class | Scenarios |
|-----------|-----------|
| `CorrelationIdTest` | generate, of, null/blank rejection, equality, toString, other ID types |
| `TimeProviderTest` | SystemClock non-null, FixedTimeProvider pinning, today(), null rejection |
| `ResultTest` | success, failure, valueOrThrow, map, null rejections |
| `ApplicationProblemTest` | code/message/cause, null code rejection, PlatformProblemCode values |
| `IdempotencyTest` | key generate/of/null/blank, pending record, markProcessed/Failed, immutability |
| `SafeLoggableTest` | field constants non-blank, interface contracts |
| `RequireTest` | notNull, notBlank, notEmpty, isTrue — all pass/fail paths |
| `TestSupportTest` | TestIds, FixedClock, PlatformAssertions pass/fail |

All tests run with no Docker, Kafka, PostgreSQL, or Keycloak.

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| `Require` throws `IllegalArgumentException` / `NullPointerException` | Standard Java convention; `ApplicationProblem` is for business failures, not programming errors |
| `Result.Success` requires non-null value | Mirrors `Optional`'s non-null contract; callers use `Result<Void>` via `Result.success(Void/Boolean)` when no value is needed |
| `test/` in `src/main/java` | Allows service modules to declare `platform` as a test-scoped dependency and use `TestIds`, `FixedClock`, `PlatformAssertions` in their own tests |
| `IdempotencyStore` is an interface only | No JPA implementation yet — follows hexagonal port-first approach |
| No Spring annotations on `SystemClock` | Services declare the bean in their `config/` package, keeping platform free of Spring wiring |

## What Is Intentionally Not Implemented

| Area | Deferred to |
|------|-------------|
| `IdempotencyStore` JPA adapter | Persistence phase (ARB-00X) |
| Keycloak security filter chain | Security integration phase |
| Kafka consumer/producer configuration | Avro contracts phase (ARB-00X) |
| OpenTelemetry SDK wiring | Observability wiring phase |
| Outbox / inbox tables | Outbox pattern task (ADR-0005) |

## Deep Review Fixes

| Fix | Change Made | Rationale |
|-----|-------------|-----------|
| Removed `spring-boot-starter-web` from platform POM | Deleted the optional `spring-boot-starter-web` dependency | Platform is a library JAR; no REST adapter classes exist here — pulling in Tomcat/Jackson as an optional dependency was misleading and unnecessary |
| Activated ArchUnit isolation rule in `PlatformArchitectureTest` | Uncommented and wired `noClasses()…check(classes)` | Platform now has real production classes; the rule trivially passes (service packages are not on the platform test classpath) but makes the contract explicit and enforced on every build |

## Open Questions

- OPEN QUESTION: Should `Result` support `flatMap` for chaining use-case calls? Deferred — evaluate when first chained use cases are implemented.
- OPEN QUESTION: `StructuredLogFields` constants assume Logback MDC keys — verify alignment with chosen telemetry pipeline when OpenTelemetry is wired.
