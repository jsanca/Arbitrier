# ARB-016 REVIEW — Deep Saga Compensation

| Field | Value |
|-------|-------|
| **Reviewer** | Deep |
| **Date** | 2026-07-09 |
| **Task** | ARB-016 |
| **Status** | PLANNED |

---

## Verdict: **PASS WITH WARNINGS**

---

## Summary

ARB-016 implements the first four failure paths for UC-01 with clean hexagonal layering, strong domain invariants, and thorough test coverage (37 domain + 33 application service tests). The compensation model is semantically correct: the orchestrator owns all compensation decisions, and inventory/credit services only execute `release` commands. All flows execute through in-memory adapters with zero infrastructure dependencies.

Two warnings and one recommendation documented below.

---

## Blockers

None.

---

## Warnings

### Warning 1 — `failCompensation()` has no status guard (OPEN)

`Saga.failCompensation()` (`Saga.java:245`) creates a new instance unconditionally — no check for terminal status, no check for COMPENSATING:

```java
public Saga failCompensation() {
    return new Saga(id, orderId, customerId, SagaStatus.FAILED_COMPENSATION, currentStep,
            customerDecision, stockReservationId, creditReservationId);
}
```

This means `failCompensation()` can be called from any state including STARTED, COMPLETED, or CANCELLED — and multiple times. The doc at OQ #2 acknowledges this is intentional ("flexibility for compensation infrastructure failures before COMPENSATING is set"), but introduces two risks:

1. A STARTED saga that receives `CompensationFailed` (from an unrelated infrastructure error) would transition to FAILED_COMPENSATION without ever having entered COMPENSATING. Downstream consumers seeing FAILED_COMPENSATION would expect prior COMPENSATING status — this breaks observability contracts.

2. Idempotency is accidental (same output on each call) rather than enforced. If a consuming layer logs `FAILED_COMPENSATION` as a new event on each invocation, duplicate alerts fire.

**Recommendation for ARB-017**: Add a meaningful guard. At minimum `status == COMPENSATING || status == STARTED` if STARTED must be allowed. Document which statuses are valid pre-conditions.

### Warning 2 — Minor test count discrepancy in HandleStockRejectedServiceTest

The implementation doc (`ARB-016-saga-compensation.md:144`) claims 9 tests for `HandleStockRejectedServiceTest`. The actual file contains 8 `@Test` methods. The count is off by one.

Other counts match (10 CreditRejected, 9 StockReleased, 6 CompensationFailed, 37 SagaTest — verified).

---

## Compensation Review

### StockRejected → CANCELLED

**Flow** (`Saga.java:97`, `HandleStockRejectedService.java:48-61`):

```
stockRejected() → CANCELLED (preserves step) → SagaCancelledDomainEvent
```

- `stockRejected()` guards `!status.isTerminal()` — correct. No reservation was made, so no release command.
- `HandleStockRejectedService` publishes only `SagaCancelledDomainEvent` and explicitly verifies no other events or commands are emitted (`HandleStockRejectedServiceTest.java:68-73`).
- No ReleaseStock, no ReleaseCredit ✅
- Step is preserved, not advanced — caller sees where cancellation happened ✅

### CreditRejected → COMPENSATING → ReleaseStock

**Flow** (`Saga.java:114`, `HandleCreditRejectedService.java:57-71`):

```
creditRejected() → COMPENSATING + COMPENSATE_INVENTORY step → SagaCompensatedDomainEvent → ReleaseStockSagaCommand
```

- `creditRejected()` requires non-terminal status AND non-null `stockReservationId`. The `stockReservationId` invariant is verified at domain layer (`Saga.java:117-118`), not just application layer — this is correct enforcement of the business rule that you cannot compensate inventory you never reserved. ✅
- No ReleaseCredit command is emitted — credit was never approved ✅
- `ReleaseStockSagaCommand` carries `sagaId`, `stockReservationId`, `orderId` — the minimum required for inventory-service to execute release ✅

### StockReleased → CANCELLED

**Flow** (`Saga.java:131`, `HandleStockReleasedService.java:47-60`):

```
inventoryReleased() → CANCELLED (preserves step) → SagaCancelledDomainEvent
```

- `inventoryReleased()` requires `status == COMPENSATING` — the tightest possible guard ✅
- Step is preserved at `COMPENSATE_INVENTORY` — caller sees which compensation step ran ✅
- Terminal state reached after compensation completes ✅

### CompensationFailed → FAILED_COMPENSATION

**Flow** (`Saga.java:245`, `HandleCompensationFailedService.java:49-63`):

```
failCompensation() → FAILED_COMPENSATION → SagaCompensationFailedDomainEvent
```

- Terminal state requiring manual intervention ✅
- No guard on `failCompensation()` — see Warning 1
- Event type is distinct from `SagaCancelledDomainEvent` — correct, different downstream semantics ✅

---

## Saga Modeling

### Semantic transitions

