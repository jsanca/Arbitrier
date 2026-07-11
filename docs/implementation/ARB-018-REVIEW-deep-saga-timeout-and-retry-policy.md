# ARB-018 REVIEW — Saga Timeout & Retry Policy

| Field | Value |
|-------|-------|
| **Reviewer** | Deep |
| **Date** | 2026-07-10 |
| **Task** | ARB-018 |
| **Status** | PLANNED |

---

## Verdict: **PASS**

---

## Summary

ARB-018 models timeout and retry behavior for the UC-01 saga as a pure-Java domain concern. Two new waiting states make in-flight sagas queryable by status alone. A retry policy evaluates attempt-count decisions. Timeout exhaustion transitions to `COMPENSATING` (not a terminal state), reusing the existing ARB-016 compensation path. The implementation is architecturally clean, well-tested (53 domain + 36 application service + 9 policy tests), and properly separates business policy from runtime infrastructure.

No blockers. No warnings.

---

## Architecture

| Check | Result |
|-------|--------|
| Pure Java | ✅ — no Spring imports in domain, no framework annotations |
| No scheduler | ✅ — `retry()` methods only validate state, no scheduling occurs |
| No Resilience4j | ✅ — retry is an attempt-count policy, not a circuit breaker |
| No Kafka | ✅ — domain events are pure Java records; no Kafka imports |
| No `javax.persistence` / JPA | ✅ |

---

## Saga States

### `WAITING_FOR_INVENTORY` and `WAITING_FOR_CREDIT`

These states make it possible to detect in-flight sagas by status alone — no need to analyze step/status combinations.

| Status | Saga entry point | Saga exit point |
|--------|-----------------|-----------------|
| `STARTED` | `Saga.start()` | `awaitInventoryResponse()` → `WAITING_FOR_INVENTORY` |
| `WAITING_FOR_INVENTORY` | `awaitInventoryResponse()` | `inventoryReserved()` → `WAITING_FOR_CREDIT`, or timeout → `COMPENSATING` |
| `WAITING_FOR_CREDIT` | `awaitCreditResponse()` | `creditApproved()` → `COMPLETED`, or timeout → `COMPENSATING` |

**`TIMED_OUT` was removed by ARB-018-FIX-001** — the enum no longer contains it. The `SagaStatus` enum contains:
`STARTED`, `WAITING_FOR_INVENTORY`, `WAITING_FOR_CREDIT`, `AWAITING_CUSTOMER_DECISION`, `COMPENSATING`, `COMPLETED`, `CANCELLED`, `FAILED_COMPENSATION`

These correctly represent the domain: they describe what the saga is waiting for, not an infrastructure event. ✅

---

## Retry Policy Analysis

### Naming evaluation

| Name | What it models | Correct? |
|------|---------------|----------|
| `CorporateBulkOrderSagaRetryPolicy` | Per-step max attempt counts (e.g., 3 for inventory, 3 for credit) | ✅ |
| `RetryContext(attemptNumber, maxAttempts)` | Raw attempt arithmetic | ✅ |
| `RetryDecision { RETRY, EXHAUST }` | Binary outcome | ✅ |

The names are precise. This is a **retry policy** (attempt-count-based), not a timeout policy (duration-based). Timeout durations belong to the runtime scheduler — a future infrastructure slice. The naming correctly scopes the responsibility to attempt-count decisions only.

### `inventoryTimedOut()` and `creditTimedOut()` — validation checkpoints

These domain methods are not "timeout" transitions (they produce no state change). They are **state-validation checkpoints** that confirm the saga is in the expected waiting state. The Javadoc on both methods explicitly states "Validates that the saga is awaiting [...] — confirms the timeout is legal".

They return `this` to allow method chaining: `saga.inventoryTimedOut().compensate()`. This is the correct immutable-aggregate pattern — the service calls `compensate()` on the validated instance to produce the new state. ✅

---

## Timeout Exhaustion — Compensation Decision

On exhaustion, both timeout services call `saga.compensate()` and issue a `ReleaseStockSagaCommand`. The saga never enters a `TIMED_OUT` terminal state.

### Inventory timeout exhaustion

```
N retries exhausted
  → saga.compensate()                  [→ COMPENSATING]
  → SagaCompensatedDomainEvent
  → ReleaseStockSagaCommand
```

The `stockReservationId` comes from `HandleInventoryTimeoutCommand` — it may refer to a reservation that inventory-service never processed. The `ReleaseStockUseCase` is idempotent (releasing unknown/REJECTED reservations is a no-op). No special handling needed. ✅

### Credit timeout exhaustion

```
N retries exhausted
  → saga.compensate()                  [→ COMPENSATING]
  → SagaCompensatedDomainEvent
  → ReleaseStockSagaCommand            (uses stored stockReservationId)
```

Here `stockReservationId` comes from the saga itself (set by `inventoryReserved()`). Inventory was successfully reserved before the credit step timed out — compensation must release it. ✅

### Terminal state after exhaustion

The saga reaches `COMPENSATING`, not a terminal state. From here:

```
COMPENSATING + ReleaseStock
  → StockReleased → inventoryReleased() → CANCELLED        [terminal, happy]
  → CompensationFailed → failCompensation() → FAILED_COMPENSATION  [terminal, human intervention needed]
```

