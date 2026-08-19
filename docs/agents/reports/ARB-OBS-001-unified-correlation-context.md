# Report: ARB-OBS-001 — Unified Correlation Context

## Context

**Task ID:** ARB-OBS-001  
**Roadmap slice:** ARB-OBS — Observability  
**Owner:** Clio  
**Role:** Architecture / Infrastructure  
**Execution status:** DONE — completed within timebox  
**Scope:** `CorrelationContext`, `DefaultCorrelationContext`, `CorrelationContextHolder`, `MdcCorrelationContextBridge`; refactored `CorrelationFilter`  
**Out of scope:** ScopeValue migration, Kafka propagation, scheduler propagation, OpenTelemetry, tracing, Micrometer, CompletionStage propagation  
**Timebox result:** Completed within the 20–30 minute target window.

## Summary

Introduced a transport-independent `CorrelationContext` abstraction in `platform.correlation` and a `ThreadLocal`-backed `CorrelationContextHolder` that decouples business code and platform infrastructure from SLF4J MDC. `MdcCorrelationContextBridge` handles the MDC side-effect as an isolated, testable utility. `CorrelationFilter` was refactored to delegate context creation and MDC population through the new abstractions. Existing HTTP behavior is identical; all prior tests pass unchanged.

## Deliverables

| Artifact | Location |
|---|---|
| `CorrelationContext` interface | `platform.correlation` |
| `DefaultCorrelationContext` record | `platform.correlation` |
| `CorrelationContextHolder` | `platform.correlation` |
| `MdcCorrelationContextBridge` | `platform.correlation` |
| `CorrelationFilter` (refactored) | `platform.web` |
| `DefaultCorrelationContextTest` (6 tests) | `platform/test/.../correlation/` |
| `CorrelationContextHolderTest` (6 tests) | `platform/test/.../correlation/` |
| `MdcCorrelationContextBridgeTest` (6 tests) | `platform/test/.../correlation/` |
| Implementation report | `docs/agents/reports/ARB-OBS-001-unified-correlation-context.md` |
| Recovery checkpoint | `docs/agents/checkpoints/CHECKPOINT-ARB-OBS-001-unified-correlation-context.md` |

## Architectural Decisions

### 1. `CorrelationContext` as a plain interface, not an abstract class or sealed type

The interface declares two accessors: `correlationId()` (always non-null) and `requestId()` (nullable). Making `requestId()` nullable at the interface level is the correct expression of the domain: HTTP-initiated contexts have a request ID; scheduler, outbox dispatcher, and Kafka consumer contexts do not. A sealed type hierarchy would add unnecessary verbosity without behavioral benefit at this stage.

### 2. `CorrelationContextHolder` uses a plain `ThreadLocal`

`InheritableThreadLocal` was considered and rejected. It propagates context to child threads created by the parent, but not to `CompletionStage` callbacks or pool-reused threads — where the inherited context is stale and misleading. A plain `ThreadLocal` makes the propagation boundary explicit: whoever sets the context is responsible for propagating it explicitly to other threads. Cross-thread propagation is the subject of future slices (ARB-OBS-004).

### 3. MDC population is isolated in `MdcCorrelationContextBridge`

Previously, `CorrelationFilter` contained direct `MDC.put`/`MDC.remove` calls. That coupling meant MDC was the only possible output from the correlation context. By moving MDC population to `MdcCorrelationContextBridge`, MDC becomes one pluggable carrier rather than the mandatory one. Future carriers (OpenTelemetry baggage, ScopeValue, Kafka headers) can be added as additional bridges without touching `CorrelationFilter`.

### 4. `bind()` skips `requestId` MDC entry when null

Non-HTTP execution models that establish a `CorrelationContext` without a `requestId` will not produce a `requestId=null` MDC entry. `MDC.put(key, null)` varies in behavior across SLF4J implementations and could produce confusing `"null"` strings in log output. Skipping the `put` call entirely is safe: `MDC.get(REQUEST_ID)` returns `null` in both cases.

### 5. `CorrelationFilter` no longer imports `StructuredLogFields` or `MDC`

The refactored filter has no direct knowledge of MDC. It imports only `CorrelationId`, `RequestId`, `DefaultCorrelationContext`, `CorrelationContextHolder`, and `MdcCorrelationContextBridge`. This makes the filter's dependency boundary explicit and ensures the MDC concern does not re-leak into the filter as the codebase evolves.

### 6. `CorrelationContextHolder.get()` returns `Optional<CorrelationContext>`