| Method | Pre-condition | Semantic language |
|--------|---------------|-------------------|
| `stockRejected()` | non-terminal | Business term: stock was rejected = cancel |
| `creditRejected()` | non-terminal, stockReservationId != null | Business term: credit rejected = compensate inventory |
| `inventoryReleased()` | COMPENSATING | Business term: inventory released = saga cancelled |
| `failCompensation()` | none | Infrastructure term: compensation failure |

Three of four transitions use business language. `failCompensation()` is intentionally generic — it describes infrastructure failure, not a domain event. This is acceptable per the domain model's scope.

### Missing transitions before ARB-017

None currently. The four transitions cover:
- **Stock rejected before reservation** → direct cancel
- **Credit rejected after inventory** → compensate inventory → cancel
- **Inventory released during compensation** → cancel
- **Compensation infrastructure failure** → terminal failure

ARB-017 will logically add:
- `creditReleased()` — releasing credit during compensation (when credit was approved)
- `completed()` — already exists, used when full-allocation happy path reaches terminal
- Customer decision transitions — already exist (`awaitCustomerDecision()`, `applyCustomerDecision()`)

---

## Aggregate Invariants

All invariants reside inside the `Saga` aggregate (`Saga.java`).

| Pre-condition | Enforced in | Rule |
|---------------|-------------|------|
| Non-terminal status | `stockRejected()`, `creditRejected()`, `inventoryReserved()`, `creditApproved()`, `advance()`, `compensate()`, `cancel()`, `complete()` | Domain guard via `Require.isTrue(!status.isTerminal())` |
| `stockReservationId != null` | `creditRejected()` | Domain guard via `Require.isTrue(stockReservationId != null)` |
| Status == COMPENSATING | `inventoryReleased()` | Domain guard via `Require.isTrue(status == SagaStatus.COMPENSATING)` |
| Status == STARTED | `awaitCustomerDecision()` | Domain guard via `Require.isTrue(status == SagaStatus.STARTED)` |
| Status == AWAITING_CUSTOMER_DECISION | `applyCustomerDecision()` | Domain guard via `Require.isTrue(status == AWAITING_CUSTOMER_DECISION)` |
| Not COMPENSATING | `advance()` | Domain guard via `Require.isTrue(status != SagaStatus.COMPENSATING)` |
| Not already COMPENSATING | `compensate()` | Domain guard via `Require.isTrue(status != SagaStatus.COMPENSATING)` |
| `failCompensation()` | **NO GUARD** | See Warning 1 |

Terminal saga rules: Terminal sagas (COMPLETED, CANCELLED, FAILED_COMPENSATION) cannot transition further — all methods requiring non-terminal status enforce this. `FAILED_COMPENSATION` is terminal but `failCompensation()` can still be called (no-op/idempotent).

---

## Architectural Consistency

### Layer boundaries

| Check | Result |
|-------|--------|
| Domain is pure Java | ✅ `Saga.java` — no Spring/JPA/Kafka imports |
| Application depends only on ports/domain | ✅ `Handle*Service` classes depend on `SagaRepository`, `SagaEventPublisher`, command/result ports |
| Domain events are pure Java records | ✅ `SagaCancelledDomainEvent`, `SagaCompensationFailedDomainEvent` |
| Outbound ports are interfaces | ✅ `ReleaseStockCommandPublisher`, `ReleaseCreditCommandPublisher` |
| Test adapters isolated | ✅ `InMemorySagaRepository`, `Recording*Publisher` in `adapter/outbound/` test package |
| ArchitectureTest enforces all rules | ✅ 5 ArchUnit rules in `unit/ArchitectureTest.java` |
| `package-info.java` in every package | ✅ Verified all orchestrator-service packages |

### Application service story pattern

Each handler follows the ARB standard:
1. **validate** — command constructor validates input (via `Require`)
2. **derive** — not needed in these simple handlers
3. **execute domain** — `saga.stockRejected()` / `saga.creditRejected()` etc.
4. **persist** — `repository.save(cancelled)`
5. **publish** — `eventPublisher.publishCancelled(...)` / `publishCompensated(...)`
6. **return** — `HandleStockRejectedResult`

`HandleCreditRejectedService` additionally issues the release command after publish — this is the correct order (persist + publish before sending command to downstream service ensures the aggregate state is durable first).

### Compensation ownership

The orchestrator correctly owns the compensation decision:
- `HandleCreditRejectedService` decides WHEN and WHAT to compensate (ReleaseStock, not ReleaseCredit) ✅
- Inventory-service's `ReleaseStockService` only executes release (no compensation logic) ✅
- Credit-service's `ReleaseCreditService` only executes release (no compensation logic) ✅
- Neither inventory nor credit imports orchestrator types ✅

### Comparison with previous slices

