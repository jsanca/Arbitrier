# ARB-015 — Saga Happy Path

| Field  | Value       |
|--------|-------------|
| Task   | ARB-015     |
| Status | Implemented |
| Date   | 2026-07-09  |

## Summary

UC-01 happy-path orchestration for orchestrator-service: three event handlers
(`HandleOrderCreated`, `HandleStockReserved`, `HandleCreditApproved`), three outbound command
ports, semantic aggregate transitions (`inventoryReserved`, `creditApproved`), and reservation-ID
persistence. The full business flow executes using in-memory adapters — no JPA, Kafka, Avro, or
Docker required.

---

## Domain Model Changes

### `Saga` — added `stockReservationId`, `creditReservationId`, semantic transitions

| Change | Description |
|--------|-------------|
| `stockReservationId` field | Stored when inventory confirms reservation; required for ARB-016 compensation. |
| `creditReservationId` field | Stored when credit confirms approval; required for ARB-016 compensation. |
| `inventoryReserved(stockReservationId)` | Records the stock reservation ID and advances step to `VALIDATE_CREDIT`. |
| `creditApproved(creditReservationId)` | Records the credit reservation ID; step unchanged (caller chains `.complete()`). |
| `stockReservationId()` getter | Exposes stored stock reservation ID. |
| `creditReservationId()` getter | Exposes stored credit reservation ID. |

All existing transition methods (`advance`, `compensate`, `complete`, `cancel`, etc.) thread
the new fields through unchanged so the aggregate remains immutable.

### `SagaCompletedDomainEvent`

Emitted when the saga reaches the `COMPLETED` terminal state.  
Fields: `sagaId`, `orderId`.

---

## Files Created

### Domain events

| File | Purpose |
|------|---------|
| `domain/event/SagaCompletedDomainEvent.java` | Emitted on saga completion (sagaId, orderId). |

### Application — outbound ports

| File | Purpose |
|------|---------|
| `application/port/outbound/ReserveStockSagaCommand.java` | Command issued to inventory-service (sagaId, stockReservationId, orderId). |
| `application/port/outbound/ReserveStockCommandPublisher.java` | Publishes `ReserveStockSagaCommand`. |
| `application/port/outbound/ReserveCreditSagaCommand.java` | Command issued to credit-service (sagaId, creditReservationId, orderId, customerId). |
| `application/port/outbound/ReserveCreditCommandPublisher.java` | Publishes `ReserveCreditSagaCommand`. |
| `application/port/outbound/ConfirmOrderSagaCommand.java` | Command issued to order-service (sagaId, orderId). |
| `application/port/outbound/ConfirmOrderCommandPublisher.java` | Publishes `ConfirmOrderSagaCommand`. |

### Application — inbound ports

| File | Purpose |
|------|---------|
| `application/port/inbound/HandleOrderCreatedCommand.java` | Input: sagaId, orderId, customerId. |
| `application/port/inbound/HandleOrderCreatedResult.java` | Output: sagaId, generated stockReservationId. |
| `application/port/inbound/HandleOrderCreatedUseCase.java` | Entry point for the OrderCreated event. |
| `application/port/inbound/HandleStockReservedCommand.java` | Input: sagaId, stockReservationId. |
| `application/port/inbound/HandleStockReservedResult.java` | Output: sagaId, generated creditReservationId. |
| `application/port/inbound/HandleStockReservedUseCase.java` | Entry point for the StockReserved event. |
| `application/port/inbound/HandleCreditApprovedCommand.java` | Input: sagaId, creditReservationId. |
| `application/port/inbound/HandleCreditApprovedResult.java` | Output: sagaId. |
| `application/port/inbound/HandleCreditApprovedUseCase.java` | Entry point for the CreditApproved event. |

### Application services

| File | Purpose |
|------|---------|
| `application/service/HandleOrderCreatedService.java` | Implements `HandleOrderCreatedUseCase`. |
| `application/service/HandleStockReservedService.java` | Implements `HandleStockReservedUseCase`. |
| `application/service/HandleCreditApprovedService.java` | Implements `HandleCreditApprovedUseCase`. |

### Test adapters

| File | Purpose |
|------|---------|
| `adapter/outbound/RecordingReserveStockCommandPublisher.java` | Captures issued ReserveStock commands. |
| `adapter/outbound/RecordingReserveCreditCommandPublisher.java` | Captures issued ReserveCredit commands. |
| `adapter/outbound/RecordingConfirmOrderCommandPublisher.java` | Captures issued ConfirmOrder commands. |

