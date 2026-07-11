# Task: ARB-017B — Global Inventory Allocation

## Status

[PLANNED]

## Owner

Clio

## Context

ARB-017 Pre-Saga Availability Negotiation is DONE.

ADR-0009 establishes that:

* warehouse selection belongs to Inventory;
* Order, Saga, UI, and external callers request SKUs and quantities;
* availability is calculated globally;
* authoritative reservation may allocate one requested line across multiple warehouses;
* warehouse identifiers must not leak through public cross-service contracts.

The current inventory model assumes one `WarehouseId` per `StockReservation`. This task evolves the logical model before production adapters are implemented.

## Goal

Refactor Inventory availability and reservation logic to support global, multi-warehouse allocation while keeping warehouse details internal to the Inventory bounded context.

## Scope

Primary module:

* `server/inventory-service`

Allowed supporting changes:

* `server/order-service`
* `server/orchestrator-service`
* documentation and contracts affected by removal of public `warehouseId`

No production infrastructure adapters.

## In scope

### 1. Remove warehouse selection from public application contracts

Remove `warehouseId` from:

* `PrepareCorporateBulkOrderCommand`;
* `InventoryAvailabilityPort.checkAvailability(...)`;
* `CheckStockAvailabilityCommand`;
* order-service availability query records;
* `ReserveStockSagaCommand`;
* inventory-service `ReserveStockCommand`;
* any corresponding results, handlers, tests, or documentation where it is exposed as caller input.

The external application-level API should conceptually be:

```java
checkAvailability(lines)
reserveStock(reservationId, orderId, lines)
```

### 2. Introduce internal warehouse allocation model

Create a pure domain model representing internal allocation.

Suggested concepts:

```java
StockAllocation(
    WarehouseId warehouseId,
    String sku,
    int quantity
)
```

or an equivalent strongly typed design.

A reservation line should be able to contain allocations from multiple warehouses.

Example:

```text
SKU-LAPTOP
requested: 100
reserved: 100

allocations:
- Guanacaste: 10
- San José: 90
```

Keep the exact shape small and domain-oriented.

### 3. Introduce allocation port or policy

Add an outbound application port such as:

```java
StockAllocationPort
```

or:

```java
WarehouseAllocationPort
```

It should answer how much stock can be allocated globally for requested lines.

Possible conceptual contract:

```java
AllocationPlan plan(List<RequestedStockLine> lines);
```

The application and domain must not know whether allocation data comes from:

* PostgreSQL;
* Redis;
* an external warehouse system;
* a future optimization engine.

Use a configurable in-memory test adapter.

### 4. Global availability query

Refactor `CheckStockAvailabilityService` so it:

* accepts requested lines only;
* queries global availability through the allocation/availability port;
* returns business-level quantities;
* does not expose warehouse allocations to Order;
* performs no reservation;
* persists nothing;
* publishes no event.

The public result remains:

* SKU;
* requested quantity;
* available quantity;
* backorder quantity;
* fully available.

### 5. Authoritative reservation

Refactor `ReserveStockService` so it:

* receives requested lines without `warehouseId`;
* obtains an authoritative allocation plan;
* creates a reservation containing internal warehouse allocations;
* derives `RESERVED`, `PARTIALLY_RESERVED`, or `REJECTED` from the resulting allocation;
* persists through the existing repository port;
* publishes the corresponding pure domain event;
* returns the reservation outcome.

The service should continue reading as a business pipeline:

```text
allocate globally
    ↓
create reservation
    ↓
save
    ↓
publish
    ↓
return
```

### 6. Release behavior

Update release behavior so an accepted reservation releases all internal allocations.

The public release command should continue requiring only:

* reservation ID.

Inventory loads the reservation and knows which warehouses and quantities must be released.

Release must remain idempotent.

### 7. Domain events

Review existing inventory domain events.

Public integration-facing domain events should not require consumers to understand warehouse allocation unless necessary.

Prefer business-level event data:

