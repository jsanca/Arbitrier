ADR-0009 — Global Inventory Allocation Ownership

Status

Accepted

Context

ARB-017 introduced a pre-saga availability negotiation flow.

The initial implementation required "warehouseId" in the inventory availability query. During review, a propagation gap was identified because "warehouseId" did not travel through:

- order submission;
- "OrderCreatedDomainEvent";
- saga startup;
- "ReserveStockSagaCommand";
- inventory reservation.

One possible solution was to propagate "warehouseId" across every boundary.

However, that would expose an internal logistics decision to Order, Saga, UI, and external callers.

For UC-01, the buyer requests product quantities. The buyer does not need to select the physical warehouse used to fulfill the order.

A request for 100 computers may be fulfilled from:

- 10 units in Guanacaste;
- 90 units in San José;

or any other valid allocation selected by Inventory.

Decision

Warehouse selection and stock-allocation planning are owned exclusively by the Inventory bounded context.

External callers express requested SKUs and quantities, not warehouse choices.

The public inventory availability contract will use:

checkAvailability(lines)

The authoritative reservation contract will likewise accept requested lines without requiring a warehouse identifier.

Inventory may fulfill a requested line from one or more warehouses.

The allocation algorithm, warehouse selection, prioritization, routing, and optimization remain internal Inventory concerns.

Architectural Model

Order / UI / Saga
|
| requested SKUs and quantities
v
InventoryAvailabilityPort
|
v
Inventory bounded context
|
| internal allocation policy
v
Warehouse Allocation Plan

The caller receives only the business-level answer needed to continue:

- requested quantity;
- available quantity;
- backorder quantity;
- full/partial/unavailable result.

Warehouse-level allocation details are not part of the public contract unless a future business requirement explicitly needs them.

Pre-check versus reservation

The pre-saga availability check remains advisory and non-binding.

checkAvailability(lines)

answers:

«What quantity appears globally available now?»

The saga reservation answers:

«What quantity was authoritatively reserved, and from which internal warehouse allocations?»

Inventory may recompute the allocation during the authoritative reservation.

If stock changes between pre-check and reservation, the existing saga failure and compensation paths handle the outcome.

Domain consequences

The current inventory model associates a reservation with a single "WarehouseId".

That model must evolve to support allocation across multiple warehouses.

A likely internal model is:

StockReservation
|
+-- StockReservationLine
|
+-- StockAllocation
warehouseId
quantity

The exact aggregate structure will be implemented in ARB-017B.

Public contract consequences

Remove "warehouseId" from:

- "PrepareCorporateBulkOrderCommand";
- "InventoryAvailabilityPort";
- "CheckStockAvailabilityCommand";
- order-side availability query DTOs;
- saga reserve-stock commands;
- inventory "ReserveStockCommand".

Do not propagate warehouse identifiers through Order or Saga merely to satisfy an internal Inventory requirement.

Consequences

Positive

- Order and Saga remain free of logistics concerns.
- Inventory owns its allocation invariants.
- Multi-warehouse fulfillment becomes possible.
- The public API remains simple and business-oriented.
- Allocation policies may evolve without changing callers.
- The client programmer does not need to understand warehouse topology.

Negative

- Inventory reservation modeling becomes more complex.
- A single reservation may contain several warehouse allocations.
- Release logic must release each allocation correctly.
- Persistence will require reservation-allocation relationships.
- Concurrency and locking must eventually operate per warehouse/SKU allocation.

Deferred concerns

The following remain outside this ADR:

- allocation optimization;
- shipping cost;
- geographic proximity;
- cross-country fulfillment rules;
- warehouse priority;
- split-shipment UX;
- reservation locking;
- JPA persistence;
- stock movement events.

Decision rule

Warehouse identifiers may cross the Inventory boundary only when required by an explicit business use case.

They must not leak merely because the current implementation stores stock by warehouse.