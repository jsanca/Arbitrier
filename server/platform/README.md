# platform

Cross-cutting library jar shared by all server-side services.

## Rules

- **No business domain knowledge.** Platform must not reference orders, credit, inventory, or sagas by name.
- **No infrastructure implementations.** No JPA, Kafka, Spring Security, or database code yet.
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

## Packages Reserved for Future Tasks

| Package | When |
|---------|------|
| `security` | Keycloak JWT integration phase |
| `kafka` | Avro contracts + Kafka wiring phase |
| `exception` | If a shared exception hierarchy beyond `ApplicationProblem` is needed |

## Internal Dependency Graph

```
error        ← (nothing)
validation   ← (nothing)
time         ← (nothing)
logging      ← (nothing)
observability← (nothing)
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

`ARB-004` — Platform foundation implemented. No infrastructure adapters. No business domain classes.
