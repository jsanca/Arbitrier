# ARB-016 — Saga Compensation

| Field  | Value       |
|--------|-------------|
| Task   | ARB-016     |
| Status | Implemented |
| Date   | 2026-07-09  |

## Summary

Failure paths and compensation for orchestrator-service: `HandleStockRejected`,
`HandleCreditRejected`, `HandleStockReleased`, and `HandleCompensationFailed` handlers;
semantic aggregate transitions (`stockRejected`, `creditRejected`, `inventoryReleased`);
outbound `ReleaseStockCommandPublisher` and `ReleaseCreditCommandPublisher` ports; new
domain events `SagaCancelledDomainEvent` and `SagaCompensationFailedDomainEvent`. All
flows execute using in-memory adapters — no JPA, Kafka, Avro, or Docker required.

---

## Compensation Flows

### StockRejected — direct cancel

```
StockRejected
  → saga.stockRejected()      status=CANCELLED (no compensation needed)
  → save saga
  → SagaCancelledDomainEvent
  (no release commands)
```

Stock was never reserved. No compensation command is issued.

### CreditRejected — compensate inventory

```
CreditRejected
  → saga.creditRejected()     status=COMPENSATING, step=COMPENSATE_INVENTORY
  → save saga
  → SagaCompensatedDomainEvent
  → ReleaseStockSagaCommand   (uses stored stockReservationId)
  (credit not released — was never approved)
```

### StockReleased — cancel after compensation

```
StockReleased (after CreditRejected compensation)
  → saga.inventoryReleased()  status=CANCELLED, step=COMPENSATE_INVENTORY
  → save saga
  → SagaCancelledDomainEvent
```

### CompensationFailed — terminal failure

```
CompensationFailed
  → saga.failCompensation()   status=FAILED_COMPENSATION
  → save saga
  → SagaCompensationFailedDomainEvent
```

Requires manual intervention.

---

## Domain Model Changes

### `Saga` — new semantic compensation methods

| Method | Pre-condition | Effect |
|--------|---------------|--------|
| `stockRejected()` | Non-terminal | → CANCELLED (preserves step) |
| `creditRejected()` | Non-terminal; `stockReservationId != null` | → COMPENSATING, step=COMPENSATE_INVENTORY |
| `inventoryReleased()` | status == COMPENSATING | → CANCELLED (preserves step) |

`failCompensation()` was already present from ARB-014; no change needed.

---

## Files Created

### Domain events

| File | Purpose |
|------|---------|
| `domain/event/SagaCancelledDomainEvent.java` | Emitted on direct or post-compensation cancellation (sagaId, orderId). |
| `domain/event/SagaCompensationFailedDomainEvent.java` | Emitted when saga reaches FAILED_COMPENSATION (sagaId, orderId). |

### Application — outbound ports

| File | Purpose |
|------|---------|
| `application/port/outbound/ReleaseStockSagaCommand.java` | Command issued to inventory-service (sagaId, stockReservationId, orderId). |
| `application/port/outbound/ReleaseStockCommandPublisher.java` | Publishes `ReleaseStockSagaCommand`. |
| `application/port/outbound/ReleaseCreditSagaCommand.java` | Command for future credit compensation (sagaId, creditReservationId, orderId). |
| `application/port/outbound/ReleaseCreditCommandPublisher.java` | Port for future credit compensation. See open questions. |

### Application — inbound ports

| File | Purpose |
|------|---------|
| `application/port/inbound/HandleStockRejectedCommand/Result/UseCase.java` | Input: sagaId. |
| `application/port/inbound/HandleCreditRejectedCommand/Result/UseCase.java` | Input: sagaId. |
| `application/port/inbound/HandleStockReleasedCommand/Result/UseCase.java` | Input: sagaId. |
| `application/port/inbound/HandleCompensationFailedCommand/Result/UseCase.java` | Input: sagaId. |

### Application services

| File | Purpose |
|------|---------|
| `application/service/HandleStockRejectedService.java` | Direct cancel; no release command. |
| `application/service/HandleCreditRejectedService.java` | COMPENSATING + ReleaseStock command. |
| `application/service/HandleStockReleasedService.java` | Cancel after inventory compensation. |
| `application/service/HandleCompensationFailedService.java` | FAILED_COMPENSATION terminal state. |

### Test adapters

| File | Purpose |
|------|---------|
| `adapter/outbound/RecordingReleaseStockCommandPublisher.java` | Captures issued ReleaseStock commands. |
| `adapter/outbound/RecordingReleaseCreditCommandPublisher.java` | Captures issued ReleaseCredit commands (future use). |

---

## Files Updated

| File | Change |
|------|--------|
| `domain/model/Saga.java` | Added `stockRejected()`, `creditRejected()`, `inventoryReleased()` with invariant Javadocs. |
| `application/port/outbound/SagaEventPublisher.java` | Added `publishCancelled()`, `publishCompensationFailed()`. |
| `adapter/outbound/RecordingSagaEventPublisher.java` | Implemented new publish methods; added `cancelledEvents()`, `compensationFailedEvents()`; updated `totalEventCount()`. |
| `config/OrchestratorServiceConfiguration.java` | Added beans for 4 new handler services + `ReleaseStockCommandPublisher`. |
| `integration/OrchestratorServiceTestConfiguration.java` | Added `@Primary` beans for `ReleaseStockCommandPublisher` and `ReleaseCreditCommandPublisher`. |
| `domain/SagaTest.java` | Added 11 tests for `stockRejected()`, `creditRejected()`, `inventoryReleased()`; total 37 tests. |

---

## Test Coverage

| Test class | Count | Scope |
|-----------|-------|-------|
| `SagaTest` | 37 | Domain aggregate — all compensation transitions |
| `HandleStockRejectedServiceTest` | 9 | Direct cancel, no release, terminal guard, not-found, validation |
| `HandleCreditRejectedServiceTest` | 10 | COMPENSATING, ReleaseStock command, missing-stock guard, not-found |
| `HandleStockReleasedServiceTest` | 9 | Cancel after compensation, step preserved, COMPENSATING guard |
| `HandleCompensationFailedServiceTest` | 6 | FAILED_COMPENSATION, event, not-found, validation |
| _(existing)_ | 96 | Happy path + ARB-014 foundation |
| **Total** | **139** | **All pass, no infrastructure required** |

---

## Open Questions

1. **`HandleCreditReleasedUseCase` — deferred to ARB-017**: In ARB-016 the CreditRejected
   flow never issues a ReleaseCredit command (credit was never approved). A future flow where
   credit is approved and then a later failure requires credit release belongs to ARB-017.
   The `ReleaseCreditCommandPublisher` port and `ReleaseCreditSagaCommand` are defined here
   in anticipation; no service or handler is wired.

2. **`failCompensation()` status guard**: The existing `failCompensation()` domain method
   has no status guard — it can be called from any state including non-COMPENSATING. This
   is intentional for flexibility (e.g., compensation infrastructure failure before the
   COMPENSATING status is set). A stricter guard may be added in ARB-017 once the
   compensation error-routing strategy is finalised.

3. **Transactionality**: Application services will become `@Transactional` when JPA
   persistence is introduced. DB + Kafka consistency will be handled by the Outbox pattern
   (ADR-0005).

4. **StockRejected idempotency**: Receiving a second StockRejected for an already-cancelled
   saga throws `IllegalArgumentException` (domain guard: non-terminal required). The Kafka
   consumer layer should handle this as an idempotent no-op once consumers are wired.
