# ARB-005: Domain Model v1

**Status:** Implemented
**Date:** 2026-07-07

## Summary

Pure-Java domain model for the four saga-participant services. Zero Spring, JPA, Kafka, or Avro imports anywhere in `domain/model`. All types use `com.arbitrier.platform.validation.Require` for guard checks.

---

## Types Created per Service

### order-service (`com.arbitrier.order.domain.model`)

| Type | Kind | Purpose |
|------|------|---------|
| `UserId` | record | Authenticated user who submitted the order |
| `CustomerId` | record | Corporate buyer organisation |
| `OrderId` | record | Unique order identifier |
| `Sku` | record | Stock-keeping unit code |
| `Quantity` | record | Positive integer quantity within an order line |
| `Money` | record | Non-negative monetary amount with ISO-4217 currency |
| `OrderLine` | record | SKU + Quantity pair within an order |
| `OrderStatus` | enum | PENDING, AWAITING_CUSTOMER_DECISION, CONFIRMED, PARTIALLY_CONFIRMED, CANCELLED |
| `CancellationReason` | enum | CUSTOMER_CANCELLED, CUSTOMER_DEFERRED, INSUFFICIENT_CREDIT, SYSTEM_TIMEOUT |
| `Order` | final class | Aggregate root; immutable lifecycle transitions |

### inventory-service (`com.arbitrier.inventory.domain.model`)

| Type | Kind | Purpose |
|------|------|---------|
| `StockReservationId` | record | Unique stock reservation identifier |
| `WarehouseId` | record | Warehouse from which stock is reserved |
| `StockReservationStatus` | enum | RESERVED, PARTIALLY_RESERVED, REJECTED, RELEASED |
| `StockReservationLine` | record | Outcome for a single SKU line (requested vs reserved) |
| `StockReservation` | final class | Aggregate root; idempotent release |

### credit-service (`com.arbitrier.credit.domain.model`)

| Type | Kind | Purpose |
|------|------|---------|
| `CreditReservationId` | record | Unique credit reservation identifier |
| `Money` | record | Non-negative monetary amount (bounded-context copy; not shared from order-service) |
| `CreditReservationStatus` | enum | APPROVED, REJECTED, RELEASED |
| `CreditReservation` | final class | Aggregate root; release only valid for APPROVED; idempotent |

### orchestrator-service (`com.arbitrier.orchestrator.domain.model`)

| Type | Kind | Purpose |
|------|------|---------|
| `SagaId` | record | Unique saga instance identifier |
| `SagaStatus` | enum | STARTED, AWAITING_CUSTOMER_DECISION, COMPLETED, CANCELLED, FAILED_COMPENSATION |
| `SagaStep` | enum | RESERVE_INVENTORY, VALIDATE_CREDIT, AWAIT_CUSTOMER_DECISION, COMPLETE_ORDER, COMPENSATE_INVENTORY, COMPENSATE_CREDIT |
| `CustomerDecision` | enum | ACCEPT_PARTIAL, WAIT_BACKORDER, CANCEL_ORDER |
| `CompensationAction` | enum | RELEASE_INVENTORY_RESERVATION, RELEASE_CREDIT_RESERVATION, NONE |
| `Saga` | final class | Aggregate root; immutable lifecycle transitions |

---

## Core Invariants

- **Order**: at least one line required; CancellationReason required for CANCELLED; terminal states block further transitions.
- **Quantity**: must be positive (> 0).
- **Money**: amount must be zero or positive; currency must not be blank.
- **StockReservation**: `fullyReserved()` requires all lines fully reserved; `partiallyReserved()` requires mixed lines; `release()` is idempotent.
- **CreditReservation**: `release()` only valid for APPROVED; idempotent if already RELEASED; throws for REJECTED.
- **Saga**: `applyCustomerDecision()` requires AWAITING_CUSTOMER_DECISION; terminal sagas block complete/cancel/compensate calls.

---

## Unit Tests

| Test class | Location | Coverage |
|-----------|----------|----------|
| `OrderTest` | `server/order-service/src/test/java/com/arbitrier/order/domain/` | 11 test cases covering creation, transitions, guards |
| `StockReservationTest` | `server/inventory-service/src/test/java/com/arbitrier/inventory/domain/` | 8 test cases covering factory methods, idempotent release, line validation |
| `CreditReservationTest` | `server/credit-service/src/test/java/com/arbitrier/credit/domain/` | 5 test cases covering approved/rejected/release/idempotency |
| `SagaTest` | `server/orchestrator-service/src/test/java/com/arbitrier/orchestrator/domain/` | 11 test cases covering all transitions and guards |

ArchUnit tests activated in all four services (`ArchitectureTest.java`).

---

## Open Questions

- OPEN QUESTION: Should `Order` track which lines are confirmed vs backorder in `PARTIALLY_CONFIRMED` state?
- OPEN QUESTION: Does `CreditReservation` need a currency field separate from `Money`?
- OPEN QUESTION: Should `SagaStep` be finer-grained once Avro contracts are defined?