| Principle | ARB-014 | ARB-015 | ARB-016 |
|-----------|---------|---------|---------|
| Domain owns invariants | ✅ | ✅ | ✅ |
| Semantic transitions | `complete()`, `cancel()`, `compensate()` | `inventoryReserved()`, `creditApproved()` | `stockRejected()`, `creditRejected()`, `inventoryReleased()` |
| Strongly typed VOs | `SagaId` | — | — |
| Application tells business story | ✅ | ✅ | ✅ |
| Test adapters isolated | ✅ | ✅ | ✅ |
| No infrastructure leakage | ✅ | ✅ | ✅ |

---

## Compensation Readiness (ARB-017 boundary)

### Saga memory

Current fields on `Saga`:

| Field | Present | Used by ARB-016 |
|-------|---------|-----------------|
| `id` (SagaId) | ✅ | Saga identification |
| `orderId` (String) | ✅ | Audit, event publishing |
| `customerId` (String) | ✅ | Not used in compensation (pass-through) |
| `status` (SagaStatus) | ✅ | State machine |
| `currentStep` (SagaStep) | ✅ | Compensation context |
| `customerDecision` (CustomerDecision) | ✅ | Not used in compensation |
| `stockReservationId` (String) | ✅ | CRITICAL — required by `creditRejected()` for ReleaseStock |
| `creditReservationId` (String) | ✅ | Not used in ARB-016 (credit never approved in compensation path) |

ARB-017 will likely need:
- **Order line items / SKUs** — for backorder scenarios where partial inventory reservation requires understanding which lines were fulfilled
- **Rejection reason / error codes** — for observability and customer-facing status
- **Timestamps** — for timeout-based compensation (currently no scheduler exists)

The current fields are sufficient for ARB-016. ARB-017 additions are expected and natural.

### Remaining gaps before ARB-017

| Gap | Impact |
|-----|--------|
| `HandleCreditReleasedUseCase` defined but no handler wired | Intentional — deferred to ARB-017 |
| `ReleaseCreditCommandPublisher` port exists, `RecordingReleaseCreditCommandPublisher` too | No publisher calls `publishReleaseCredit` yet |
| No Kafka consumers for inbound events (StockRejected, CreditRejected, StockReleased, CompensationFailed) | Handlers exist but no inbound adapter wiring |
| No Kafka producers for outbound commands (ReleaseStock) | `ReleaseStockCommandPublisher` port exists but no `KafkaReleaseStockCommandPublisher` implementation |

All gaps are documented in the implementation doc as deferred or OQ.

---

## Native Image Compatibility

| Check | Result |
|-------|--------|
| No `Class.forName()` | ✅ |
| No runtime classpath scanning | ✅ |
| No dynamic proxies | ✅ |
| No new `RuntimeHintsRegistrar` needed | ✅ (no `@Entity`, no new Avro types) |
| No reflection | ✅ |

All code is plain Java records, enums, and interfaces. GraalVM native-image compatibility is preserved. No new registration required.

---

## Idempotency Assessment

| Duplicate scenario | Domain behavior | Idempotency layer |
|--------------------|-----------------|-------------------|
| Duplicate `StockRejected` | `stockRejected()` throws — CANCELLED is terminal | Consumer layer must catch and no-op |
| Duplicate `CreditRejected` | `creditRejected()` throws — COMPENSATING or CANCELLED is terminal | Consumer must catch and no-op |
| Duplicate `StockReleased` | `inventoryReleased()` throws — CANCELLED status | Consumer must catch and no-op |
| Duplicate `CompensationFailed` | No guard — succeeds silently as no-op (output same) | Application layer already idempotent |

**Recommendation**: Idempotency enforcement should live at the consumer layer (Kafka inbound adapter, when built). The domain and application layers correctly throw for invalid state transitions. The consumer adapter (ARB-017+) should catch `IllegalArgumentException` for terminal-state violations and log+ack rather than bubble the error. The `CompensationFailed` path is already idempotent at the application layer due to the missing guard.

---

## Recommendations

1. **Guarded `failCompensation()` (see Warning 1)**: Add a pre-condition check before ARB-017 ships. At minimum `status == COMPENSATING || status == STARTED`. This prevents silent transitions from CANCELLED or COMPLETED into FAILED_COMPENSATION (which would be semantically wrong).

2. **Consumer-layer idempotency pattern (ARB-017)**: When Kafka consumer adapters are wired, standardize an `IdempotentEventHandler` that catches `IllegalArgumentException` from domain guards and treats terminal-state violations as acknowledge-and-skip, not fail-and-retry. This pattern already exists as a concept in `IdempotencyStore` (platform) but no adapter implementation exists.

3. **Test count consistency**: Update `HandleStockRejectedServiceTest` doc count to 8, or add an additional test to reach 9. The current 8 tests cover the essential flows (result, state, event, no-commands, terminal guard, not-found, blank-id). No test gap identified — only a doc discrepancy.

---

## Decision

ARB-016 may be marked **DONE** after documenting the two warnings in the appropriate repository. No code changes required before ARB-017 begins.

The compensation model is semantically correct, the architectural boundaries are preserved, and the project is ready for ARB-017.
