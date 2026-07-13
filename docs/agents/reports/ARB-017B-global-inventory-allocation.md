# ARB-017B — Global Inventory Allocation

| Field  | Value       |
|--------|-------------|
| Task   | ARB-017B    |
| Status | Implemented |
| Date   | 2026-07-09  |

## Summary

Refactors inventory availability and reservation to support global, multi-warehouse allocation
while keeping warehouse identifiers entirely internal to the Inventory bounded context.

Removes `warehouseId` from all public cross-service contracts. Introduces `StockAllocation` as
an internal domain value that stores per-warehouse allocated quantities inside `StockReservationLine`.
Replaces the single-warehouse `StockAvailabilityPort` with `WarehouseAllocationPort`, whose
allocation plan drives both availability checks and reservations.

ADR-0009 governs this decision: warehouse selection is an Inventory-only concern.

---

## Design Decisions

### Single port for availability and reservation planning

`WarehouseAllocationPort.allocate(List<RequestedStockLine>)` returns an `AllocationPlan`
used by both:

- `CheckStockAvailabilityService` — reads `totalAllocated(sku)` for advisory availability
- `ReserveStockService` — reads `forSku(sku)` to build `StockReservationLine` objects with
  internal `StockAllocation` instances

The plan never over-allocates: the contract guarantees `totalAllocated(sku) <= requestedQuantity`.

### Derived `reservedQuantity` in `StockReservationLine`

`reservedQuantity()` is derived from the sum of allocations, not stored:

```java
public int reservedQuantity() {
    return allocations.stream().mapToInt(StockAllocation::quantity).sum();
}
```

This ensures a single source of truth. Factory method `StockReservationLine.unallocated(sku, qty)`
creates lines with no allocation (out-of-stock case).

### Greedy first-fit allocation in `ConfigurableWarehouseAllocationPort`

The test adapter iterates warehouses in insertion order. For each requested SKU it allocates
`min(stock, remaining)` from each warehouse until the request is satisfied or warehouses are
exhausted. This enables multi-warehouse test scenarios without infrastructure.

### `SagaOrderLine` — order lines travel with saga commands

`HandleOrderCreatedCommand` now carries `List<SagaOrderLine>` so that the orchestrator can
forward line-level data to the inventory-service via `ReserveStockSagaCommand`. The saga
aggregate itself does not store lines (it only tracks saga state).

---

## Allocation Flow

```
CheckStockAvailabilityService:
  command.lines()
    → RequestedStockLine per line
    → WarehouseAllocationPort.allocate(requestedLines)
    → plan.totalAllocated(sku) per line (advisory, capped at requested)

ReserveStockService:
  command.lines()
    → RequestedStockLine per line
    → WarehouseAllocationPort.allocate(requestedLines)
    → plan.forSku(sku) per line → List<StockAllocation>
    → new StockReservationLine(sku, requested, allocations)
    → StockReservation.fullyReserved | partiallyReserved | rejected
    → repository.save + eventPublisher.publish
```

---

## Files Changed

### inventory-service — domain model

| File | Change |
|------|--------|
| `domain/model/StockAllocation.java` | NEW — `(warehouseId, sku, quantity)` value object |
| `domain/model/StockReservationLine.java` | Updated — now `(skuCode, requestedQuantity, List<StockAllocation>)`; `reservedQuantity()` derived |
| `domain/model/StockReservation.java` | Updated — factory methods no longer take `WarehouseId` |
| `domain/event/StockReservedDomainEvent.java` | Updated — removed `WarehouseId` field |
| `domain/event/StockPartiallyReservedDomainEvent.java` | Updated — removed `WarehouseId` field |
| `domain/event/StockRejectedDomainEvent.java` | Updated — removed `WarehouseId` field |

### inventory-service — application layer

