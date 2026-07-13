# orchestrator-service

The saga coordinator. Drives the UC-01 Corporate Bulk Order saga from start to terminal state.

## Responsibility

- Listens for `OrderPlacedEvent` to start a new saga instance (Kafka consumer — deferred).
- Orchestrates the sequence after the buyer has completed pre-saga availability negotiation: inventory reservation → credit reservation → order confirmation.
- Manages saga state machine transitions.
- Issues compensation commands when a step fails or is rejected (ARB-016).
- Notifies `order-service` of the final outcome (`CONFIRMED`, `PARTIALLY_CONFIRMED`, `CANCELLED`).

## Saga State Machine (UC-01)

```
STARTED
  └─► WAITING_FOR_INVENTORY
        ├─► (StockReserved)   ──► WAITING_FOR_CREDIT
        │                            ├─► (CreditApproved) ────────────────────► COMPLETED
        │                            ├─► (CreditRejected) [compensate]
        │                            │         └─► COMPENSATING ──► CANCELLED
        │                            │                          └──► FAILED_COMPENSATION
        │                            └─► (credit timeout exhausted) [compensate]
        │                                      └─► COMPENSATING ──► CANCELLED
        │                                                        └──► FAILED_COMPENSATION
        ├─► (StockRejected)   ──────────────────────────────────────► CANCELLED
        └─► (inventory timeout exhausted) [compensate, idempotent release]
                  └─► COMPENSATING ──► CANCELLED
                                   └──► FAILED_COMPENSATION

Timeout retry: saga stays in WAITING_FOR_INVENTORY / WAITING_FOR_CREDIT
               while attempts remain (no state change).
Timeout exhaust: saga → COMPENSATING → existing compensation path.
FAILED_COMPENSATION is the terminal state requiring human intervention.
```

## Domain Model

Pure-Java, zero-framework types in `com.arbitrier.orchestrator.domain.model` and `domain.command`:

| Type | Kind |
|------|------|
| `SagaId` | record — unique saga instance identifier |
| `SagaStatus` | enum — STARTED, WAITING_FOR_INVENTORY, WAITING_FOR_CREDIT, AWAITING_CUSTOMER_DECISION, COMPENSATING, COMPLETED, CANCELLED, FAILED_COMPENSATION |
| `SagaStep` | enum — ORDER_CREATED, RESERVE_INVENTORY, VALIDATE_CREDIT, AWAIT_CUSTOMER_DECISION, COMPLETE_ORDER, COMPENSATE_INVENTORY, COMPENSATE_CREDIT |
| `CustomerDecision` | enum — ACCEPT_PARTIAL, WAIT_BACKORDER, CANCEL_ORDER |
| `CompensationAction` | enum — RELEASE_INVENTORY_RESERVATION, RELEASE_CREDIT_RESERVATION, NONE |
| `SagaOrderLine` | record — `(sku, quantity)` order line carried in saga commands (ARB-017B) |
| `RetryDecision` | enum — RETRY, EXHAUST; `shouldRetry()` (ARB-018) |
| `RetryContext` | record — `(attemptNumber, maxAttempts)`; `evaluate()` (ARB-018) |
| `CorporateBulkOrderSagaRetryPolicy` | record — `(inventoryMaxAttempts, creditMaxAttempts)`; per-step retry evaluation; no duration fields (ARB-018) |
| `Saga` | final class — aggregate root with semantic lifecycle transitions |

### Saga semantic transitions (ARB-015 / ARB-018)

| Method | Effect |
|--------|--------|
| `Saga.start(id, orderId, customerId)` | Initial saga — status STARTED, step ORDER_CREATED |
| `saga.awaitInventoryResponse()` | STARTED → WAITING_FOR_INVENTORY, step RESERVE_INVENTORY |
| `saga.inventoryReserved(stockReservationId)` | Stores ID, advances step to VALIDATE_CREDIT |
| `saga.awaitCreditResponse()` | WAITING_FOR_INVENTORY → WAITING_FOR_CREDIT |
| `saga.creditApproved(creditReservationId)` | Stores ID; chain `.complete()` to finish |
| `saga.complete()` | Terminal — status COMPLETED, step COMPLETE_ORDER |
| `saga.advance(nextStep)` | General step transition (used by AdvanceSagaService) |
| `saga.compensate()` | Transitions to COMPENSATING status |
| `saga.inventoryTimedOut()` | Validates WAITING_FOR_INVENTORY; returns `this` |
| `saga.creditTimedOut()` | Validates WAITING_FOR_CREDIT; returns `this` |
| `saga.retryInventory()` | Validates WAITING_FOR_INVENTORY; returns `this` (retry intent) |
| `saga.retryCredit()` | Validates WAITING_FOR_CREDIT; returns `this` (retry intent) |
| `saga.compensate()` | Transitions to COMPENSATING (also used for timeout exhaustion) |

## Application Slice

### Inbound ports

| Interface | Purpose |
|-----------|---------|
| `HandleOrderCreatedUseCase` | Start saga; issue ReserveStock command |
| `HandleStockReservedUseCase` | Record stock reservation; issue ReserveCredit command |
| `HandleCreditApprovedUseCase` | Record credit approval; complete saga; issue ConfirmOrder command |
| `HandleInventoryTimeoutUseCase` | Handle inventory step timeout; evaluate retry policy (ARB-018) |
| `HandleCreditTimeoutUseCase` | Handle credit step timeout; evaluate retry policy (ARB-018) |
| `StartSagaUseCase` | Low-level saga creation (ARB-014) |
| `AdvanceSagaUseCase` | General step transition (ARB-014) |
| `CompensateSagaUseCase` | Begin compensation (ARB-014) |

