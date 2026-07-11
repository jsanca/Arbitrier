Task: ARB-017 — Pre-Saga Availability Negotiation

Status:
[PLANNED]

Owner:
Clio

Context:
ARB-016 Saga Compensation is DONE.

Originally ARB-017 was planned as Backorder Human Workflow inside the saga.
Design decision changed:

Do NOT freeze a running saga while waiting for human decision.

Instead:
- perform a pre-saga availability check
- return available vs unavailable quantities to the user
- let the user decide before starting the reservation saga
- run the saga only once there is clear intent to execute

Rationale:
The saga models distributed execution, not a human waiting room.
Availability pre-check is a query, not a reservation.
The real stock reservation still happens inside the saga, so race conditions are still handled by saga failure/compensation.

Goal:
Implement pre-saga inventory availability negotiation.

Scope:
server/order-service and server/inventory-service application layers only.

In scope:

1. Inventory availability query

In inventory-service:

- Add CheckStockAvailabilityUseCase
- Add CheckStockAvailabilityCommand
- Add CheckStockAvailabilityLineCommand
- Add CheckStockAvailabilityResult
- Add CheckStockAvailabilityLineResult

The use case:
- accepts warehouseId and requested lines
- queries StockAvailabilityPort
- returns per-line:
    - sku
    - requestedQuantity
    - availableQuantity
    - fullyAvailable boolean
    - backorderQuantity
- does NOT reserve stock
- does NOT persist anything
- does NOT publish events

2. Order-side negotiation model

In order-service:

Add application-level model/use case for preparing order submission:

- PrepareCorporateBulkOrderUseCase
- PrepareCorporateBulkOrderCommand
- PrepareCorporateBulkOrderResult

The result should express:

- allAvailable
- requestedLines
- availableLines
- unavailable/backorderLines
- recommendedAction:
    - PROCEED_FULL
    - ASK_CUSTOMER_ACCEPT_PARTIAL
    - REJECT_NO_STOCK

This is pre-saga negotiation.
No Order aggregate should be created yet unless design requires it.
Prefer not creating Order until user confirms what quantity they want to execute.

3. Customer decision outside saga

Model explicit pre-saga decisions:

- ACCEPT_FULL
- ACCEPT_PARTIAL
- CANCEL

If customer accepts partial:
- final submitted order should contain only accepted available quantities.
- the saga starts later with the accepted quantities.
- if real reservation later fails because stock changed, existing saga compensation/failure path handles it.

4. Ports

Order-service may need an outbound port:

- InventoryAvailabilityPort

This port represents a synchronous query to inventory.
Do not implement real gRPC/HTTP yet.
Use a test adapter.

5. Documentation

Create:
- docs/implementation/ARB-017-pre-saga-availability-negotiation.md

Update:
- docs/okf/index.md
- docs/rf/RF-UC-01-corporate-bulk-order.md if needed
- docs/test-cases/TC-UC-01-003-partial-backorder-human-decision.md
- server/order-service/README.md
- server/inventory-service/README.md

Document the design decision clearly:

- Human decision happens before saga start.
- Saga does not wait for customer decision in v1.
- Inventory pre-check is advisory and non-binding.
- Stock can still change between pre-check and reservation.
- Reservation remains authoritative inside saga.

Out of scope:

- No saga changes.
- No Kafka.
- No Avro mapper.
- No JPA.
- No REST controller unless already trivial.
- No gRPC/HTTP adapter.
- No UI.
- No persistence.
- No timeout.
- No scheduler.
- No backorder order creation.
- No long-lived AWAITING_CUSTOMER_DECISION saga state.

Architecture rules:

- Query use cases must not mutate domain state.
- Do not publish domain events for pre-check.
- Domain remains clean.
- Application services should read as business stories.
- Keep inbound/outbound port boundaries clear.
- No infrastructure leakage.

Tests:

Inventory-service:
- all lines fully available
- partially available lines
- no stock available
- backorderQuantity computed correctly
- no repository save
- no event published

Order-service:
- prepare order when all available → PROCEED_FULL
- prepare order when partial → ASK_CUSTOMER_ACCEPT_PARTIAL
- prepare order when none available → REJECT_NO_STOCK
- accepted partial decision produces accepted quantities only
- no Order created before final submit
- StockAvailabilityPort/InventoryAvailabilityPort is called
- invalid command validation

Acceptance Criteria:

- inventory-service tests pass.
- order-service tests pass.
- no saga tests changed unless docs-only references require it.
- no Kafka/JPA/REST infrastructure introduced.
- design decision documented.
- ARB-017 ready for Deep review.

After completion:
- Report created/modified files.
- Report tests run.
- Report open questions.
- Do not start next ARB.