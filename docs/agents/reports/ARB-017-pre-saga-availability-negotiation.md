# ARB-017 — Pre-Saga Availability Negotiation

| Field  | Value       |
|--------|-------------|
| Task   | ARB-017     |
| Status | Implemented |
| Date   | 2026-07-09  |

## Summary

Implements pre-saga inventory availability negotiation. A corporate buyer can check stock
availability before committing to an order — the system returns available vs backorder
quantities and a recommended action. The buyer makes an explicit decision before the saga
is started. No saga state, no Order aggregate, and no Kafka events are involved.

---

## Design Decision

**Human decision happens before the saga, not inside it.**

The saga is a distributed execution engine, not a human waiting room. Introducing an
`AWAITING_CUSTOMER_DECISION` state inside the saga (the original ARB-017 plan) would:
- Freeze a running saga indefinitely while waiting for a human
- Complicate compensation: a partially-reserved saga waiting for a human cannot cleanly roll back
- Mix synchronous UX concerns with asynchronous distributed concerns

The new approach separates the concerns:

| Concern | Where it lives |
|---------|----------------|
| Availability check | `PrepareCorporateBulkOrderUseCase` (order-service) |
| Buyer decision | Pre-saga, recorded by the caller before `SubmitCorporateBulkOrderUseCase` |
| Reservation | Inside the saga (authoritative) |
| Compensation | Existing ARB-016 paths — unchanged |

Key guarantees:
- **Pre-check is advisory and non-binding.** Stock levels may change between the check and
  the actual reservation attempt inside the saga.
- **Reservation inside the saga remains authoritative.** If the reservation later fails
  because stock changed, existing saga failure and compensation paths handle the rollback.
- **Race conditions are handled by saga failure/compensation, not by the pre-check.**

---

## Availability Check Flow

```
Buyer submits intended lines
  → PrepareCorporateBulkOrderUseCase
      → InventoryAvailabilityPort.checkAvailability()  (advisory query, no reservation)
      → compute per-line: availableQuantity, backorderQuantity, fullyAvailable
      → determine recommendedAction:
          PROCEED_FULL              — all lines fully available
          ASK_CUSTOMER_ACCEPT_PARTIAL — some stock available, not all
          REJECT_NO_STOCK           — no stock for any line
      → return PrepareCorporateBulkOrderResult (no Order created, no saga started)

Buyer decides: ACCEPT_FULL | ACCEPT_PARTIAL | CANCEL
  → if ACCEPT_FULL or ACCEPT_PARTIAL:
      caller submits SubmitCorporateBulkOrderUseCase with:
        - ACCEPT_FULL: original requested quantities
        - ACCEPT_PARTIAL: availableLines quantities from PrepareCorporateBulkOrderResult
  → if CANCEL: no Order is created
```

---

## inventory-service: CheckStockAvailabilityUseCase

### Per-line computation

```
availableQuantity = min(warehouseStock, requestedQuantity)
backorderQuantity = max(0, requestedQuantity - warehouseStock)
fullyAvailable    = warehouseStock >= requestedQuantity
```

### Files Created

| File | Purpose |
|------|---------|
| `application/port/inbound/CheckStockAvailabilityLineCommand.java` | Single SKU check input |
| `application/port/inbound/CheckStockAvailabilityCommand.java` | Warehouse + lines check input |
| `application/port/inbound/CheckStockAvailabilityLineResult.java` | Per-SKU availability result |
| `application/port/inbound/CheckStockAvailabilityResult.java` | All-lines result container |
| `application/port/inbound/CheckStockAvailabilityUseCase.java` | Inbound port interface |
| `application/service/CheckStockAvailabilityService.java` | Read-only use-case implementation |

### Files Updated

| File | Change |
|------|--------|
| `config/InventoryServiceConfiguration.java` | Added `checkStockAvailabilityUseCase` bean |

---

## order-service: PrepareCorporateBulkOrderUseCase

### Line groupings in result