### Outbound ports

| Interface | Purpose |
|-----------|---------|
| `SagaRepository` | Persists and loads `Saga` aggregates |
| `SagaEventPublisher` | Publishes saga lifecycle events (started, advanced, compensated, completed) |
| `ReserveStockCommandPublisher` | Issues reserve-stock command to inventory-service |
| `ReserveCreditCommandPublisher` | Issues reserve-credit command to credit-service |
| `ConfirmOrderCommandPublisher` | Issues confirm-order command to order-service |

### Domain events

| Event | When |
|-------|------|
| `SagaStartedDomainEvent` | New saga created |
| `SagaAdvancedDomainEvent` | Saga step updated |
| `SagaCompensatedDomainEvent` | Saga enters COMPENSATING status |
| `SagaCompletedDomainEvent` | Saga reaches COMPLETED terminal state |
| `InventoryTimedOutDomainEvent` | Inventory step timed out; retry scheduled |
| `CreditTimedOutDomainEvent` | Credit step timed out; retry scheduled |
| `SagaCompensatedDomainEvent` | Attempt exhaustion or rejection enters compensation |

### Happy-path workflow (ARB-015 / ARB-018)

```
OrderCreated
  → Saga.start().awaitInventoryResponse()   status=WAITING_FOR_INVENTORY, step=RESERVE_INVENTORY
  → save saga
  → SagaStartedDomainEvent
  → ReserveStockSagaCommand

StockReserved
  → load saga
  → saga.inventoryReserved().awaitCreditResponse()
                                            step=VALIDATE_CREDIT, status=WAITING_FOR_CREDIT
  → save saga
  → SagaAdvancedDomainEvent
  → ReserveCreditSagaCommand

CreditApproved
  → load saga
  → saga.creditApproved().complete()        status=COMPLETED
  → save saga
  → SagaCompletedDomainEvent
  → ConfirmOrderSagaCommand
```

### Persistence and transactions (ARB-019)

`JpaSagaRepositoryAdapter` maps the aggregate to explicit `SagaEntity` columns; it does not serialize workflow blobs. `SagaEntity` uses `@Version` optimistic locking. Every mutating application service owns its transaction boundary, keeping load, transition, save, and outbound publication in one transaction while leaving adapter classes transaction-neutral.

### Test adapters (test tree)

| Class | Purpose |
|-------|---------|
| `InMemorySagaRepository` | HashMap-backed; keyed by `SagaId` |
| `RecordingSagaEventPublisher` | Captures saga lifecycle events |
| `RecordingReserveStockCommandPublisher` | Captures issued ReserveStock commands |
| `RecordingReserveCreditCommandPublisher` | Captures issued ReserveCredit commands |
| `RecordingConfirmOrderCommandPublisher` | Captures issued ConfirmOrder commands |

### Compensation workflow (ARB-016)

```
StockRejected
  → saga.stockRejected()      status=CANCELLED
  → SagaCancelledDomainEvent
  (no release commands — nothing was reserved)

CreditRejected (after StockReserved)
  → saga.creditRejected()     status=COMPENSATING, step=COMPENSATE_INVENTORY
  → SagaCompensatedDomainEvent
  → ReleaseStockSagaCommand   (credit not released — was never approved)

StockReleased (after CreditRejected compensation)
  → saga.inventoryReleased()  status=CANCELLED
  → SagaCancelledDomainEvent

CompensationFailed
  → saga.failCompensation()   status=FAILED_COMPENSATION
  → SagaCompensationFailedDomainEvent
```

## Build & Test

```bash
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/orchestrator-service
```

Tests pass without Kafka, Postgres, Schema Registry, Keycloak, or Docker.

## Status

`ARB-019` — Saga JPA persistence and optimistic locking implemented; transaction boundaries refined at application services.

`ARB-018` — Timeout & retry policy implemented (incl. FIX-001). `WAITING_FOR_INVENTORY`,
`WAITING_FOR_CREDIT` states added. Exhaustion triggers `COMPENSATING` + idempotent
`ReleaseStockSagaCommand` — existing compensation path closes to `CANCELLED` or
`FAILED_COMPENSATION`. `CorporateBulkOrderSagaRetryPolicy` models attempt limits only
(no durations). 201 tests pass.

`ARB-017B` — `SagaOrderLine` added; `HandleOrderCreatedCommand` and `ReserveStockSagaCommand`
now carry `List<SagaOrderLine>`; warehouse identifiers removed (ADR-0009). 140 tests pass.

`ARB-016` — Compensation paths implemented: `HandleStockRejected`, `HandleCreditRejected`,
`HandleStockReleased`, `HandleCompensationFailed`.

`ARB-015` — Happy-path handlers implemented: `HandleOrderCreated`, `HandleStockReserved`,
`HandleCreditApproved`. Saga stores reservation IDs.

`ARB-014` — Orchestrator foundation: `StartSagaUseCase`, `AdvanceSagaUseCase`,
`CompensateSagaUseCase`. Domain extended with `ORDER_CREATED` step, `COMPENSATING` status,
`advance()`, and `compensate()`.

`ARB-005` — Domain model implemented.