* reservation ID;
* order ID;
* requested/reserved lines;
* outcome.

Warehouse allocation details may remain internal or be included only in an Inventory-owned internal event if needed later.

Do not add Avro or Kafka mapping in this task.

### 8. Order-service changes

Refactor pre-saga preparation so:

```java
PrepareCorporateBulkOrderCommand
```

does not contain `warehouseId`.

`InventoryAvailabilityPort` should query global availability by lines.

Order remains unaware of warehouses.

### 9. Orchestrator changes

Refactor reserve-stock saga commands so the orchestrator publishes:

* saga ID;
* reservation ID;
* order ID;
* requested lines;

without a warehouse identifier.

The orchestrator must remain unaware of allocation details.

Do not change saga state behavior beyond the required contract cleanup.

## Out of scope

* No JPA.
* No PostgreSQL repositories.
* No Flyway migrations.
* No Kafka consumers or producers.
* No Avro mapper changes beyond documentation of future impact.
* No allocation optimization algorithm.
* No shipping-cost calculations.
* No geographic routing.
* No cross-country regulatory rules.
* No UI.
* No REST or gRPC adapter.
* No distributed locking.
* No stock decrement persistence.
* No ARB-018 work.

## Architecture rules

* Warehouse selection is an Inventory concern.
* Order and Saga must not depend on `WarehouseId`.
* Domain remains pure Java.
* Application depends on ports and domain only.
* Warehouse allocation details must not leak into unrelated bounded contexts.
* Value objects should remain strongly typed internally.
* Application services should read as business stories.
* Avoid duplicated derived status.
* Reservation status must derive from the aggregate/allocation result.

## Tests

### Inventory availability

* global full availability across one warehouse;
* global full availability split across multiple warehouses;
* partial global availability;
* no global availability;
* available quantity capped at requested quantity;
* backorder quantity correct;
* public result contains no warehouse identifier;
* no mutation, save, or event.

### Inventory reservation

* full reservation from one warehouse;
* full reservation split across multiple warehouses;
* partial reservation across warehouses;
* rejected reservation;
* aggregate stores internal allocations;
* total reserved quantity equals sum of allocations;
* no allocation exceeds available quantity;
* correct domain event published;
* application result contains correct status.

### Release

* release reservation with one allocation;
* release reservation with several allocations;
* release remains idempotent;
* rejected reservation release remains a no-op;
* no duplicate release event.

### Order service

* preparation requires no warehouse identifier;
* global availability port is called with requested lines;
* full/partial/none recommendation behavior remains unchanged.

### Orchestrator

* reserve-stock command contains no warehouse identifier;
* happy-path and compensation tests remain green;
* no saga state behavior changes.

## Documentation

Create:

* `docs/implementation/ARB-017B-global-inventory-allocation.md`

Create or update:

* `docs/adr/ADR-0009-global-inventory-allocation-ownership.md`
* `docs/implementation/ARB-017-pre-saga-availability-negotiation.md`
* `server/inventory-service/README.md`
* `server/order-service/README.md`
* `server/orchestrator-service/README.md`
* relevant RF, OKF, and test-case documents.

Mark the previous `warehouseId propagation` open question as resolved by ADR-0009:

> warehouseId is not propagated because warehouse selection is owned internally by Inventory.

## Acceptance Criteria

* Inventory availability works globally without public `warehouseId`.
* Inventory reservation supports multiple warehouse allocations.
* Order does not know warehouse identifiers.
* Saga does not know warehouse identifiers.
* Release works from stored reservation allocations.
* Existing full, partial, rejection, compensation, and pre-saga tests remain green.
* New multi-warehouse tests pass.
* No JPA, Kafka, Avro runtime, REST, gRPC, or infrastructure introduced.
* Documentation reflects ADR-0009.
* ARB-017B is ready for Deep review.

## After completion

Report:

* created and modified files;
* domain model changes;
* public contract changes;
* test totals;
* remaining open questions.

Do not start the next roadmap task.