The empty-Optional response when no context is active is more expressive than a `@Nullable` return and avoids NPE risk at call sites. Future consumers (outbox poller, Kafka adapter) can check `CorrelationContextHolder.get().ifPresent(ctx -> ...)` without null guards.

## Architecture: Before and After

**Before:**
```
HTTP → CorrelationFilter → MDC (directly)
```

**After:**
```
HTTP → CorrelationFilter
              │
              ▼
       DefaultCorrelationContext → CorrelationContextHolder (ThreadLocal)
              │
              ▼
       MdcCorrelationContextBridge → MDC

Future:
Scheduler → DefaultCorrelationContext → CorrelationContextHolder
Kafka     → DefaultCorrelationContext → CorrelationContextHolder
```

## Validation

```
Platform module:
  Tests run: 398, Failures: 0, Errors: 0, Skipped: 0   (+18 new tests)

Contracts module:
  Tests run: 34, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
Build command: mvn -B test --no-transfer-progress -pl server/contracts,server/platform
```

## Tests

### `DefaultCorrelationContextTest` (6 tests)

| Test | Verifies |
|---|---|
| `rejects_null_correlationId` | NPE on null correlationId |
| `rejects_blank_correlationId` | IAE on blank correlationId |
| `accepts_null_requestId` | requestId may be null |
| `stores_both_fields_when_provided` | accessors return correct values |
| `implements_CorrelationContext` | usable through the interface |
| `record_equality_by_value` | equal when fields match |

### `CorrelationContextHolderTest` (6 tests)

| Test | Verifies |
|---|---|
| `get_returns_empty_when_nothing_set` | empty Optional when unset |
| `get_returns_context_after_set` | Optional contains installed context |
| `clear_removes_context` | get returns empty after clear |
| `set_replaces_existing_context` | second set wins |
| `set_rejects_null` | NPE guard on null context |
| `clear_is_idempotent_when_nothing_set` | safe to call clear twice |

### `MdcCorrelationContextBridgeTest` (6 tests)

| Test | Verifies |
|---|---|
| `bind_sets_correlationId_in_mdc` | correlationId written to MDC |
| `bind_sets_requestId_in_mdc_when_present` | requestId written when non-null |
| `bind_does_not_set_requestId_when_null` | no MDC entry when requestId null |
| `unbind_removes_correlationId` | correlationId cleared from MDC |
| `unbind_removes_requestId` | requestId cleared from MDC |
| `unbind_is_safe_when_nothing_bound` | safe to call unbind without bind |

### Existing `CorrelationFilterTest` — 11 tests — all pass unchanged

MDC behavior, header propagation, ADR-0008 W3C Trace Context isolation, and error-path cleanup are all preserved.

## Tradeoffs

- `CorrelationContextHolder.get()` returns `Optional` rather than providing a `getOrThrow()` convenience. Call sites that require a context must decide their own fallback behavior; this is intentional — the holder should not impose a policy on missing contexts.
- `MdcCorrelationContextBridge` is a static utility. A Spring `@Component` version would allow injection but would also require the bridge to be a Spring bean, adding infrastructure coupling that serves no benefit for a stateless, thread-bound utility. Static is correct here.
- `requestId` nullability is expressed at the `CorrelationContext` interface level rather than through a separate interface for HTTP vs. non-HTTP contexts. This keeps the model simple at the cost of a nullable field; callers that care about `requestId` must null-check. The trade-off is acceptable — `requestId` is an operational diagnostic field, not a routing or business concern.

## Open Questions

None for this slice.

## Follow-ups

Per the task roadmap:
- **ARB-OBS-002** — Scheduler Context Propagation: outbox poller and Spring `@Scheduled` tasks establish a `DefaultCorrelationContext` before each cycle
- **ARB-OBS-003** — Kafka Correlation Propagation: Kafka consumer adapter reads correlation headers and installs context via `CorrelationContextHolder`
- **ARB-OBS-004** — CompletionStage Context Propagation: explicit context transfer across async boundaries
- **ARB-OBS-005** — ScopeValue Migration (Java 21+ structured concurrency)

## References

- Task: ARB-OBS-001 — Unified Correlation Context
- Checkpoint: [docs/agents/checkpoints/CHECKPOINT-ARB-OBS-001-unified-correlation-context.md](../checkpoints/CHECKPOINT-ARB-OBS-001-unified-correlation-context.md)
- `StructuredLogFields`: `platform.logging.StructuredLogFields`
- `CorrelationFilter` (prior): `platform.web.CorrelationFilter`
- ADR-0008 (W3C Trace Context): `docs/adr/ADR-0008-...`
