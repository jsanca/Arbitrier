# inventory-service

Manages stock levels and processes inventory reservation requests from the saga.

## Responsibility

- Receives stock reservation and release commands (from orchestrator-service via Kafka — deferred).
- Attempts to reserve the requested quantities per warehouse.
- Publishes domain events: `StockReserved`, `StockPartiallyReserved`, `StockRejected`, `StockReleased`.
- Executes compensation (stock release) idempotently when the saga is rolled back.

## Domain Model (ARB-005 / ARB-017B)

Pure-Java, zero-framework types in `com.arbitrier.inventory.domain.model`:

| Type | Kind |
|------|------|
| `StockReservationId` | record — unique reservation identifier |
| `WarehouseId` | record — warehouse identifier (internal to Inventory — ADR-0009) |
| `StockReservationStatus` | enum — RESERVED, PARTIALLY_RESERVED, REJECTED, RELEASED |
| `StockAllocation` | record — `(warehouseId, sku, quantity)` internal warehouse allocation |
| `StockReservationLine` | record — per-SKU outcome; `reservedQuantity()` derived from allocations |
| `StockReservation` | final class — aggregate root; factory methods no longer expose `WarehouseId`; idempotent `release()` |

## Application Slice (ARB-012 / ARB-017)

### Inbound ports

| Interface | Location | Notes |
|-----------|----------|-------|
| `ReserveStockUseCase` | `application/port/inbound/` | Mutating — reserves stock, publishes event |
| `ReleaseStockUseCase` | `application/port/inbound/` | Mutating — releases stock, publishes event |
| `CheckStockAvailabilityUseCase` | `application/port/inbound/` | Read-only — no reservation, no event |

### Outbound ports

| Interface | Location | Status |
|-----------|----------|--------|
| `StockReservationRepository` | `application/port/outbound/` | JPA production adapter; in-memory test adapter |
| `WarehouseAllocationPort` | `application/port/outbound/` | Configurable test adapter; replaces `StockAvailabilityPort` (ADR-0009) |
| `StockReservationEventPublisher` | `application/port/outbound/` | Recording only (no Kafka yet) |

### Domain events

| Event | When |
|-------|------|
| `StockReservedDomainEvent` | All requested lines fully reserved |
| `StockPartiallyReservedDomainEvent` | Some lines reserved, not all |
| `StockRejectedDomainEvent` | No lines could be reserved |
| `StockReleasedDomainEvent` | RESERVED or PARTIALLY_RESERVED reservation released |

### Reservation outcomes

For each line, `WarehouseAllocationPort.allocate()` returns an `AllocationPlan`. The reserved
quantity for a line is the sum of its `StockAllocation` quantities (may span multiple warehouses):

| All lines full? | Any line > 0? | Outcome |
|-----------------|---------------|---------|
| Yes | Yes | `RESERVED` |
| No | Yes | `PARTIALLY_RESERVED` |
| No | No | `REJECTED` |

### Release idempotency

`ReleaseStockUseCase` is idempotent. Re-releasing an already-RELEASED reservation is a no-op:
the same result is returned without persisting or publishing an event.

Releasing a REJECTED reservation is also a no-op (no stock was ever held — no event emitted).
See `ReleaseStockService` Javadoc for the documented decision.

### Persistence (ARB-019)

`JpaStockReservationRepositoryAdapter` persists the full reservation → lines → allocations graph through separate JPA entities. Warehouse identifiers remain inside this bounded context. The aggregate root uses `@Version`; mutating application services own transactions so load, immutable transition, save, and event publication share one boundary.

### Test adapters (test tree)

| Class | Purpose |
|-------|---------|
| `InMemoryStockReservationRepository` | HashMap-backed; no DB |
| `ConfigurableWarehouseAllocationPort` | Per-warehouse per-SKU configurable; greedy first-fit allocation |
| `RecordingStockReservationEventPublisher` | Captures events for assertion |

## Build & Test

```bash
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/inventory-service
```

Tests pass without Kafka, Postgres, Schema Registry, Keycloak, or Docker.

## Status

`ARB-017B` — Global multi-warehouse allocation: `WarehouseAllocationPort` replaces `StockAvailabilityPort`; `StockAllocation` added; `warehouseId` removed from all public contracts (ADR-0009). 57 tests pass.
`ARB-017` — `CheckStockAvailabilityUseCase` added: read-only pre-saga availability check. No persistence, no events.
`ARB-019` — JPA persistence for reservations, lines, and multi-warehouse allocations implemented with optimistic locking.
`ARB-012` — Application slice implemented: `ReserveStockUseCase`, `ReleaseStockUseCase`, domain events, test adapters. Kafka and REST adapters remain pending.
`ARB-005` — Domain model implemented.