---

## Files Updated

| File | Change |
|------|--------|
| `domain/model/Saga.java` | Added `stockReservationId`, `creditReservationId` fields and `inventoryReserved()`, `creditApproved()` methods. All other transitions updated to thread new fields. |
| `application/port/outbound/SagaEventPublisher.java` | Added `publishCompleted(SagaCompletedDomainEvent)`. |
| `adapter/outbound/RecordingSagaEventPublisher.java` | Implemented `publishCompleted()`; added `completedEvents()` list; updated `totalEventCount()`. |
| `config/OrchestratorServiceConfiguration.java` | Added `@Bean` wiring for three handler services; imports for three command publishers. |
| `integration/OrchestratorServiceTestConfiguration.java` | Added `@Primary` beans for three recording command publishers. |
| `domain/SagaTest.java` | Added tests for `inventoryReserved()`, `creditApproved()`, and combined happy-path chain; total 27 tests. |

---

## Service Behaviour

### `HandleOrderCreatedService`

1. Parse `SagaId` from command.
2. Create `Saga.start(id, orderId, customerId)` — status `STARTED`, step `ORDER_CREATED`.
3. Generate `stockReservationId` (UUID).
4. Persist via `SagaRepository`.
5. Publish `SagaStartedDomainEvent`.
6. Publish `ReserveStockSagaCommand(sagaId, stockReservationId, orderId)`.
7. Return `HandleOrderCreatedResult(sagaId, stockReservationId)`.

### `HandleStockReservedService`

1. Parse `SagaId`; load saga or throw `IllegalArgumentException`.
2. Call `saga.inventoryReserved(stockReservationId)` — records ID, advances to `VALIDATE_CREDIT`.
3. Generate `creditReservationId` (UUID).
4. Persist updated saga.
5. Publish `SagaAdvancedDomainEvent(sagaId, orderId, VALIDATE_CREDIT)`.
6. Publish `ReserveCreditSagaCommand(sagaId, creditReservationId, orderId, customerId)`.
7. Return `HandleStockReservedResult(sagaId, creditReservationId)`.

### `HandleCreditApprovedService`

1. Parse `SagaId`; load saga or throw `IllegalArgumentException`.
2. Call `saga.creditApproved(creditReservationId)` — records ID.
3. Chain `.complete()` — transitions to `COMPLETED`, step `COMPLETE_ORDER`.
4. Persist completed saga.
5. Publish `SagaCompletedDomainEvent(sagaId, orderId)`.
6. Publish `ConfirmOrderSagaCommand(sagaId, orderId)`.
7. Return `HandleCreditApprovedResult(sagaId)`.

---

## Test Coverage

| Test class | Count | Scope |
|-----------|-------|-------|
| `SagaTest` | 27 | Domain aggregate (inventoryReserved, creditApproved, complete, advance, compensate) |
| `HandleOrderCreatedServiceTest` | 11 | Handler: persistence, event, command, unique IDs, validation |
| `HandleStockReservedServiceTest` | 11 | Handler: step advance, reservation ID storage, command, not-found, validation |
| `HandleCreditApprovedServiceTest` | 10 | Handler: COMPLETED status, both IDs preserved, event, command, not-found, validation |
| _(existing)_ | 43 | StartSaga, AdvanceSaga, CompensateSaga services |
| **Total** | **92** | **All pass, no infrastructure required** |

---

## Open Questions

1. **Idempotency on OrderCreated**: `HandleOrderCreatedService` does not check for a pre-existing
   saga with the same `sagaId`. A Kafka consumer retry would overwrite the first saga instance.
   Add an idempotency check at the consumer layer when Kafka consumers are wired.

2. **`stockReservationId` correlation**: The orchestrator generates `stockReservationId` and
   includes it in the `ReserveStockSagaCommand`. The inventory-service echoes it back in the
   `StockReserved` event. The `HandleStockReservedCommand.stockReservationId` carries this value —
   but there is currently no assertion that the echoed ID matches the one originally sent.
   The consumer layer should validate this when Kafka wiring is introduced.

3. **Transactionality**: Application services will become `@Transactional` when JPA persistence
   is introduced. DB + Kafka consistency will be handled by the Outbox pattern (ADR-0005).

4. **ADR-0008 — W3C Trace Context**: `sagaId` is a business identifier, not a trace ID.
   When Kafka consumers are wired, `traceparent` must be propagated in Kafka headers, not in
   the payload. `sagaId` in the payload is sufficient for business correlation.
