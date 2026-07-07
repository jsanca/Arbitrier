# UC-01 — Corporate Bulk Order with B2B Credit

| Field | Value |
|-------|-------|
| Status | Draft |
| Date | 2026-07-07 |
| Source | `docs/okf/seeds/UC-01-corporate-bulk-order.md`, `docs/okf/seeds/UC-01-use-case-and-test-matrix.md` |

## Intention

Model a corporate bulk order as an orchestrated saga where inventory reservation, B2B credit consumption, human decisions for partial backorders, compensations, and final order visibility are explicit.

## Context

The corporate buyer submits one or more SKU lines through the B2B portal. The order begins in `PENDING`, the orchestrator coordinates inventory and credit services, and the buyer or operator must be able to see the current saga status and event timeline.

The only documented terminal states for UC-01 are:

- `CONFIRMED`
- `PARTIALLY_CONFIRMED`
- `CANCELLED`

The documented waiting state is:

- `AWAITING_CUSTOMER_DECISION`

## Decision or Requirement

UC-01 must preserve these guarantees:

- No order remains in an ambiguous state.
- Reserved inventory is released when the current saga does not confirm the reserved stock.
- Credit is never consumed for an order that ends as `CANCELLED`.
- Credit is consumed exactly for the amount that is confirmed.
- Partial backorder requires an explicit buyer decision before proceeding.
- The orchestrator owns business transitions; adapters must not hide business decisions.

## Inputs

- Authenticated corporate buyer.
- Corporate account with active B2B credit line.
- Order lines containing SKU and quantity.
- Product catalog availability.
- Warehouse inventory availability.
- Inventory events: `StockReserved`, `StockPartiallyReserved`.
- Credit events: `CreditApproved`, `CreditRejected`.
- Buyer decisions: `ACCEPT_PARTIAL`, `WAIT_BACKORDER`, `CANCEL_ORDER`.

## Outputs

- Order created with `PENDING` status.
- `OrderCreated` event.
- Inventory reservation request.
- Credit validation and consumption request.
- `OrderConfirmed` event when fully confirmed.
- `PARTIALLY_CONFIRMED` order when the buyer accepts available lines.
- `CANCELLED` order with reason `insufficient_credit`, `system_timeout`, `customer_deferred`, or `customer_cancelled`.
- Visible saga status and event timeline.

## Preconditions

- Buyer is authenticated with Keycloak.
- Buyer has an active B2B credit line.
- Product catalog and warehouse inventory are available and up to date.
- The order contains one or more SKU lines.

## Postconditions

- Happy path: order is `CONFIRMED`, all requested stock is reserved, and credit is consumed for the full confirmed amount.
- Accepted partial path: order is `PARTIALLY_CONFIRMED`, available stock remains reserved, backorder lines are cancelled or released, and credit is consumed only for confirmed lines.
- Deferred backorder path: current saga is `CANCELLED` with reason `customer_deferred`, all reserved inventory is released, and a derived waiting order is created or requested.
- Cancelled partial path: order is `CANCELLED` with reason `customer_cancelled`, and all reserved inventory is released.
- Credit rejected path: order is `CANCELLED` with reason `insufficient_credit`, reserved inventory is released, and exact credit limit details are not exposed to the buyer.
- Timeout path: order is `CANCELLED` with reason `system_timeout`, and compensation runs according to the current saga step.

## Failure Behavior

- Inventory timeout before reservation: retry according to the documented Resilience4j policy, then cancel with `system_timeout`; credit must not be consumed.
- Credit timeout after stock reservation: retry according to the documented Resilience4j policy, then emit or request `ReleaseStock` and cancel with `system_timeout`.
- Credit rejection after stock reservation: release reserved inventory and cancel with `insufficient_credit`.
- Duplicate `OrderCreated`: do not create a duplicate saga or duplicate stock reservation.
- Duplicate `ReleaseStock`: leave inventory consistent and do not create negative reservation.

## Observability Expectations

- Every saga transition logs `sagaId`, `orderId`, state, transition, and reason when available.
- Every important transition produces traces and metrics.
- The dashboard shows order status, saga state, event timeline, and pending human decision when the saga is waiting.
- Compensation attempts and idempotent duplicate handling must be visible in logs and traces.

## Test Evidence Placeholder

- Unit tests: to be linked after implementation.
- Integration tests: see `docs/test-cases/TC-UC-01-001-create-pending-order.md` through `TC-UC-01-011-duplicate-order-created-idempotent.md`.
- E2E tests: see `docs/test-cases/TC-UC-01-012-saga-timeline-visible.md`.
- Contract tests: OPEN QUESTION until Avro schemas and topics are documented.

## Open Questions

- OPEN QUESTION: Should commands also go through Kafka, or only events?
- OPEN QUESTION: Should customer decision be exposed through `order-service` or `orchestrator-service`?
- OPEN QUESTION: Should waiting backorder create a new order immediately or emit `BackorderRequested`?
- OPEN QUESTION: What is the timeout SLA for inventory and credit?
- OPEN QUESTION: What roles are needed in Keycloak for buyer, operator, approver, and admin?
- OPEN QUESTION: What exact topic names, event schemas, and command schemas should be used?
- OPEN QUESTION: What exact estimated shipping date rule is shown after confirmation?
