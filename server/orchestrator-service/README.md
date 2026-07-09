# orchestrator-service

The saga coordinator. Drives the UC-01 Corporate Bulk Order saga from start to terminal state.

## Responsibility

- Listens for `OrderPlacedEvent` to start a new saga instance (Kafka consumer — deferred).
- Orchestrates the sequence: inventory reservation → credit reservation → (optional) human decision.
- Manages saga state machine transitions.
- Issues compensation commands when a step fails or is rejected (ARB-016).
- Notifies `order-service` of the final outcome (`CONFIRMED`, `PARTIALLY_CONFIRMED`, `CANCELLED`).

## Saga State Machine (UC-01)

```
STARTED
  └─► CREDIT_RESERVATION_REQUESTED
        ├─► CREDIT_RESERVED
        │     └─► INVENTORY_RESERVATION_REQUESTED
        │           ├─► INVENTORY_FULLY_RESERVED ──────────────────► CONFIRMED
        │           ├─► INVENTORY_PARTIALLY_RESERVED
        │           │     └─► AWAITING_CUSTOMER_DECISION
        │           │           ├─► (customer accepts)  ────────────► PARTIALLY_CONFIRMED
        │           │           └─► (customer rejects) [compensate]─► CANCELLED
        │           └─► INVENTORY_RESERVATION_FAILED
        │                 └─► (compensate credit) ─────────────────► CANCELLED
        └─► CREDIT_RESERVATION_DENIED ───────────────────────────────► CANCELLED
```

## Domain Model

Pure-Java, zero-framework types in `com.arbitrier.orchestrator.domain.model`:

| Type | Kind |
|------|------|
| `SagaId` | record — unique saga instance identifier |
| `SagaStatus` | enum — STARTED, AWAITING_CUSTOMER_DECISION, COMPENSATING, COMPLETED, CANCELLED, FAILED_COMPENSATION |
| `SagaStep` | enum — ORDER_CREATED, RESERVE_INVENTORY, VALIDATE_CREDIT, AWAIT_CUSTOMER_DECISION, COMPLETE_ORDER, COMPENSATE_INVENTORY, COMPENSATE_CREDIT |
| `CustomerDecision` | enum — ACCEPT_PARTIAL, WAIT_BACKORDER, CANCEL_ORDER |
| `CompensationAction` | enum — RELEASE_INVENTORY_RESERVATION, RELEASE_CREDIT_RESERVATION, NONE |
| `Saga` | final class — aggregate root with semantic lifecycle transitions |

### Saga semantic transitions (ARB-015)

| Method | Effect |
|--------|--------|
| `Saga.start(id, orderId, customerId)` | Initial saga — status STARTED, step ORDER_CREATED |
| `saga.inventoryReserved(stockReservationId)` | Stores ID, advances step to VALIDATE_CREDIT |
| `saga.creditApproved(creditReservationId)` | Stores ID; chain `.complete()` to finish |
| `saga.complete()` | Terminal — status COMPLETED, step COMPLETE_ORDER |
| `saga.advance(nextStep)` | General step transition (used by AdvanceSagaService) |
| `saga.compensate()` | Transitions to COMPENSATING status |

## Application Slice

### Inbound ports

| Interface | Purpose |
|-----------|---------|
| `HandleOrderCreatedUseCase` | Start saga; issue ReserveStock command |
| `HandleStockReservedUseCase` | Record stock reservation; issue ReserveCredit command |
| `HandleCreditApprovedUseCase` | Record credit approval; complete saga; issue ConfirmOrder command |
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

### Happy-path workflow (ARB-015)

```
OrderCreated
  → Saga.start()           status=STARTED, step=ORDER_CREATED
  → save saga
  → SagaStartedDomainEvent
  → ReserveStockSagaCommand

StockReserved
  → load saga
  → saga.inventoryReserved()  step=VALIDATE_CREDIT, stockReservationId stored
  → save saga
  → SagaAdvancedDomainEvent
  → ReserveCreditSagaCommand

CreditApproved
  → load saga
  → saga.creditApproved().complete()  status=COMPLETED, creditReservationId stored
  → save saga
  → SagaCompletedDomainEvent
  → ConfirmOrderSagaCommand
```

### Test adapters (test tree)

| Class | Purpose |
|-------|---------|
| `InMemorySagaRepository` | HashMap-backed; keyed by `SagaId` |
| `RecordingSagaEventPublisher` | Captures saga lifecycle events |
| `RecordingReserveStockCommandPublisher` | Captures issued ReserveStock commands |
| `RecordingReserveCreditCommandPublisher` | Captures issued ReserveCredit commands |
| `RecordingConfirmOrderCommandPublisher` | Captures issued ConfirmOrder commands |

## Build & Test

```bash
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/orchestrator-service
```

Tests pass without Kafka, Postgres, Schema Registry, Keycloak, or Docker.

## Status

`ARB-015` — Happy-path handlers implemented: `HandleOrderCreated`, `HandleStockReserved`,
`HandleCreditApproved`. Saga stores reservation IDs. 92 tests pass.

`ARB-014` — Orchestrator foundation: `StartSagaUseCase`, `AdvanceSagaUseCase`,
`CompensateSagaUseCase`. Domain extended with `ORDER_CREATED` step, `COMPENSATING` status,
`advance()`, and `compensate()`.

`ARB-005` — Domain model implemented.