| Field | Contents |
|-------|----------|
| `requestedLines` | All lines at original requested quantities |
| `availableLines` | Subset where `availableQuantity > 0` — use these for ACCEPT_PARTIAL submission |
| `backorderLines` | Subset where `backorderQuantity > 0` — partially or fully unavailable lines |

A line may appear in both `availableLines` and `backorderLines` when it is partially available.

### Files Created

| File | Purpose |
|------|---------|
| `application/port/inbound/RecommendedAction.java` | Enum: PROCEED_FULL, ASK_CUSTOMER_ACCEPT_PARTIAL, REJECT_NO_STOCK |
| `application/port/inbound/CustomerPreSagaDecision.java` | Enum: ACCEPT_FULL, ACCEPT_PARTIAL, CANCEL |
| `application/port/inbound/PrepareCorporateBulkOrderLineCommand.java` | Single SKU command line |
| `application/port/inbound/PrepareCorporateBulkOrderCommand.java` | customerId, userId, warehouseId, lines |
| `application/port/inbound/PrepareCorporateBulkOrderLineResult.java` | Per-SKU result |
| `application/port/inbound/PrepareCorporateBulkOrderResult.java` | Full result with line groupings |
| `application/port/inbound/PrepareCorporateBulkOrderUseCase.java` | Inbound port interface |
| `application/port/outbound/AvailabilityLineQuery.java` | SKU + requestedQuantity for port call |
| `application/port/outbound/AvailabilityLineResponse.java` | SKU + availableQuantity from port |
| `application/port/outbound/InventoryAvailabilityPort.java` | Outbound port: synchronous availability query |
| `application/service/PrepareCorporateBulkOrderService.java` | Use-case implementation |

### Files Updated

| File | Change |
|------|--------|
| `config/OrderServiceConfiguration.java` | Added `prepareCorporateBulkOrderUseCase` bean |
| `integration/OrderServiceTestConfiguration.java` | Added `@Primary StubInventoryAvailabilityPort` bean |

### Test adapter

| File | Purpose |
|------|---------|
| `adapter/outbound/StubInventoryAvailabilityPort.java` | Configurable per-SKU stub; no production use |

---

## Test Coverage

| Test class | Count | Scope |
|-----------|-------|-------|
| `CheckStockAvailabilityServiceTest` | 9 | All availability outcomes, capping, backorder arithmetic, validation |
| `PrepareCorporateBulkOrderServiceTest` | 14 | PROCEED_FULL, ASK_CUSTOMER_ACCEPT_PARTIAL, REJECT_NO_STOCK, accepted partial quantities, decision enum, port call, validation |
| _(existing inventory-service)_ | 45 | Unchanged |
| _(existing order-service)_ | 58 | Unchanged |
| **Total inventory-service** | **54** | All pass |
| **Total order-service** | **72** | All pass |

---

## Open Questions

1. **`InventoryAvailabilityPort` production adapter**: The port is defined; no gRPC or HTTP
   adapter exists yet. The production implementation will delegate to `inventory-service`.
   Transport protocol (gRPC vs HTTP) is unresolved.

2. ~~**`warehouseId` source**~~ — **Resolved by ARB-017B / ADR-0009**: `warehouseId` has been
   removed from `PrepareCorporateBulkOrderCommand` and all public contracts. Warehouse selection
   is internal to the Inventory bounded context.

3. ~~**Multi-warehouse**~~ — **Resolved by ARB-017B**: `WarehouseAllocationPort` returns an
   `AllocationPlan` that spans multiple warehouses. `totalAllocated(sku)` sums across all
   warehouses for the advisory availability check.

4. **Decision persistence**: The `CustomerPreSagaDecision` enum is modeled but not persisted.
   If a buyer's session expires between the pre-check and submission, the decision is lost.
   Persistence of the decision is OPEN QUESTION.

5. ~~**`SubmitCorporateBulkOrderCommand` and warehouse**~~ — **Resolved by ARB-017B / ADR-0009**:
   `warehouseId` is no longer needed in `ReserveStockSagaCommand`. Order lines (`SagaOrderLine`)
   travel through the saga command chain; warehouse selection remains internal to Inventory.