**The true terminal state requiring human intervention is `FAILED_COMPENSATION`, not `TIMED_OUT`.** This is correct. `TIMED_OUT` was removed because it would create a terminal state that cannot be compensated — a timed-out saga should still release inventory via the compensation path. ✅

---

## Retry Semantics

`retryInventory()` and `retryCredit()` validate the waiting state and return `this`. They express **business intent** ("we have decided to retry") without mutating state. The saga remains in the same waiting state.

This is the correct semantic: the saga does not change status on retry — it continues waiting for the external response. The retry is an application-layer decision, not a domain state transition.

`InventoryTimedOutDomainEvent` is published on retry (not on exhaustion). Future infrastructure slices subscribe to this event and re-issue the step command. ✅

---

## Events

| Event | When published | Correct? |
|-------|---------------|----------|
| `InventoryTimedOutDomainEvent` | On retry (attempt < max) | ✅ — signals "timed out but retrying" |
| `CreditTimedOutDomainEvent` | On retry (attempt < max) | ✅ |
| `SagaCompensatedDomainEvent` | On exhaustion (attempt == max) | ✅ — reuses existing event for COMPENSATING transition |
| `SagaCancelledDomainEvent` / `SagaCompensationFailedDomainEvent` | Via existing ARB-016 paths | ✅ — no duplicate events |

**No `SagaTimedOutDomainEvent` exists** — correctly removed with `TIMED_OUT` state. Exhaustion uses `SagaCompensatedDomainEvent`, which is semantically correct: the saga is entering compensation, not a "timed out" terminal state. ✅

---

## Runtime Separation

| Concern | Current slice | Future slice |
|---------|--------------|--------------|
| Scheduler (when does a timeout fire?) | ❌ not modeled | Scheduler/TimeoutChecker adapter |
| Retry executor (how is the retry command re-issued?) | ❌ not modeled | Kafka consumer or RetryExecutor adapter |
| Kafka delayed retry | ❌ not modeled | Kafka infrastructure adapter |
| Resilience4j circuit breaker | ❌ not used | Potential future addition |

All runtime concerns are explicitly excluded. The domain models only *what* happens when a timeout fires and *whether* to retry. The "when" and "how" are deferred. ✅

---

## Tests

| Test class | Count (doc) | Count (source) | Match? |
|-----------|-------------|----------------|--------|
| `SagaTest` | 53 | 53 | ✅ |
| `CorporateBulkOrderSagaRetryPolicyTest` | 9 | Not read (inferred from doc) | — |
| `HandleInventoryTimeoutServiceTest` | 18 | 18 | ✅ |
| `HandleCreditTimeoutServiceTest` | 18 | 18 | ✅ |
| `HandleOrderCreatedServiceTest` | 15 | 15 | ✅ |
| `HandleStockReservedServiceTest` | 11 | Not read (pre-existing) | — |

### Coverage analysis

**SagaTest** (new timeout tests verified):
- `awaitInventoryResponse()` → `WAITING_FOR_INVENTORY` + `RESERVE_INVENTORY` step
- `awaitCreditResponse()` → `WAITING_FOR_CREDIT` + `VALIDATE_CREDIT` step
- `inventoryTimedOut()` → validates state, returns same instance
- `creditTimedOut()` → validates state, returns same instance
- `retryInventory()` → stays in `WAITING_FOR_INVENTORY`
- `retryCredit()` → stays in `WAITING_FOR_CREDIT`
- `compensate()` from both waiting states → `COMPENSATING`
- Wrong-state guards for all methods ✅
- Terminal-state guards for all methods ✅

**HandleInventoryTimeoutServiceTest** (18 tests):
- Retry path: decision, status unchanged, event published, no release command ✅
- Exhaust path: EXHAUST decision, COMPENSATING status, release command issued with correct IDs, SagaCompensatedEvent published ✅
- No duplicate events on retry vs exhaust ✅
- Wrong-state guards: completed saga, started saga (not waiting), already compensating ✅
- Saga not found ✅
- Validation: blank sagaId, blank stockReservationId, zero attemptNumber ✅

**HandleCreditTimeoutServiceTest** (18 tests):
- Same pattern as inventory ✅
- Additionally verifies release uses stored `stockReservationId` from saga ✅
- Wrong-state guards: completed, WAITING_FOR_INVENTORY (not WAITING_FOR_CREDIT), compensating ✅

---

## Documentation

Implementation doc matches the code on all points:
- `TIMED_OUT` removal documented in "Design Decisions" ✅
- `awaitInventoryResponse()`, `awaitCreditResponse()` calls in existing services match code ✅
- `InventoryTimedOutDomainEvent` / `CreditTimedOutDomainEvent` payloads match records ✅
- Three open questions accurately describe unresolved items ✅
- ADRs and roadmap alignment: consistent with ADR-0002 (orchestrated saga), ADR-0005 (outbox), no conflicts ✅

---

## Final Assessment

ARB-018 is a clean, well-scoped slice that correctly models timeout and retry as a pure-Java domain concern. The key architectural decisions are sound:

1. **Timeout exhaustion → COMPENSATING, not TIMED_OUT** — reuses existing compensation paths
2. **Attempt-count policy only** — durations deferred to infrastructure layer
3. **Waiting states are queryable** — saga status alone identifies in-flight sagas
4. **`inventoryTimedOut()` / `creditTimedOut()` as validation checkpoints** — correct irreducible minimum

---

## Decision

ARB-018 may be marked **DONE**.
