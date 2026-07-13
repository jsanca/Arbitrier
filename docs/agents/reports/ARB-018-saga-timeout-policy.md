# ARB-018 — Saga Timeout & Retry Policy

| Field  | Value       |
|--------|-------------|
| Task   | ARB-018     |
| Status | Implemented (incl. ARB-018-FIX-001) |
| Date   | 2026-07-10  |

## Summary

Models timeout and retry behavior for the UC-01 saga as a pure-Java business concern.
The implementation defines timeout states, timeout transitions, retry decisions, and
timeout domain events — **without introducing any runtime scheduler, framework dependency,
or infrastructure**.

**Business timeout ≠ runtime scheduler.** This slice models *what happens* when a step
times out and whether to retry. *When* the timeout fires and *how* the retry is executed
are concerns for future Runtime / Infrastructure slices.

---

## Design Decisions

### New saga states

Two new waiting states were introduced to make it possible to detect in-flight sagas
without analyzing step/status combinations:

| Status                | Meaning |
|-----------------------|---------|
| `WAITING_FOR_INVENTORY` | ReserveStock command issued; awaiting inventory-service response |
| `WAITING_FOR_CREDIT`    | ReserveCredit command issued; awaiting credit-service response |

`TIMED_OUT` was initially introduced as a terminal state and removed by ARB-018-FIX-001.
Timeout exhaustion transitions to `COMPENSATING` instead — the existing compensation path
(`HandleStockReleasedService` → `CANCELLED`, `HandleCompensationFailedService` → `FAILED_COMPENSATION`)
handles the terminal transition. `FAILED_COMPENSATION` remains the state requiring human
intervention.

### Awaiting transitions called in existing services

`HandleOrderCreatedService` now calls `awaitInventoryResponse()` before persisting the
saga (status `WAITING_FOR_INVENTORY`, step `RESERVE_INVENTORY`).

`HandleStockReservedService` now calls `awaitCreditResponse()` after `inventoryReserved()`
before persisting (status `WAITING_FOR_CREDIT`, step `VALIDATE_CREDIT`).

This makes the saga self-descriptive: a query for sagas in `WAITING_FOR_INVENTORY` finds
all sagas waiting for an inventory reply, ready for a timeout check.

### Timeout exhaustion triggers compensation, not a terminal state

On exhaustion, both timeout services call `saga.compensate()` and issue a
`ReleaseStockSagaCommand`. This ensures:
- Inventory always gets a compensating release, regardless of which step timed out.
- For inventory timeout: the reservation ID came from `HandleInventoryTimeoutCommand` —
  if inventory-service never processed the original command, the release is an idempotent no-op.
- For credit timeout: the saga already stored `stockReservationId` from `inventoryReserved()`.
- The existing `HandleStockReleasedService` closes the compensation to `CANCELLED`.
- `FAILED_COMPENSATION` is reached only if compensation itself fails (infrastructure fault).

### `CorporateBulkOrderSagaRetryPolicy` — retry attempts only, no durations

The policy record expresses maximum attempt counts per step. It does not carry timeout
durations — those belong to the runtime scheduler (a future infrastructure slice).
`RetryContext(attemptNumber, maxAttempts).evaluate()` does the arithmetic.

### `inventoryTimedOut()` and `creditTimedOut()` as state-validation checkpoints

These domain methods validate that the saga is in the correct waiting state before the
timeout is processed. They return `this` — the application service then decides to retry
or compensate based on the policy.

### `retryInventory()` and `retryCredit()` as semantic anchors

Both methods validate the waiting state and return `this` (no status change). They
express the business intent "we have decided to retry" without mutating state, consistent
with the immutable aggregate pattern.

---

## Timeout Flow

```
Timeout event arrives (attempt N)
  → load saga (must be WAITING_FOR_INVENTORY or WAITING_FOR_CREDIT)
  → saga.inventoryTimedOut() or saga.creditTimedOut()         [validates state]
  → policy.evaluateInventory(N) or evaluateCredit(N)
      → if RETRY:
            saga.retryInventory() / saga.retryCredit()          [stays in waiting state]
            persist
            publish InventoryTimedOutDomainEvent / CreditTimedOutDomainEvent

      → if EXHAUST:
            saga.compensate()                                    [→ COMPENSATING]
            persist
            publish SagaCompensatedDomainEvent
            publish ReleaseStockSagaCommand (idempotent)

StockReleased
  → saga.inventoryReleased()                                    [→ CANCELLED]

CompensationFailed
  → saga.failCompensation()                                     [→ FAILED_COMPENSATION]
```

---

## Files Created

### Domain model

| File | Purpose |
|------|---------|
| `domain/model/RetryDecision.java` | Enum: `RETRY`, `EXHAUST`; `shouldRetry()` helper |
| `domain/model/RetryContext.java` | Record `(attemptNumber, maxAttempts)`; `evaluate()` |
| `domain/model/CorporateBulkOrderSagaRetryPolicy.java` | Per-step max attempts; `evaluateInventory()`, `evaluateCredit()` — attempt counts only, no durations |

### Domain events

