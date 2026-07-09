# ARB-012 — Inventory Service Application Slice

| Field  | Value       |
|--------|-------------|
| Task   | ARB-012     |
| Status | Implemented |
| Date   | 2026-07-08  |

## Summary

First application slice for inventory-service: `ReserveStockUseCase` and `ReleaseStockUseCase`
with full domain event coverage and idempotent release. No JPA, Kafka, Avro, REST, or Docker
required to run tests.

---

## Files Created

### Domain events

| File | Purpose |
|------|---------|
| `domain/event/StockReservedDomainEvent.java` | All lines fully reserved. |
| `domain/event/StockPartiallyReservedDomainEvent.java` | At least one line reserved, at least one not. |
| `domain/event/StockRejectedDomainEvent.java` | No lines could be reserved. |
| `domain/event/StockReleasedDomainEvent.java` | Reserved stock returned (RESERVED or PARTIALLY_RESERVED → RELEASED only). |

### Application — inbound ports

| File | Purpose |
|------|---------|
| `application/port/inbound/ReserveStockUseCase.java` | Input port for reservation. |
| `application/port/inbound/ReleaseStockUseCase.java` | Input port for release (idempotent). |
| `application/port/inbound/ReserveStockCommand.java` | Command: orderId, reservationId, warehouseId, lines. |
| `application/port/inbound/ReserveStockLineCommand.java` | Per-line: sku, quantity (validated positive). |
| `application/port/inbound/ReleaseStockCommand.java` | Command: reservationId. |
| `application/port/inbound/ReserveStockResult.java` | Result: reservationId + outcome (RESERVED / PARTIALLY_RESERVED / REJECTED). |
| `application/port/inbound/ReleaseStockResult.java` | Result: reservationId (always implies RELEASED). |

### Application — outbound ports

| File | Purpose |
|------|---------|
| `application/port/outbound/StockAvailabilityPort.java` | Queries available quantity per SKU per warehouse. |
| `application/port/outbound/StockReservationRepository.java` | Persists and loads `StockReservation` aggregates. |
| `application/port/outbound/StockReservationEventPublisher.java` | Publishes all four domain events. |

### Application services

| File | Purpose |
|------|---------|
| `application/service/ReserveStockService.java` | Implements `ReserveStockUseCase`. |
| `application/service/ReleaseStockService.java` | Implements `ReleaseStockUseCase`. |

### Configuration

| File | Purpose |
|------|---------|
| `config/InventoryServiceConfiguration.java` | Spring wiring: `ReserveStockService` and `ReleaseStockService` beans. |

### Test adapters

| File | Purpose |
|------|---------|
| `adapter/outbound/InMemoryStockReservationRepository.java` | HashMap-backed repository for tests. |
| `adapter/outbound/ConfigurableStockAvailabilityPort.java` | Per-SKU configurable availability (defaults to 0). |
| `adapter/outbound/RecordingStockReservationEventPublisher.java` | Captures published events for assertion. |

### Test configuration and IT

| File | Change |
|------|--------|
| `integration/InventoryServiceTestConfiguration.java` | New — wires in-memory adapters for context load tests. |
| `integration/InventoryServiceApplicationIT.java` | Added `@Import(InventoryServiceTestConfiguration.class)`. |
| `unit/ArchitectureTest.java` | Added two rules: domain and application must not depend on Avro/Kafka. |

---

## Reservation Logic

**Per-line allocation**:
```
reservedQty = min(requestedQty, stockAvailabilityPort.availableQuantity(warehouse, sku))
```

**Outcome decision**:
| Condition | Outcome | Domain factory |
|-----------|---------|---------------|
| All lines: `reserved == requested` | RESERVED | `StockReservation.fullyReserved()` |
| At least one line: `reserved > 0`; not all full | PARTIALLY_RESERVED | `StockReservation.partiallyReserved()` |
| All lines: `reserved == 0` | REJECTED | `StockReservation.rejected()` |

There is no multi-warehouse split or warehouse optimisation in this slice.

---

## Release Decision for REJECTED Reservations

**Decision**: Releasing a REJECTED reservation is a **no-op** — no state change is persisted
and no `StockReleasedDomainEvent` is published.

**Rationale**: A REJECTED reservation never held any stock (every line's `reservedQuantity == 0`).
Publishing a `StockReleased` event for stock that was never held would be misleading to the
orchestrator and any downstream consumers.

OPEN QUESTION: Verify with orchestrator-service saga design that the compensation path does NOT
route through `StockReleased` following `StockRejected`. If the orchestrator relies on
`StockReleased` as a saga completion signal even for the REJECTED case, this decision must be
revisited in ARB-013.

---

## Idempotency

`ReleaseStockService.release()` is idempotent:
- If the reservation status is already `RELEASED`, the method returns immediately without
  persisting or publishing an event.
- The second call returns the same `ReleaseStockResult` as the first.

---

## Open Questions

1. **REJECTED release and saga compensation**: Confirm orchestrator-service does not expect
   `StockReleased` after `StockRejected` (see Release Decision above).

2. **`StockAvailabilityPort` implementation**: The port is backed by no storage yet. The JPA adapter
   will need to query a `stock_availability` table or equivalent. The reservation locking strategy
   (optimistic vs pessimistic) must be chosen before the JPA phase.

3. **Reservation not found on release**: `ReleaseStockService` throws `IllegalArgumentException`.
   Once a REST or Kafka consumer is added, this should map to a typed `InventoryProblemCode`
   (similar to `OrderProblemCode.CUSTOMER_ACCESS_DENIED` in order-service).

4. **Duplicate reserve commands**: `ReserveStockService` does not check for an existing reservation
   with the same `reservationId` before saving. If the orchestrator retries the reserve command
   idempotently, a second reservation would overwrite the first. An idempotency check (ARB-005 /
   `IdempotencyStore`) should be added at the Kafka consumer layer.

5. **Partial stock allocation strategy**: In this slice, `reservedQty = min(requested, available)`
   with no cross-warehouse optimisation. If the saga design requires a different allocation
   strategy (e.g., all-or-nothing per line), this service must be updated.