| File | Change |
|------|--------|
| `application/port/outbound/RequestedStockLine.java` | NEW — `(sku, requestedQuantity)` for `WarehouseAllocationPort` |
| `application/port/outbound/AllocationPlan.java` | NEW — wraps `List<StockAllocation>` with `forSku()` and `totalAllocated()` |
| `application/port/outbound/WarehouseAllocationPort.java` | NEW — replaces `StockAvailabilityPort` |
| `application/port/inbound/ReserveStockCommand.java` | Updated — removed `warehouseId` field |
| `application/port/inbound/CheckStockAvailabilityCommand.java` | Updated — removed `warehouseId` field |
| `application/service/ReserveStockService.java` | Updated — uses `WarehouseAllocationPort`, builds lines from `AllocationPlan` |
| `application/service/CheckStockAvailabilityService.java` | Updated — uses `WarehouseAllocationPort`, reads `totalAllocated()` |
| `config/InventoryServiceConfiguration.java` | Updated — wires `WarehouseAllocationPort` instead of `StockAvailabilityPort` |

### inventory-service — test adapters and tests

| File | Change |
|------|--------|
| `adapter/outbound/ConfigurableWarehouseAllocationPort.java` | NEW — per-warehouse per-SKU configurable; greedy first-fit |
| `integration/InventoryServiceTestConfiguration.java` | Updated — uses `ConfigurableWarehouseAllocationPort` |
| `domain/StockReservationTest.java` | Updated — no `WarehouseId` in factory calls; `StockAllocation` in line construction |
| `application/service/ReserveStockServiceTest.java` | Updated — multi-warehouse test added; no warehouseId in command |
| `application/service/ReleaseStockServiceTest.java` | Updated — reservation helpers use `StockAllocation` |
| `application/service/CheckStockAvailabilityServiceTest.java` | Updated — multi-warehouse test added; no warehouseId in command |

### order-service

| File | Change |
|------|--------|
| `application/port/inbound/PrepareCorporateBulkOrderCommand.java` | Updated — removed `warehouseId` field |
| `application/port/outbound/InventoryAvailabilityPort.java` | Updated — `checkAvailability(lines)` no longer takes `warehouseId` |
| `application/service/PrepareCorporateBulkOrderService.java` | Updated — removed warehouseId from port call |
| `adapter/outbound/StubInventoryAvailabilityPort.java` | Updated — method signature matches new port |
| `application/service/PrepareCorporateBulkOrderServiceTest.java` | Updated — removed warehouseId from command and validation test |

### orchestrator-service

| File | Change |
|------|--------|
| `domain/command/SagaOrderLine.java` | NEW — `(sku, quantity)` value object with validation |
| `application/port/inbound/HandleOrderCreatedCommand.java` | Updated — added `List<SagaOrderLine> lines` |
| `application/port/outbound/ReserveStockSagaCommand.java` | Updated — added `List<SagaOrderLine> lines` |
| `application/service/HandleOrderCreatedService.java` | Updated — passes `command.lines()` to `ReserveStockSagaCommand` |
| `application/service/HandleOrderCreatedServiceTest.java` | Updated — command includes lines; asserts lines forwarded to stock command |

---

## Test Coverage

| Module | Tests | Delta |
|--------|-------|-------|
| inventory-service | 57 | +3 (multi-warehouse, `StockAllocation` model, `unallocated` factory) |
| order-service | 71 | -1 (removed `blank_warehouse_id_throws`) |
| orchestrator-service | 140 total across all orchestrator tests | +1 (`empty_lines_throws`) |

All tests pass. No infrastructure required.

---

## Open Questions

1. **`WarehouseAllocationPort` production implementation**: The test adapter uses greedy first-fit.
   A real implementation will query a database and may use a more sophisticated allocation
   policy (priority, geographic proximity, cost). Algorithm details are OPEN QUESTION.

2. **JPA persistence for `StockAllocation`**: The one-to-many relationship between
   `StockReservationLine` and `StockAllocation` will require a join table or embedded
   collection mapping when JPA is introduced. Schema design is OPEN QUESTION.

3. **`StockAvailabilityPort`**: The old single-warehouse port interface remains in the codebase
   (with its `ConfigurableStockAvailabilityPort` test adapter) but is no longer wired or used.
   It can be deleted in a cleanup task once confirmed no other reference remains.