| File | Purpose |
|------|---------|
| `domain/event/InventoryTimedOutDomainEvent.java` | `(sagaId, orderId, attemptNumber)` — emitted on retry |
| `domain/event/CreditTimedOutDomainEvent.java` | `(sagaId, orderId, attemptNumber)` — emitted on retry |

### Inbound ports

| File | Purpose |
|------|---------|
| `application/port/inbound/HandleInventoryTimeoutCommand.java` | `(sagaId, stockReservationId, attemptNumber)` |
| `application/port/inbound/HandleInventoryTimeoutResult.java` | `(sagaId, RetryDecision)` |
| `application/port/inbound/HandleInventoryTimeoutUseCase.java` | Interface |
| `application/port/inbound/HandleCreditTimeoutCommand.java` | `(sagaId, creditReservationId, attemptNumber)` |
| `application/port/inbound/HandleCreditTimeoutResult.java` | `(sagaId, RetryDecision)` |
| `application/port/inbound/HandleCreditTimeoutUseCase.java` | Interface |

### Application services

| File | Purpose |
|------|---------|
| `application/service/HandleInventoryTimeoutService.java` | On retry: publishes `InventoryTimedOutDomainEvent`. On exhaust: `compensate()` + `SagaCompensatedDomainEvent` + `ReleaseStockSagaCommand` |
| `application/service/HandleCreditTimeoutService.java` | Same pattern for credit step |

---

## Files Modified

| File | Change |
|------|--------|
| `domain/model/SagaStatus.java` | Added `WAITING_FOR_INVENTORY`, `WAITING_FOR_CREDIT` |
| `domain/model/Saga.java` | Added `awaitInventoryResponse()`, `awaitCreditResponse()`, `inventoryTimedOut()`, `creditTimedOut()`, `retryInventory()`, `retryCredit()` |
| `application/port/outbound/SagaEventPublisher.java` | Added `publishInventoryTimedOut()`, `publishCreditTimedOut()` |
| `adapter/outbound/RecordingSagaEventPublisher.java` (test) | Implemented new publisher methods; updated `totalEventCount()` |
| `application/service/HandleOrderCreatedService.java` | Calls `awaitInventoryResponse()` after `Saga.start()` |
| `application/service/HandleStockReservedService.java` | Calls `awaitCreditResponse()` after `inventoryReserved()` |
| `config/OrchestratorServiceConfiguration.java` | Added `CorporateBulkOrderSagaRetryPolicy` bean (3/3 max attempts); added 2 new use case beans with `ReleaseStockCommandPublisher` |

---

## Test Coverage

| Test class | Count | Scope |
|------------|-------|-------|
| `SagaTest` | 53 | Timeout transitions, waiting-state compensate, state guards |
| `CorporateBulkOrderSagaRetryPolicyTest` | 9 | Policy naming, per-step limits, `RetryDecision` helpers |
| `HandleInventoryTimeoutServiceTest` | 18 | Retry path, exhaustion → COMPENSATING + ReleaseStock, wrong-state guards, validation |
| `HandleCreditTimeoutServiceTest` | 18 | Same pattern for credit |
| `HandleOrderCreatedServiceTest` | 15 | Updated status assertion to `WAITING_FOR_INVENTORY` |
| `HandleStockReservedServiceTest` | 11 | Updated setUp to use `awaitInventoryResponse()` |
| **Total orchestrator-service** | **201** | All pass |

### Test scenarios covered (ARB-018-FIX-001 additions)

- Inventory timeout exhaustion → `COMPENSATING`, `SagaCompensatedDomainEvent`, `ReleaseStockSagaCommand` issued
- Credit timeout exhaustion → `COMPENSATING`, `SagaCompensatedDomainEvent`, `ReleaseStockSagaCommand` with stored `stockReservationId`
- Retry path does not issue release command
- Exhaustion does not publish step-timeout event (only `SagaCompensatedDomainEvent`)
- Timeout on COMPENSATING saga → throws (state guard)
- Successful release (existing `HandleStockReleasedServiceTest`) ends `CANCELLED`
- Failed compensation (existing `HandleCompensationFailedServiceTest`) ends `FAILED_COMPENSATION`
- Policy naming test: `CorporateBulkOrderSagaRetryPolicyTest`

---

## Open Questions

1. **`CorporateBulkOrderSagaRetryPolicy` configuration**: Currently hardcoded as
   `new CorporateBulkOrderSagaRetryPolicy(3, 3)` in Spring config. Future work should
   externalise these values via application properties without introducing a Spring
   `@ConfigurationProperties` dependency on the domain record.

2. **Inventory timeout idempotent release — no reservation created**: The `stockReservationId`
   in `HandleInventoryTimeoutCommand` was issued but inventory-service may have never processed
   it. The `ReleaseStockUseCase` is idempotent — releasing an unknown or REJECTED reservation
   is a safe no-op. This is the intended contract; no special handling is needed.

3. **`InventoryTimedOutDomainEvent` / `CreditTimedOutDomainEvent` consumers**: Events are
   published on retry but no runtime consumer exists yet. Future infrastructure slices will
   subscribe and re-issue the step command.
